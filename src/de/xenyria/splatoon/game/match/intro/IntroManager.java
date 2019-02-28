package de.xenyria.splatoon.game.match.intro;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.Title;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.xenyria.servercore.spigot.listener.ProtocolListener;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.ArmorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class IntroManager {

    private Match match;
    private int teamIndex = 0;
    private int teamTicks = 0;
    private ArrayList<Team> teams = new ArrayList<>();

    public enum Phase {

        MAP_PREVIEW,
        TEAM_PREVIEW;

    }

    public Phase introPhase() {

        if(ticks >= Map.IntroductionCamera.INTRODUCTION_TICKS) {

            return Phase.TEAM_PREVIEW;

        } else {

            return Phase.MAP_PREVIEW;

        }

    }

    public class FakeEntity {

        private EntitySquid squid;
        private EntityPlayer player;
        private Team team;
        private ItemStack mainHandWeapon,offHandWeapon;
        private Location spawnPoint;

    }

    private ArrayList<FakeEntity> entities = new ArrayList<>();

    private HashMap<SplatoonHumanPlayer, ArrayList<Entity>> spawnedEntities = new HashMap<>();

    public void handlePlayerQuit(SplatoonHumanPlayer player) {

        ArrayList<Entity> entities = spawnedEntities.getOrDefault(player, new ArrayList<>());
        for(Entity entity : entities) {

            if(entity instanceof EntityPlayer) {

                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, (EntityPlayer)entity));

            }
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(entity.getId()));

        }
        spawnedEntities.remove(player);

    }
    public void addSpawnedEntity(SplatoonHumanPlayer player, Entity entity) {

        ArrayList<Entity> entities = spawnedEntities.getOrDefault(player, new ArrayList<>());
        if(entities.isEmpty()) {

            spawnedEntities.put(player, entities);

        }
        entities.add(entity);

    }
    public void removeSpawnedEntity(SplatoonHumanPlayer player, Entity entity) {

        ArrayList<Entity> entities = spawnedEntities.getOrDefault(player, null);
        if(entities != null) {

            entities.remove(entity);

        }


    }

    public IntroManager(Match match) {

        this.match = match;
        teams.addAll(match.getRegisteredTeams());

        for(SplatoonPlayer player : match.getAllPlayers()) {

            if(!player.isHuman()) {

                ((EntityNPC)player).disableTracker();

            }

            if(!player.isSpectator()) {

                FakeEntity entity = new FakeEntity();
                entity.team = player.getTeam();
                entity.spawnPoint = player.getSpawnPoint();
                entity.mainHandWeapon = player.getEquipment().getPrimaryWeapon().asItemStack();
                entity.squid = new EntitySquid(match.nmsWorld());
                entity.squid.setCustomNameVisible(true);
                entity.squid.collides = false;
                entity.squid.getBukkitEntity().setCustomName(player.getColor().prefix() + player.getName());

                entity.squid.locX = entity.spawnPoint.getX();
                entity.squid.locY = entity.spawnPoint.getY();
                entity.squid.locZ = entity.spawnPoint.getZ();
                entity.squid.yaw = entity.spawnPoint.getYaw();
                entity.squid.pitch = entity.spawnPoint.getPitch();

                GameProfile profile = player.getGameProfile();

                GameProfile clone = new GameProfile(profile.getId(), profile.getName());
                if(profile.getProperties().containsKey("textures")) {

                    Property property = profile.getProperties().get("textures").iterator().next();

                    clone.getProperties().put("textures", new Property("textures", property.getValue(), property.getSignature()));

                }
                clone.getProperties().put(ProtocolListener.GAMEPROFILE_IGNORE_KEY, new Property("xst","xst", "xst"));

                entity.player = new EntityPlayer(match.nmsWorld().getMinecraftServer(), (WorldServer) match.nmsWorld(), clone, new PlayerInteractManager(player.getMatch().nmsWorld()));

                entity.player.locX = entity.spawnPoint.getX();
                entity.player.locY = entity.spawnPoint.getY();
                entity.player.locZ = entity.spawnPoint.getZ();
                entity.player.yaw = entity.spawnPoint.getYaw();
                entity.player.pitch = entity.spawnPoint.getPitch();

                entities.add(entity);

            }

        }

        previousLocation = getCurrentLocation();
        ticks++;

        for(SplatoonPlayer player : match.getAllPlayers()) {

            if(player.isHuman()) {

                for (FakeEntity entity : entities) {

                    EntityPlayer player1 = player.getNMSPlayer();
                    addSpawnedEntity((SplatoonHumanPlayer)player, entity.squid);
                    player1.playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(entity.squid));
                    player1.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(entity.squid.getId(), entity.squid.getDataWatcher(), true));
                    player1.playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(entity.squid, (byte)(entity.squid.yaw * 0.7111)));

                }

            }

        }

    }

    private int ticks = 0;
    private boolean destroyedSquids = false;
    private boolean sentTeamTeleport = false;
    private boolean sentFormChange = false;
    private boolean sentHumanSpawn = false;
    private boolean sentWeaponGive = false;

    public void initializeSequence() {

        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

            for(SplatoonPlayer player1 : match.getAllPlayers()) {

                if(player1 != player) {

                    removeSpawnedEntity((SplatoonHumanPlayer)player, player1.getNMSPlayer());
                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player1.getNMSPlayer().getId()));
                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player1.getNMSPlayer()));

                }

            }

            for(FakeEntity entity : entities) {

                addSpawnedEntity((SplatoonHumanPlayer)player, entity.player);
                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entity.player));

            }

            Location location = getCurrentLocation();
            MatchType type = match.getMatchType();

            String prefix = (player.getColor() != null ? player.getTeam().getColor().prefix() : "§8");

            player.getPlayer().sendTitle(new Title(prefix + type.getTitle(), type.getDescription(), 20, 80, 20));
            player.getPlayer().setGameMode(GameMode.SPECTATOR);
            player.getPlayer().teleport(getCurrentLocation());
            player.getPlayer().setAllowFlight(true);
            player.getPlayer().setFlying(true);
            player.getPlayer().setFlySpeed(0f);
            player.getPlayer().setWalkSpeed(0f);
            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutAbilities(player.getNMSPlayer().abilities));

        }

        ArrayList<SplatoonHumanPlayer> players = new ArrayList();
        players.addAll(match.getHumanPlayers());

        new Thread(() -> {

            HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags>();
            for(PacketPlayOutPosition.EnumPlayerTeleportFlags flags1 : PacketPlayOutPosition.EnumPlayerTeleportFlags.values()) { flags.add(flags1); }

            while (ticks < Map.IntroductionCamera.INTRODUCTION_TICKS) {

                try { Thread.sleep(1000 / 60); } catch (Exception e) {}
                ticks++;

                ticksToTeleport++;
                Location oldLocation = previousLocation.clone();
                Location currentLocation = getCurrentLocation();
                boolean absoluteInstead = ticksToTeleport > 5;
                if(absoluteInstead) { ticksToTeleport = 0;}

                Packet packet = null;
                if(absoluteInstead) {

                    packet = new PacketPlayOutPosition(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ(), currentLocation.getYaw(), currentLocation.getPitch(), new HashSet<>(), 0);

                } else {

                    Vector delta = currentLocation.toVector().clone().subtract(oldLocation.toVector());
                    float yawOffset = currentLocation.getYaw() - previousLocation.getYaw();
                    float pitchOffset = currentLocation.getPitch() - previousLocation.getPitch();
                    packet = new PacketPlayOutPosition(delta.getX(), delta.getY(), delta.getZ(), yawOffset, pitchOffset, flags, 0);

                }

                for (SplatoonHumanPlayer player : players) {

                    if(player.getMatch() == match) {

                        player.getNMSPlayer().playerConnection.sendPacket(packet);

                    }

                }

                previousLocation = currentLocation.clone();

            }

        }).start();

    }

    private Location previousLocation = null;

    public Location getCurrentLocation() {

        Map map = match.getMap();
        Map.IntroductionCamera camera = map.getIntroductionCamera();

        Location location = new Location(match.getWorld(),
                map.getMapOffset().getX() + camera.getStart().getX(),
                map.getMapOffset().getY() + camera.getStart().getY(),
                map.getMapOffset().getZ() + camera.getStart().getZ());

        double totalDistance = camera.getStart().distance(camera.getEnd());
        double ratio = ((double)ticks / (double)Map.IntroductionCamera.INTRODUCTION_TICKS);

        Vector baseDir = camera.getFocus().clone().subtract(camera.getStart().clone());
        Location location1 = new Location(match.getWorld(), 0,0,0);
        location1.setDirection(baseDir);

        Vector direction = camera.getEnd().clone().subtract(camera.getStart()).normalize();

        // Position
        location = location.add(direction.multiply(ratio * totalDistance));

        // Blickrichtung
        Vector focusDir = map.getMapOffset().clone().add(camera.getFocus().clone()).subtract(location.toVector()).normalize();
        location.setDirection(focusDir);
        return location;

    }

    private boolean finished = false;
    private int ticksToTeleport = 0;
    private boolean appliedTeamZoom = false;

    public boolean isFinished() { return finished; }

    public void tick() {

        if(!finished) {

            if (ticks >= Map.IntroductionCamera.INTRODUCTION_TICKS) {

                if(!appliedTeamZoom) {

                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        player.getNMSPlayer().playerConnection.sendPacket(
                                new PacketPlayOutAbilities(player.getNMSPlayer().abilities)
                        );

                    }
                    appliedTeamZoom = true;

                }

                /*if(!destroyedSquids) {

                    int[] squidIDs = new int[entities.size()];
                    int i = 0;
                    for(FakeEntity entity : entities) {

                        squidIDs[i] = entity.squid.getId();
                        i++;

                    }

                    for(SplatoonPlayer player : match.getAllPlayers()) {

                        if(player.isHuman()) {

                            player.getNMSPlayer().playerConnection.sendPacket(
                                    new PacketPlayOutEntityDestroy(squidIDs)
                            );

                        }

                    }

                    destroyedSquids = true;

                }*/

                teamTicks++;
                if(teamIndex > (teams.size() - 1)) {

                    int[] removal = new int[entities.size()*2];
                    int x = 0;
                    for(int i = 0; i < entities.size() * 2; i+=2) {

                        FakeEntity entity = entities.get(x);
                        removal[0] = entity.squid.getId();
                        removal[1] = entity.player.getId();
                        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                            removeSpawnedEntity(player, entity.squid);
                            removeSpawnedEntity(player, entity.player);

                        }
                        x++;

                    }

                    for(FakeEntity entity : entities) {

                        for (SplatoonHumanPlayer player : match.getHumanPlayers()) {

                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entity.player));

                        }

                    }
                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(removal));

                    }

                    finished = true;
                    return;

                }

                if(!sentTeamTeleport) {

                    Location location = match.getMap().getSpawns().get(teamIndex).introVector(match.getWorld());
                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        player.getPlayer().teleport(location);

                    }
                    sentTeamTeleport = true;

                }

                if(teamTicks > 14) {

                    if(!sentHumanSpawn) {

                        sentHumanSpawn = true;
                        Team team = teams.get(teamIndex);
                        for(FakeEntity entity : entities) {

                            if(entity.team.equals(team)) {

                                for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(entity.squid.getId()));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(entity.player));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(entity.player, (byte)(entity.spawnPoint.getYaw() * 0.7111)));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutAnimation(entity.player, 0));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                                            entity.player.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(ArmorUtil.getHelmet(team.getColor()))
                                    ));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                                            entity.player.getId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(ArmorUtil.getChestplate(team.getColor()))
                                    ));
                                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                                            entity.player.getId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(ArmorUtil.getBoots(team.getColor()))
                                    ));
                                    player.getPlayer().playSound(entity.spawnPoint, Sound.ENTITY_FISH_SWIM, 1f, .8f);

                                }

                            }

                        }

                    }

                    if(ticks > 26) {

                        if(!sentWeaponGive) {

                            sentWeaponGive = true;
                            Team team = teams.get(teamIndex);
                            for(FakeEntity entity : entities) {

                                if(entity.team.equals(team)) {

                                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                                                entity.player.getId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(entity.mainHandWeapon))
                                        );
                                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                                                entity.player.getId(), EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(entity.offHandWeapon))
                                        );

                                    }

                                }

                            }

                        }

                    }

                }

                if (teamTicks > 42) {

                    Team before = teams.get(teamIndex);
                    for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

                        for(FakeEntity entity : entities) {

                            if(entity.team.equals(before)) {

                                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(entity.player.getId()));
                                removeSpawnedEntity(player, entity.player);

                            }

                        }

                    }

                    teamTicks = 0;
                    sentTeamTeleport = false;
                    sentFormChange = false;
                    sentHumanSpawn = false;
                    sentWeaponGive = false;
                    teamIndex++;

                }

            }

        }

    }

}
