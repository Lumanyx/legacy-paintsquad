package de.xenyria.splatoon.lobby.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.mojang.authlib.GameProfile;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.navigation.NavigationManager;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.gear.boots.FootGear;
import de.xenyria.splatoon.game.equipment.gear.chest.BodyGear;
import de.xenyria.splatoon.game.equipment.gear.head.HeadGear;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.lobby.npc.animation.RecentPlayerAnimation;
import de.xenyria.splatoon.lobby.npc.animation.TalkAnimation;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class RecentPlayerNPC {

    private RecentPlayerAnimation animation;

    public RecentPlayerNPC(Location location, RecentPlayerAnimation animation) {

        animation.assign(this);
        this.animation = animation;

        WorldServer server = ((CraftWorld)location.getWorld()).getHandle();
        player = new EntityPlayer(server.getMinecraftServer(), server, new GameProfile(UUID.randomUUID(), "RecentPlayer"), new PlayerInteractManager(server));
        this.world = location.getWorld();
        player.locX = location.getX();
        player.locY = location.getY();
        player.locZ = location.getZ();
        player.yaw = location.getYaw();
        player.aS = location.getYaw();
        player.pitch = location.getPitch();
        player.setInvisible(true);
        player.setInvisible(false);
        player.setInvisible(true);
        player.setInvisible(false);

    }

    private World world;

    private EntityPlayer player = null;
    private EntityArmorStand seat = null;
    private ArrayList<Player> trackers = new ArrayList<>();

    public RecentPlayerNPC(Location location, RecentPlayerAnimation animation, boolean sit) {

        this(location, animation);
        net.minecraft.server.v1_13_R2.World world = ((CraftWorld)location.getWorld()).getHandle();

        if(sit) {

            seat = new EntityArmorStand(world, location.getX(), location.getY() - 2, location.getZ());
            seat.setPosition(location.getX(), location.getY() - 2, location.getZ());
            seat.yaw = location.getYaw();
            seat.aS = location.getYaw();
            seat.setInvisible(true);
            seat.setNoGravity(true);

        }

    }


    public void updateFacing(float yaw, float pitch) {

        byte yawVal = (byte)((yaw * 0.703333));
        byte pitchVal = (byte)((pitch * 0.703333));

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(this.player, yawVal));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.player.getId(), yawVal, pitchVal, true));

        }
        this.player.yaw = yaw;
        this.player.pitch = pitch;

    }

    public void broadcastAnimation(byte val) {

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(this.player, val));

        }

    }

    public void tick() {

        Iterator<Player> iterator = trackers.iterator();
        while (iterator.hasNext()) {

            Player player = iterator.next();
            SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

            if(!player.isOnline() || !player.getWorld().equals(world) || (player1 != null && player1.getMatch() != null && !(player1.getMatch() instanceof SplatoonLobby))) {

                iterator.remove();
                if(player.isOnline()) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(this.player.getId()));
                    if(seat != null) {

                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(seat.getId()));

                    }

                }

            }

        }

        for(Player player : world.getPlayers()) {

            if(!trackers.contains(player)) {

                if (player.getLocation().distance(this.player.getBukkitEntity().getLocation()) <= RECENT_PLAYER_RANGE) {

                    SplatoonHumanPlayer humanPlayer = SplatoonHumanPlayer.getPlayer(player);
                    if(humanPlayer != null && humanPlayer.getMatch() != null && humanPlayer.getMatch() instanceof SplatoonLobby) {

                        RecentPlayer player1 = humanPlayer.getRandomRecentPlayer();
                        if(player1 != null) {

                            Scoreboard scoreboard = humanPlayer.getPlayer().getScoreboard();
                            org.bukkit.scoreboard.Team team = scoreboard.getTeam("lobby-team-npc");
                            if(team != null) {

                                team.addEntry(player1.getName());

                            }

                            spawn(humanPlayer, player1);
                            humanPlayer.markAsSpawned(player1);

                        }

                    }

                }

            }

        }

        if(animation != null) {

            animation.tick();

        }

    }

    public static final double RECENT_PLAYER_RANGE = 96D;

    public void spawn(SplatoonHumanPlayer player1, RecentPlayer template) {

        try {

            Color color = template.getColor();
            PacketContainer container = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            List<PlayerInfoData> dataList = new ArrayList<PlayerInfoData>();

            WrappedGameProfile profile = new WrappedGameProfile(player.getUniqueID(), template.getName());
            profile.getProperties().put("textures", new WrappedSignedProperty("textures", template.getTexture(), template.getSignature()));

            PlayerInfoData npcData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(color.prefix() + template.getName()));
            dataList.add(npcData);
            container.getPlayerInfoDataLists().write(0, dataList);
            container.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player1.getPlayer(), container, false);
            trackers.add(player1.getPlayer());

            Bukkit.getScheduler().runTaskLater(XenyriaSpigotServerCore.getPlugin(), () -> {

                if(trackers.contains(player1.getPlayer())) {

                    PacketPlayOutNamedEntitySpawn entitySpawn = new PacketPlayOutNamedEntitySpawn(player);
                    player1.getNMSPlayer().playerConnection.sendPacket(entitySpawn);

                    // Sitz
                    if (seat != null) {

                        PacketPlayOutSpawnEntityLiving living = new PacketPlayOutSpawnEntityLiving(seat);
                        player1.getNMSPlayer().playerConnection.sendPacket(living);

                        PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(seat.getId(), seat.getDataWatcher(), true);
                        player1.getNMSPlayer().playerConnection.sendPacket(meta);

                        PacketContainer mountContainer = new PacketContainer(PacketType.Play.Server.MOUNT);
                        mountContainer.getIntegers().write(0, seat.getId());
                        mountContainer.getIntegerArrays().write(0, new int[]{player.getId()});
                        try {

                            ProtocolLibrary.getProtocolManager().sendServerPacket(player1.getPlayer(), mountContainer, false);

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                    }

                    HeadGear h1 = (HeadGear) XenyriaSplatoon.getGearRegistry().dummyInstance(template.getHelmetID());
                    ItemStack helmet = h1.asItemStack(color);
                    BodyGear b1 = (BodyGear) XenyriaSplatoon.getGearRegistry().dummyInstance(template.getChestplateID());
                    ItemStack chestplate = b1.asItemStack(color);
                    FootGear f1 = (FootGear) XenyriaSplatoon.getGearRegistry().dummyInstance(template.getBootsID());
                    ItemStack boots = f1.asItemStack(color);

                    player1.getNMSPlayer().playerConnection.sendPacket(
                            new PacketPlayOutEntityEquipment(player.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(helmet))
                    );
                    player1.getNMSPlayer().playerConnection.sendPacket(
                            new PacketPlayOutEntityEquipment(player.getId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(chestplate))
                    );
                    player1.getNMSPlayer().playerConnection.sendPacket(
                            new PacketPlayOutEntityEquipment(player.getId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(boots))
                    );

                    byte yawBytes = (byte)(this.player.yaw * 0.703);
                    byte pitchBytes = (byte)(this.player.pitch * 0.703);

                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.player.getId(), yawBytes, pitchBytes, true));
                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(this.player, yawBytes));
                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutAnimation(this.player, (byte)0));

                    Bukkit.getScheduler().runTaskLater(XenyriaSpigotServerCore.getPlugin(), () -> {

                        PacketPlayOutPlayerInfo removal = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player);
                        player1.getNMSPlayer().playerConnection.sendPacket(removal);

                    }, 60l);

                }

            }, 2l);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public Location getLocation() {

        return new Location(player.getBukkitEntity().getWorld(), player.locX, player.locY, player.locZ, player.yaw, player.pitch);

    }

    public void move(double x, double y, double z) {

        player.locX+=x;
        player.locY+=y;
        player.locZ+=z;

        PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(player);
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(teleport);

        }

    }

}
