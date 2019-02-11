package de.xenyria.splatoon.lobby;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.xenyria.core.math.BitUtil;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.camera.CinematicCamera;
import de.xenyria.servercore.spigot.camera.CinematicSequence;
import de.xenyria.servercore.spigot.camera.listener.CinematicCameraEventHandler;
import de.xenyria.servercore.spigot.display.image.ImageManager;
import de.xenyria.servercore.spigot.util.WorldUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class PlazaLobbyManager {

    private static World lobbyWorld;
    public static World getLobbyWorld() { return lobbyWorld; }

    private SplatoonLobby lobby;
    public SplatoonLobby getLobby() { return lobby; }

    public void addPlayerToLobby(SplatoonHumanPlayer player) {

        if(player.getMatch() != null) {

            player.leaveMatch();

        }
        player.joinMatch(lobby);

    }

    public PlazaLobbyManager() {

        try {

            lobbyWorld = Bukkit.createWorld(new WorldCreator("sp_lobby").generator(new ChunkGenerator() {
                @Override
                public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                    return new CraftChunkData(world);
                }
            }));
            ImageManager.loadImage("unavailable", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "unavailable.png"), SplatoonLobby.SCREEN_DIMENSIONS);
            ImageManager.loadImage("testfire", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "testfire.png"), SplatoonLobby.SCREEN_DIMENSIONS);
            ImageManager.loadImage("testfire_alt", new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "images" + File.separator + "testfire_alt.png"), SplatoonLobby.SCREEN_DIMENSIONS);

            lobby = new SplatoonLobby(lobbyWorld);
            XenyriaSpigotServerCore.getXenyriaLogger().log("Lobby wurde initialisiert!");

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void triggerIntro(SplatoonHumanPlayer player1) {

        Player player = player1.getPlayer();

        ArrayList<CinematicSequence> sequences = XenyriaSplatoon.getLobbyManager().getLobby().createIntroSequences(player);
        CinematicCamera camera = new CinematicCamera(player.getWorld());
        for(CinematicSequence sequence : sequences) { camera.addSequence(sequence); }
        camera.setExitGameMode(GameMode.SURVIVAL);
        camera.setGamemodeToUse(GameMode.SPECTATOR);
        camera.setEventHandler(new CinematicCameraEventHandler() {

            private EntityPlayer self = null;

            @Override
            public void onStart(CinematicCamera cinematicCamera) {

                net.minecraft.server.v1_13_R2.World world = ((CraftWorld)player.getWorld()).getHandle();
                UUID uuid = UUID.randomUUID();
                GameProfile profile = new GameProfile(uuid, player.getName());

                EntityPlayer selfPlayer = ((CraftPlayer)player).getHandle();
                if(selfPlayer.getProfile().getProperties().containsKey("textures")) {

                    GameProfile profile1 = selfPlayer.getProfile();
                    Property property = profile1.getProperties().get("textures").iterator().next();
                    profile1.getProperties().put("textures", property);

                }

                self = new EntityPlayer(world.getMinecraftServer(), (WorldServer)world, profile, new PlayerInteractManager(world));
                self.setUUID(uuid);
                self.locX = lobby.getLobbySpawn().getX();
                self.locY = lobby.getLobbySpawn().getY();
                self.locZ = lobby.getLobbySpawn().getZ();
                self.yaw = lobby.getLobbySpawn().getYaw();
                self.aS = lobby.getLobbySpawn().getYaw();
                self.pitch = lobby.getLobbySpawn().getPitch();

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, self
                ));

                Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(self));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(self, (byte)0));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(self.getId(),
                            EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(player1.getEquipment().getHeadGear().asItemStack(null))));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(self.getId(),
                            EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(player1.getEquipment().getBodyGear().asItemStack(null))));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(self.getId(),
                            EnumItemSlot.FEET, CraftItemStack.asNMSCopy(player1.getEquipment().getFootGear().asItemStack(null))));

                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, self
                        ));

                    }, 3l);

                }, 3l);


            }

            @Override
            public void onExit(CinematicCamera cinematicCamera) {

                PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(self.getId());
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
                player.teleport(lobby.getLobbySpawn());
                SplatoonHumanPlayer.getPlayer(player).updateEquipment();

            }

            @Override
            public void handleUnexpectedPlayerQuit(CinematicCamera cinematicCamera, Player player) {

            }

            @Override
            public void hideAllPlayers(CinematicCamera cinematicCamera, Player player) {

            }

            @Override
            public void showAllPlayers(CinematicCamera cinematicCamera, Player player) {

            }
        });
        camera.addPlayer(player);
        camera.start();

    }

}
