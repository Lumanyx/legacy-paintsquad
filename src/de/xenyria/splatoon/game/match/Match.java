package de.xenyria.splatoon.game.match;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.mysql.fabric.xmlrpc.base.Array;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.array.TwoDimensionalMap;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.ai.MatchAIManager;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.match.intro.IntroManager;
import de.xenyria.splatoon.game.match.outro.OutroManager;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.RemovableGameObject;
import de.xenyria.splatoon.game.objects.Sprinkler;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.TentaMissleRocket;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public abstract class Match {

    private HashMap<UUID, ArrayList<SplatoonHumanPlayer>> spawnedInkTanks = new HashMap<>();

    private World world;
    public World getWorld() { return world; }

    private MatchControlInterface matchController;
    public MatchControlInterface getMatchController() { return matchController; }
    public void setMatchController(MatchControlInterface matchControlInterface) { this.matchController = matchControlInterface; }

    private HashMap<BlockPosition, IBlockData> rollbackMap = new HashMap<>();

    private BlockFlagManager blockFlagManager = new BlockFlagManager(this);
    public BlockFlagManager getBlockFlagManager() { return blockFlagManager; }

    public void handleInkTanks() {

        if(inProgress() && !(this instanceof SplatoonLobby)) {

            Iterator<java.util.Map.Entry<UUID, ArrayList<SplatoonHumanPlayer>>> iterator = spawnedInkTanks.entrySet().iterator();
            while (iterator.hasNext()) {

                java.util.Map.Entry<UUID, ArrayList<SplatoonHumanPlayer>> entry = iterator.next();
                Player player = Bukkit.getPlayer(entry.getKey());

                if(player != null) {

                    SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                    ArrayList<SplatoonHumanPlayer> spawnedPlayers = entry.getValue();
                    if(!spawnedPlayers.isEmpty()) {

                        Iterator<SplatoonHumanPlayer> iterator1 = spawnedPlayers.iterator();
                        while (iterator1.hasNext()) {

                            SplatoonHumanPlayer player2 = iterator1.next();
                            if(inIntro() || inOutro() || player2.isSpecialActive() || player2.isSplatted() || player2.isSquid() || player2.getLocation().toVector().distance(player.getLocation().toVector()) >= 32D) {

                                player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player2.getTank().getId()));
                                iterator1.remove();

                            } else {

                                byte yaw = (byte) ((player2.getTank().yaw)*0.70333);

                                PacketPlayOutEntityHeadRotation look = new PacketPlayOutEntityHeadRotation(player2.getTank(), yaw);
                                player1.getNMSPlayer().playerConnection.sendPacket(look);

                            }

                        }

                    }

                } else {

                    iterator.remove();

                }

            }

            if(!inIntro() && !inOutro()) {

                for (SplatoonHumanPlayer player : getHumanPlayers()) {

                    if (ResourcePackUtil.hasCustomResourcePack(player.getPlayer())) {

                        for (SplatoonHumanPlayer otherPlayer : getHumanPlayers()) {

                            //if (player != otherPlayer) {
                            if (spawnedInkTanks.containsKey(player.getUUID())) {

                                boolean spawned = (spawnedInkTanks.containsKey(player.getUUID()) && spawnedInkTanks.get(player.getUUID()).contains(otherPlayer));
                                if (!spawned) {

                                    if (!otherPlayer.isSquid() && !otherPlayer.isSpecialActive() && !otherPlayer.isSplatted() && !otherPlayer.isSpectator() && otherPlayer.getLocation().toVector().distance(player.getPlayer().getLocation().toVector()) < 32D) {

                                        otherPlayer.getTank().locX = otherPlayer.getLocation().getX();
                                        otherPlayer.getTank().locY = otherPlayer.getLocation().getY();
                                        otherPlayer.getTank().locZ = otherPlayer.getLocation().getZ();

                                        ArrayList<SplatoonHumanPlayer> list = spawnedInkTanks.get(player.getUUID());
                                        list.add(otherPlayer);
                                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(otherPlayer.getTank()));
                                        player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(otherPlayer.getTank().getId(), otherPlayer.getTank().getDataWatcher(), true));

                                        short tankDurability = otherPlayer.getTeam().getColor().tankDurabilityValue();
                                        ItemStack stack = new ItemBuilder(Material.GOLDEN_AXE).setUnbreakable(true).setDurability(
                                                tankDurability
                                        ).create();

                                        PacketPlayOutEntityEquipment equipment = new PacketPlayOutEntityEquipment(otherPlayer.getTank().getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(stack));
                                        player.getNMSPlayer().playerConnection.sendPacket(equipment);

                                        PacketContainer container = new PacketContainer(PacketType.Play.Server.MOUNT);
                                        container.getIntegers().write(0, otherPlayer.getEntityID());
                                        container.getIntegerArrays().write(0, new int[]{otherPlayer.getTank().getId()});
                                        try {

                                            ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), container, false);

                                        } catch (Exception e) {

                                            e.printStackTrace();

                                        }

                                    }

                                }

                            } else {

                                if (player.millisSinceMatchSwitch() > 150) {

                                    spawnedInkTanks.put(player.getUUID(), new ArrayList<>());

                                }

                            }

                        }

                    }

                }

            }

        }

    }

    private HashMap<Team, Integer> teamTurfCounter = new HashMap<>();
    public void incrementTurfCounter(Team team) {

        if(!teamTurfCounter.containsKey(team)) {

            teamTurfCounter.put(team, 1);

        } else {

            teamTurfCounter.put(team, teamTurfCounter.get(team)+1);

        }

    }
    public void decrementTurfCounter(Team team) {

        if(teamTurfCounter.containsKey(team)) {

            teamTurfCounter.put(team, teamTurfCounter.get(team)-1);

        }

    }

    public void clearQueues() {

        queuedPlayerRemovals.clear();
        queuedGameObjectRemovals.clear();
        queuedProjectiles.clear();

    }

    public void rollback() {

        rollback(false);

    }

    public void rollback(boolean sendUpdates) {

        net.minecraft.server.v1_13_R2.World world = nmsWorld();
        Bukkit.broadcastMessage(Chat.SYSTEM_PREFIX + "Rollback in §eMatch " + getClass());

        blockFlagManager.reset();

        HashMap<ChunkCoordIntPair, ArrayList<BlockPosition>> multiBlockChange = new HashMap<>();
        ArrayList<Player> receivers = new ArrayList<>();

        for(java.util.Map.Entry<BlockPosition, IBlockData> entry : rollbackMap.entrySet()) {

            Block block = getWorld().getBlockAt(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());
            if(sendUpdates) {

                ChunkCoordIntPair pair = new ChunkCoordIntPair(block.getX() >> 4, block.getZ() >> 4);
                if(!multiBlockChange.containsKey(pair)) {

                    multiBlockChange.put(pair, new ArrayList<>());

                }
                multiBlockChange.get(pair).add(new BlockPosition(block.getX(),block.getY(), block.getZ()));
                block.setBlockData(CraftBlockData.fromData(entry.getValue()));

            } else {

                world.setTypeAndData(entry.getKey(), entry.getValue(), 0);

            }

        }
        rollbackMap.clear();

        if(rollbackEnabled) {

            for(java.util.Map.Entry<ChunkCoordIntPair, ArrayList<BlockPosition>> entry : multiBlockChange.entrySet()) {

                ArrayList<Player> players = new ArrayList<>();
                Vector pos = new Vector(entry.getKey().x * 16, 0, entry.getKey().z * 16);
                for(Player player : getWorld().getPlayers()) {

                    Vector playerPos = player.getLocation().toVector();
                    playerPos.setY(0);
                    double dist = pos.distance(playerPos);
                    if(dist <= 96) {

                        players.add(player);

                    }

                }

                if(!players.isEmpty()) {

                    PacketContainer container = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
                    MultiBlockChangeInfo[] changes = new MultiBlockChangeInfo[entry.getValue().size()];
                    int i = 0;
                    for(BlockPosition position : entry.getValue()) {

                        changes[i] = new MultiBlockChangeInfo(new Location(getWorld(), position.getX(), position.getY(), position.getZ()), WrappedBlockData.fromHandle(world.getType(position)));
                        i++;

                    }
                    container.getChunkCoordIntPairs().write(0, new com.comphenix.protocol.wrappers.ChunkCoordIntPair(
                            entry.getKey().x, entry.getKey().z
                    ));
                    container.getMultiBlockChangeInfoArrays().write(0, changes);

                    for(Player player : Bukkit.getOnlinePlayers()) {

                        try {

                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container, false);

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                    }

                }

            }

        }

    }

    private static ArrayList<Match> registeredMatches = new ArrayList<>();
    public static void tickMatches() {

        for(Match match : registeredMatches) {

            try {

                match.tick();

            } catch (Exception e) {

                XenyriaSplatoon.getXenyriaLogger().error("Es trat ein Fehler in einem Match auf!", e);
                e.printStackTrace();

            }

        }

    }

    private HashMap<Team, Inventory> jumpMenuMap = new HashMap<>();
    private HashMap<Team, HashMap<Integer, JumpPoint>> jumpPointSlotMap = new HashMap<>();

    public static final String JUMP_MENU_TITLE = " §8" + Characters.FAT_ARROW_RIGHT + " Wähle ein Ziel";

    public Inventory getOrCreateJumpMenu(Team team) {

        if(jumpMenuMap.containsKey(team)) {

            return jumpMenuMap.get(team);

        } else {

            Inventory inventory = Bukkit.createInventory(null, 27, team.getColor().prefix() + "Supersprung" + JUMP_MENU_TITLE);
            jumpMenuMap.put(team, inventory);
            return inventory;

        }

    }

    private MatchWorldInformationProvider worldInformationProvider = new MatchWorldInformationProvider(this);
    public MatchWorldInformationProvider getWorldInformationProvider() { return worldInformationProvider; }

    private static int lastID = 1;
    private static int nextID() { lastID++; return lastID-1; }

    private int id;

    public Match(World world) {

        this.id = nextID();
        this.world = world;
        map = new Map();
        //map.getPaintDefinition().getPaintableMaterials().add(Material.STONE);
        registeredMatches.add(this);
        XenyriaSplatoon.getXenyriaLogger().log("§eMatch #" + id + " §7erstellt.");

    }

    public String mapSchematicName = "";

    public void initializeAIManager() {

        manager = new MatchAIManager(this);
        File file = new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + mapSchematicName + ".aisf");
        if(!file.exists()) {

            manager.initSpots(manager.gatherNodesBySpawns());
            new Thread(() -> {

                byte[] bytes = manager.regionsToBytes();
                try {

                    FileOutputStream stream = new FileOutputStream(file);
                    stream.write(bytes);
                    stream.close();
                    broadcast("§e" + manager.getPaintableRegions().size() + " Färbregionen §7generiert");

                } catch (Exception e) {

                    e.printStackTrace();

                }

            }).start();

        } else {

            new Thread(() -> {

                try {

                    FileInputStream stream = new FileInputStream(file);
                    byte[] bytes = new byte[stream.available()];
                    stream.read(bytes);
                    stream.close();
                    manager.fromBytes(bytes);
                    broadcast("§e" + manager.getPaintableRegions().size() + " Färbregionen §7geladen");

                } catch (Exception e) {

                    e.printStackTrace();
                    manager.initSpots(getAIController().gatherNodesBySpawns());

                }

            }).start();

        }


    }

    private ArrayList<Team> registeredTeams = new ArrayList<>();
    public ArrayList<Team> getRegisteredTeams() { return registeredTeams; }
    public void registerTeam(Team team) {

        registeredTeams.add(team);
        colorToTeamMap.put(team.getColor(), team);
        matchController.teamAdded(team);

    }
    private HashMap<Color, Team> colorToTeamMap = new HashMap<>();

    private ArrayList<GameObject> objects = new ArrayList<>();
    private ArrayList<GameObject> toAdd = new ArrayList<>();
    private ArrayList<HitableEntity> hitableObjects = new ArrayList<>();
    private ArrayList<SplatoonPlayer> registeredPlayers = new ArrayList<>();

    public ArrayList<EntityNPC> getNPCs() {

        ArrayList<EntityNPC> npcs = new ArrayList<>();
        for(SplatoonPlayer player : getAllPlayers()) {

            if(player instanceof EntityNPC) {

                npcs.add((EntityNPC)player);

            }

        }
        return npcs;

    }

    public void addPlayer(SplatoonPlayer player) {

        hitableObjects.add(player); registeredPlayers.add(player); matchController.playerAdded(player);
        player.resetSpecialUseCounter();

        if(player instanceof SplatoonHumanPlayer) {

            createScoreboardTeams(((SplatoonHumanPlayer)player).getPlayer().getScoreboard());
            for(EntityNPC npc : getNPCs()) {

                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc.getNMSPlayer()
                ));

            }

        }
        addPlayerToScoreboards(player);

        if(player instanceof EntityNPC) {

            EntityNPC npc = (EntityNPC)player;
            if(npc.isVisibleInTab()) {

                for(SplatoonHumanPlayer player1 : getHumanPlayers()) {

                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc.getNMSPlayer()));

                }

            }

        }

    }

    public void removeCreatedTeams(Scoreboard scoreboard) {

        for(Color color : Color.values()) {

            org.bukkit.scoreboard.Team team = scoreboard.getTeam("match-"+color.name());
            if(team != null) {

                team.unregister();

            }

        }

    }

    public void createScoreboardTeams(Scoreboard scoreboard) {

        for(Team team : getRegisteredTeams()) {

            org.bukkit.scoreboard.Team team1 = scoreboard.getTeam("match-"+team.getColor().getName());
            if(team1 != null) { team1.unregister(); }
            team1 = scoreboard.registerNewTeam("match-" + team.getColor().name());
            team1.setColor(team.getColor().getChatColor());
            team1.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
            team1.setPrefix("bliblablub");

            for(SplatoonPlayer player : getPlayers(team)) {

                team1.addEntry(player.getUUID().toString());

            }

        }

    }
    public void addPlayerToScoreboards(SplatoonPlayer toAdd) {

        if(toAdd.getColor() != null) {

            for (SplatoonHumanPlayer player : getHumanPlayers()) {

                Player player1 = player.getPlayer();
                Scoreboard scoreboard = player1.getScoreboard();

                org.bukkit.scoreboard.Team team = scoreboard.getTeam("match-" + toAdd.getColor().name());
                if (team != null) {

                    team.addEntry(toAdd.getUUID().toString());

                }

            }

        }

    }
    public void removePlayerFromScoreboards(SplatoonPlayer toRemove) {

        if(toRemove.getColor() != null) {

            for (SplatoonHumanPlayer player : getHumanPlayers()) {

                Player player1 = player.getPlayer();
                Scoreboard scoreboard = player1.getScoreboard();

                org.bukkit.scoreboard.Team team = scoreboard.getTeam("match-" + toRemove.getColor().name());
                if (team != null && team.getEntries().contains(toRemove.getUUID().toString())) {

                    team.removeEntry(toRemove.getUUID().toString());

                }

            }

        }

    }

    public void removePlayer(SplatoonPlayer player) {

        hitableObjects.remove(player); registeredPlayers.remove(player); matchController.playerRemoved(player);
        if(player instanceof SplatoonHumanPlayer) {

            SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
            if(spawnedInkTanks.containsKey(player1.getUUID())) {

                ArrayList<SplatoonHumanPlayer> list = spawnedInkTanks.get(player1.getUUID());
                for(SplatoonHumanPlayer player2 : list) {

                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player2.getTank().getId()));

                }

            }
            spawnedInkTanks.remove(player1.getUUID());
            removeCreatedTeams(player1.getPlayer().getScoreboard());

        } else {

            for(SplatoonHumanPlayer player1 : getHumanPlayers()) {

                player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player.getNMSPlayer()));

            }

        }
        removePlayerFromScoreboards(player);

    }
    public ArrayList<SplatoonPlayer> getAllPlayers() { return registeredPlayers; }

    public void addGameObject(GameObject object) {

        toAdd.add(object);
        matchController.objectAdded(object);

    }
    public void removeGameObject(GameObject object) {

        objects.remove(object);
        if(hitableObjects.contains(object)) { hitableObjects.remove(object); }
        matchController.objectRemoved(object);

    }

    //public ArrayList<GameObject> getObjects() { return objects; }
    public void end() {

        registeredMatches.remove(this);

    }

    private static int lastObjectID;
    public static int nextObjectID() { return lastObjectID++; }

    public GameObject getObject(int id) {

        for(GameObject object : getGameObjects()) {

            if(object.getID() == id) {

                return object;

            }

        }
        return null;

    }

    public void tick() {

        handleInkTanks();
        if(manager != null) {

            getAIController().tick();

        }
        for(GameObject object : toAdd) {

            if(object instanceof HitableEntity) { hitableObjects.add((HitableEntity)object); }
            objects.add(object);

        }
        toAdd.clear();

        // Jump-Menu
        for(Team team : getRegisteredTeams()) {

            Inventory inventory = getOrCreateJumpMenu(team);
            HashMap<Integer, JumpPoint> pointMap = jumpPointSlotMap.getOrDefault(team, new HashMap<>());
            if(!jumpPointSlotMap.containsKey(team)) { jumpPointSlotMap.put(team, pointMap); }
            pointMap.clear();

            int i = 0;
            ArrayList<JumpPoint> points = matchController.getJumpPoints(team);
            if(!points.isEmpty()) {

                for (int y = points.size() - 1; y < inventory.getSize(); y++) {

                    inventory.setItem(y, null);

                }
                for (JumpPoint point : points) {

                    inventory.setItem(i, point.getItem());
                    pointMap.put(i, point);
                    i++;

                }

            } else {

                inventory.clear();

            }

        }

        for(SplatoonPlayer player : queuedPlayerRemovals) {

            registeredPlayers.remove(player);
            hitableObjects.remove(player);
            matchController.playerRemoved(player);

        }
        for(GameObject object : queuedGameObjectRemovals) { objects.remove(object); hitableObjects.remove(object); matchController.objectRemoved(object); }
        queuedGameObjectRemovals.clear();

        for(GameObject object : objects) {

            object.onTick();

            if(object instanceof HitableEntity) {

                HitableEntity entity = (HitableEntity) object;
                AxisAlignedBB aabb = entity.aabb();
                SplatoonServer.broadcastColorParticle(getWorld(), aabb.minX, aabb.minY, aabb.minZ, registeredTeams.get(0).getColor(), 1f);
                SplatoonServer.broadcastColorParticle(getWorld(), aabb.maxX, aabb.maxY, aabb.maxZ, registeredTeams.get(0).getColor(), 1f);

            }

        }

        Iterator<DrippingInk> dripIterator = drippingInk.iterator();
        while (dripIterator.hasNext()) {

            DrippingInk ink = dripIterator.next();
            ink.ticksToPaint--;
            if(ink.ticksToPaint < 1) {

                dripIterator.remove();
                paint(ink.player, ink.position, ink.player.getTeam());

            }

        }

        projectiles.addAll(queuedProjectiles);
        queuedProjectiles.clear();
        Iterator<SplatoonProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {

            SplatoonProjectile projectile = iterator.next();
            if(projectile.getShooter() != null) {

                if(!projectile.getShooter().isValid() || (projectile.getShooter().getTeam() == null || projectile.getShooter().getMatch() != this)) {

                    iterator.remove();
                    continue;

                }

            }

            projectile.tick();
            if(projectile.isRemoved()) { iterator.remove(); }

        }
        iterateTrails();
        for(SplatoonPlayer player : registeredPlayers) {



        }

    }

    public ArrayList<HitableEntity> getHitableEntities() { return hitableObjects; }

    private ArrayList<SplatoonProjectile> projectiles = new ArrayList<>();
    public ArrayList<SplatoonProjectile> getProjectiles() { return projectiles; }

    public int getPaintedTurf(Team team) {

        return teamTurfCounter.getOrDefault(team, 0);

    }

    public int paintedBlockCount() {

        int c = 0;
        for(int i : teamTurfCounter.values()) {

            c+=i;

        }
        return c;

    }

    private TwoDimensionalMap<org.bukkit.Chunk> chunkCache = new TwoDimensionalMap<>();
    public org.bukkit.Chunk getOrLoad(int x, int z) {

        org.bukkit.Chunk chunk = chunkCache.get(x,z);
        if(chunk == null) {

            chunk = world.getChunkAt(x,z);
            chunkCache.set(x,z, chunk);

        }
        return chunk;

    }

    public void paint(SplatoonPlayer player, Vector vector, Team team) {

        final int x = (int)vector.getX();
        final int y = (int)vector.getY();
        final int z = (int)vector.getZ();

        if(isPaintable(team, x, y, z)) {

            BlockFlagManager.BlockFlag flag = blockFlagManager.getBlock(getOffset(), x, y, z);
            if(flag.isTrail()) {

                Iterator<TrailBlock> iterator = blocks.iterator();
                while (iterator.hasNext()) {

                    TrailBlock block1 = iterator.next();
                    if(block1.x == x && block1.y == y && block1.z == z) {

                        iterator.remove();
                        break;

                    }

                }
                flag.setTrail(false);

            }

            if(flag.hasSetTeam()) {

                Team team1 = colorFromTeamID(flag.getTeamID());
                if(team1 != null) {

                    decrementTurfCounter(team1);

                }

            }

            if(rollbackEnabled) {

                if(!rollbackMap.containsKey(new BlockPosition(x,y,z))) {

                    rollbackMap.put(new BlockPosition(x,y,z), nmsWorld().getType(new BlockPosition(x,y,z)));

                }

            }

            org.bukkit.Chunk chunk = getOrLoad(x>>4,z>>4);
            Chunk chunk1 = ((CraftChunk)chunk).getHandle();
            ChunkSection section = chunk1.getSections()[y>>4];

            if(section != null) {

                // Verhindert Lichtupdates
                section.setType(x&15,y&15,z&15,team.getColor().getWoolData());
                PacketContainer container = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
                container.getBlockPositionModifier().write(0, new com.comphenix.protocol.wrappers.BlockPosition(x,y,z));
                container.getBlockData().write(0, WrappedBlockData.fromHandle(team.getColor().getWoolData()));

                for(SplatoonHumanPlayer player1 : getHumanPlayers()) {

                    try {

                        ProtocolLibrary.getProtocolManager().sendServerPacket(player1.getPlayer(), container);

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                }

            }

            flag.setTeamID(team.getID());
            incrementTurfCounter(team);

            if(player != null) {

                boolean isWall = flag.isWall();
                if(!isWall) {

                    player.incrementPoints(BLOCK_POINT_RATIO);

                }

            }

        }

    }

    private Team colorFromTeamID(byte teamID) {

        for(Team team : getRegisteredTeams()) {

            if(team.getID() == teamID) { return team; }

        }
        return null;

    }

    public static final double BLOCK_POINT_RATIO = 0.5238125D;

    public net.minecraft.server.v1_13_R2.World nmsWorld() { return ((CraftWorld)world).getHandle(); }

    public Team getTeam(Color color) {

        for(Team team : getRegisteredTeams()) {

            if(team.getColor() == color) {

                return team;

            }

        }
        return null;

    }

    private boolean rollbackEnabled = false;
    public void enableRollback() { rollbackEnabled = true; }
    public boolean isRollbackEnabled() { return rollbackEnabled; }

    /*public void paint(Vector vector, SplatoonPlayer player) {

        paint(player, vector, player.getTeam());

        /*
        Block block = world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
        if(isPaintable(player.getTeam(), block)) {

            if(block.hasMetadata("Trail")) {

                Iterator<TrailBlock> iterator = blocks.iterator();
                while (iterator.hasNext()) {

                    TrailBlock block1 = iterator.next();
                    if(block1.x == block.getX() && block1.y == block.getY() && block1.z == block.getZ()) {

                        iterator.remove();
                        break;

                    }

                }
                block.removeMetadata("Trail", XenyriaSplatoon.getPlugin());

            }

            if(rollbackEnabled) {

                if(!rollbackMap.containsKey(new BlockPosition(block.getX(), block.getY(), block.getZ()))) {

                    rollbackMap.put(new BlockPosition(block.getX(), block.getY(), block.getZ()), nmsWorld().getType(new BlockPosition(
                            block.getX(), block.getY(), block.getZ()
                    )));

                }

            }

            block.setType(player.getTeam().getColor().getWool());
            block.setMetadata("Team", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), player.getTeam().getColor().name()));

        }*/

    //}

    private Map map = null;
    public Map getMap() { return map; }

    public boolean isPaintable(Team team, int x, int y, int z) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(x, y, z);
        if(flag != null) {

            if(flag.hasSetTeam()) {

                return flag.getTeamID() != team.getID();

            } else {

                return flag.isPaintable();

            }

        }
        return false;

    }

    public boolean isEnemyTurf(Block block, Team team) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        if(flag != null) {

            return flag.hasSetTeam() && flag.getTeamID() != team.getID();

        }
        return false;

    }

    private IntroManager introManager;
    public IntroManager getIntroManager() { return introManager; }
    public void initIntroManager() {

        introManager = new IntroManager(this);

    }

    private MatchAIManager manager;
    public MatchAIManager getAIController() {

        return manager;

    }

    public boolean isPaintable(Block block) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        return flag != null && flag.isPaintable();

    }

    public boolean inIntro() { return false; }

    public ArrayList<SplatoonHumanPlayer> getHumanPlayers() {

        ArrayList<SplatoonHumanPlayer> players = new ArrayList<>();
        for(SplatoonPlayer player : registeredPlayers) {

            if(player instanceof SplatoonHumanPlayer) {

                players.add((SplatoonHumanPlayer) player);

            }

        }
        return players;

    }

    public abstract MatchType getMatchType();

    public Location getSpawnPoint(SplatoonHumanPlayer player1) {

        Team team = player1.getTeam();
        int teamID = registeredTeams.indexOf(team);
        int playerTeamID = getPlayers(team).indexOf(player1);
        Map.TeamSpawn spawn = getMap().getSpawns().get(teamID);
        Vector[] offsets = Map.TeamSpawn.getSpawnPositionOffsets(spawn.getDirection());
        int indx = playerTeamID;
        if(indx > 3) {

            indx = 3;

        }

        Location location = spawn.getPosition().clone().add(offsets[indx]).add(.5, 0, .5);
        return location;

    }

    public Location getNextSpawnPoint(Team team) {

        int teamID = registeredTeams.indexOf(team);
        int newPlayerID = getPlayers(team).size();
        if(newPlayerID > 3) {

            newPlayerID = 3;

        }
        Map.TeamSpawn spawn = getMap().getSpawns().get(teamID);
        Vector[] offsets = Map.TeamSpawn.getSpawnPositionOffsets(spawn.getDirection());
        Location location = spawn.getPosition().clone().add(offsets[newPlayerID]).add(.5, 0, .5);
        return location;
    }

    public Location getNextSpawnPoint(Team team, int indx) {

        int teamID = registeredTeams.indexOf(team);
        int newPlayerID = indx;
        Map.TeamSpawn spawn = getMap().getSpawns().get(teamID);
        Vector[] offsets = Map.TeamSpawn.getSpawnPositionOffsets(spawn.getDirection());
        Location location = spawn.getPosition().clone().add(offsets[newPlayerID]).add(.5, 0, .5);
        return location;
    }


    private OutroManager outroManager;
    public void initOutroManager() {

        outroManager = new OutroManager(this);

    }

    public int indexOfPlayer(SplatoonPlayer player) {

        return registeredPlayers.indexOf(player);

    }

    public OutroManager getOutroManager() { return outroManager; }

    public boolean inOutro() {

        return false;

    }

    public void broadcast(String s) {

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.getPlayer().sendMessage(s);

        }

    }

    private Vector minBound,maxBound;
    public boolean boundsSet() {

        return minBound != null && maxBound != null;

    }
    public void updateBounds(Vector min, Vector max) {

        this.minBound = min;
        this.maxBound = max;

    }

    public Vector minBounds() { return minBound; }
    public Vector maxBounds() { return maxBound; }

    public ArrayList<Team> getEnemyTeams(Team team) {

        ArrayList<Team> teams = new ArrayList<>();
        for(Team team1 : registeredTeams) {

            if(team1 != team) {

                teams.add(team1);

            }

        }
        return teams;

    }

    public ArrayList<JumpPoint> getJumpPoints(EntityNPC npc) {

        return getMatchController().getJumpPoints(npc.getTeam());

    }

    public int getMaxEnemies(SplatoonPlayer player) {

        int i = 0;
        for(SplatoonPlayer player1 : getAllPlayers()) {

            if(!player1.isSpectator() && player1 != player && player1.getTeam() != player.getTeam()) {

                i++;

            }

        }
        return i;

    }

    public JumpPoint getJumpPointFor(SplatoonPlayer player) {

        for(JumpPoint point : getMatchController().getJumpPoints(player.getTeam())) {

            if(point instanceof JumpPoint.Player) {

                JumpPoint.Player player1 = (JumpPoint.Player)point;
                if(player1.getPlayer().equals(player)) {

                    return point;

                }

            }

        }
        return null;

    }

    public void clearAllObjects() {

        for(GameObject object : getGameObjects()) {

            object.reset();
            if(object instanceof RemovableGameObject) {

                ((RemovableGameObject) object).remove();

            }

        }
        getGameObjects().clear();
        for(SplatoonProjectile projectile : getProjectiles()) {

            projectile.remove();

        }
        getProjectiles().clear();
        manager = null;

        clearQueues();

    }

    public void handleMatchEnd() {

        XenyriaSplatoon.getXenyriaLogger().log("Es werden §e" + forceLoadedChunks.size() + " Chunks §7für §eMatch #" + id + " §7entladen.");
        for(ChunkCoordIntPair pair : forceLoadedChunks) {

            org.bukkit.Chunk chunk = world.getChunkAt(pair.x, pair.z);
            chunk.unload(false);

        }

    }

    private ArrayList<ChunkCoordIntPair> forceLoadedChunks = new ArrayList<>();
    public ArrayList<ChunkCoordIntPair> getForceLoadedChunks() { return forceLoadedChunks; }

    private HashMap<SplatoonPlayer, WeaponSet> usedWeaponSets = new HashMap<>();
    public void setUsedWeaponSet(SplatoonPlayer player, WeaponSet set) { usedWeaponSets.put(player, set); }
    public WeaponSet getUsedWeaponSet(SplatoonPlayer player1) { return usedWeaponSets.getOrDefault(player1, WeaponSetRegistry.getSet(1)); }

    public Vector getOffset() { return new Vector(); }

    public boolean isWall(Block block) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        return flag != null && flag.isWall();

    }

    public SplatoonPlayer getPlayerFromID(int id) {

        if(id <= (registeredPlayers.size()-1)) {

            return registeredPlayers.get(id);

        }
        return null;

    }

    public boolean inProgress() { return true; }

    public boolean hasAIController() { return manager != null; }

    public boolean belongsToTeam(Block block, Team team) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        return flag != null && flag.hasSetTeam() && flag.getTeamID() == team.getID();

    }

    public void reset() {

        // Turf zurücksetzen
        teamTurfCounter.clear();
        introManager = null;
        outroManager = null;
        usedWeaponSets.clear();
        blockFlagManager = new BlockFlagManager(this);
        blocks.clear();
        manager = null;
        jumpMenuMap.clear();
        jumpPointSlotMap.clear();
        map.getSpawns().clear();
        this.spawnedInkTanks.clear();
        this.drippingInk.clear();
        this.getRegisteredTeams().clear();
        Iterator<SplatoonPlayer> iterator = registeredPlayers.iterator();
        ArrayList<EntityNPC> remove = new ArrayList<>();
        while (iterator.hasNext()) {

            SplatoonPlayer player = iterator.next();
            if(player instanceof EntityNPC) {

                remove.add((EntityNPC) player);

            }

        }
        for(EntityNPC player : remove) {

            player.remove();

        }

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.setTeam(null);

        }

    }

    public Team getTeam(Block block) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        if(flag != null && flag.hasSetTeam()) {

            return colorFromTeamID(flag.getTeamID());

        }
        return null;

    }

    public boolean inLobbyPhase() { return false; }

    public abstract void removeBeacon(BeaconObject object);


    //public ArrayList<JumpPoint> getJumpPoints(SplatoonPlayer player) {


    //}

    private class DrippingInk {

        private Vector position;
        private SplatoonPlayer player;
        private int ticksToPaint = 0;

        public DrippingInk(Vector position, SplatoonPlayer player) {

            this.position = position;
            this.player = player;

            ticksToPaint = (int) (10 + (20 * new Random().nextFloat()));

        }

    }
    private ArrayList<DrippingInk> drippingInk = new ArrayList<>();

    public void dripInk(Vector vector, SplatoonPlayer player) {

        if(RandomUtil.random(20)) {

            DrippingInk ink = new DrippingInk(vector, player);
            drippingInk.add(ink);

        }

    }

    public void colorSquare(Block block, Team team, SplatoonPlayer player, int size) {

        int minX = Math.min(block.getX() - size, block.getX() + size);
        int minY = Math.min(block.getY() - size, block.getY() + size);
        int minZ = Math.min(block.getZ() - size, block.getZ() + size);
        int maxX = Math.max(block.getX() - size, block.getX() + size);
        int maxY = Math.max(block.getY() - size, block.getY() + size);
        int maxZ = Math.max(block.getZ() - size, block.getZ() + size);

        for(int x = minX; x <= maxX; x++) {

            for (int y = minY; y <= maxY; y++) {

                for (int z = minZ; z <= maxZ; z++) {

                    Vector vector = new Vector(x,y,z);
                    double dist = vector.distance(block.getLocation().toVector());
                    double percentage = ((size-dist) / ((float)size)) * 100d;
                    if(RandomUtil.random((int) percentage)) {

                        player.getMatch().paint(player, vector, team);

                    }

                }

            }

        }

    }

    public boolean isOwnedByTeam(Block block, Team team) {

        BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block.getX(), block.getY(), block.getZ());
        if(flag != null) {

            return flag.hasSetTeam() && flag.getTeamID() == team.getID();

        }
        return false;

    }

    public ArrayList<SplatoonPlayer> getPlayers(Team team1) {

        ArrayList<SplatoonPlayer> players = new ArrayList<>();
        for(SplatoonPlayer player : registeredPlayers) {

            if(player.getTeam() == team1) {

                players.add(player);

            }

        }
        return players;

    }

    private ArrayList<SplatoonPlayer> queuedPlayerRemovals = new ArrayList<>();
    private ArrayList<SplatoonProjectile> queuedProjectiles = new ArrayList<>();
    private ArrayList<GameObject> queuedGameObjectRemovals = new ArrayList<>();
    public void queueProjectile(SplatoonProjectile rocket) {

        queuedProjectiles.add(rocket);

    }
    public void queuePlayerRemoval(SplatoonPlayer player) {

        queuedPlayerRemovals.add(player);

        if(player instanceof EntityNPC) {

            Iterator<EntityNPC> iterator = EntityNPC.getNPCs().iterator();
            while (iterator.hasNext()) {

                if(iterator.next().equals(player)) {

                    iterator.remove();

                }

            }

        }

    }

    public void queueObjectRemoval(GameObject object) {

        queuedGameObjectRemovals.add(object);

    }

    public ArrayList<GameObject> getGameObjects() { return objects; }

    public JumpPoint jumpPointForSlot(Team team, int slot) {

        if(jumpPointSlotMap.containsKey(team)) {

            return jumpPointSlotMap.get(team).getOrDefault(slot, null);

        }
        return null;

    }

    public class TrailBlock {

        private Team team;
        private final int x,y,z;
        private IBlockData dataBefore = null;

        public TrailBlock(Team team, IBlockData dataBefore, int x, int y, int z) {

            this.team = team;
            this.dataBefore = dataBefore;
            this.x = x;
            this.y = y;
            this.z = z;

            remainingTicks = (int) (10 + (new Random().nextFloat() * 10));

        }

        private int remainingTicks = 0;

        @Override
        public boolean equals(Object obj) {

            return ((TrailBlock)obj).x == x && ((TrailBlock)obj).y == y && ((TrailBlock)obj).z == z;

        }

        @Override
        public int hashCode() {

            return new HashCodeBuilder().append(x).append(y).append(z).toHashCode();

        }
    }

    private ArrayList<TrailBlock> blocks = new ArrayList<>();

    public void markTrail(Block relative, Team team) {

        if(relative.getType() == team.getColor().getSponge()) { return; }

        World world = relative.getWorld();
        WorldServer server = ((CraftWorld)world).getHandle();
        TrailBlock block = new TrailBlock(team, server.getType(new BlockPosition(relative.getX(), relative.getY(), relative.getZ())), relative.getX(), relative.getY(), relative.getZ());
        relative.setType(team.getColor().getClay());
        if(!blocks.contains(block)) {

            BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(relative.getX(), relative.getY(), relative.getZ());
            flag.setTrail(true);
            blocks.add(block);

        }

    }

    public void iterateTrails() {

        Iterator<TrailBlock> blockIterator = blocks.iterator();
        while (blockIterator.hasNext()) {

            TrailBlock block = blockIterator.next();
            if(block.remainingTicks > 0) {

                block.remainingTicks--;
                if(block.remainingTicks < 1) {

                    blockIterator.remove();
                    WorldServer server = ((CraftWorld)world).getHandle();
                    server.setTypeUpdate(new BlockPosition(block.x, block.y, block.z), block.dataBefore);
                    Block block1 = world.getBlockAt(block.x, block.y, block.z);
                    BlockFlagManager.BlockFlag flag = blockFlagManager.getBlockIfExist(block1.getX(), block1.getY(), block1.getZ());
                    if(flag != null) {

                        flag.setTrail(false);

                    }

                    //if(block1.hasMetadata("Trail")) { block1.removeMetadata("Trail", XenyriaSplatoon.getPlugin()); }

                }

            }

        }

    }

}
