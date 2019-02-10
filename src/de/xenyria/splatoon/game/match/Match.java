package de.xenyria.splatoon.game.match;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.mysql.fabric.xmlrpc.base.Array;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.ai.MatchAIManager;
import de.xenyria.splatoon.game.match.intro.IntroManager;
import de.xenyria.splatoon.game.match.outro.OutroManager;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Sprinkler;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.TentaMissleRocket;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.RandomUtil;
import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public abstract class Match {


    private World world;
    public World getWorld() { return world; }

    private MatchControlInterface matchController;
    public MatchControlInterface getMatchController() { return matchController; }
    public void setMatchController(MatchControlInterface matchControlInterface) { this.matchController = matchControlInterface; }

    private HashMap<BlockPosition, IBlockData> rollbackMap = new HashMap<>();

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

            if(block.hasMetadata("Trail")) {

                block.removeMetadata("Trail", XenyriaSplatoon.getPlugin());
                Iterator<TrailBlock> trailBlockIterator = blocks.iterator();
                while (trailBlockIterator.hasNext()) {

                    TrailBlock block1 = trailBlockIterator.next();
                    if(block1.x == entry.getKey().getX() && block1.y == entry.getKey().getY() && block1.z == entry.getKey().getZ()) {

                        trailBlockIterator.remove();

                    }

                }

            }
            if(block.hasMetadata("Team")) {

                block.removeMetadata("Team", XenyriaSplatoon.getPlugin());

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

    public Match(World world) {

        this.world = world;
        map = new Map();
        //map.getPaintDefinition().getPaintableMaterials().add(Material.STONE);

        manager = new MatchAIManager(this);
        registeredMatches.add(this);

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

    public void addPlayer(SplatoonPlayer player) { hitableObjects.add(player); registeredPlayers.add(player); matchController.playerAdded(player); }
    public void removePlayer(SplatoonPlayer player) { hitableObjects.remove(player); registeredPlayers.remove(player); matchController.playerRemoved(player); }
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

        getAIController().tick();
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
                paint(ink.position, ink.player);

            }

        }

        projectiles.addAll(queuedProjectiles);
        queuedProjectiles.clear();
        Iterator<SplatoonProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {

            SplatoonProjectile projectile = iterator.next();
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

    public void paint(SplatoonPlayer player, Vector vector, Team team) {

        Block block = world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
        if(isPaintable(team, block)) {

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
            block.setType(team.getColor().getWool());
            block.setMetadata("Team", new FixedMetadataValue(XenyriaSplatoon.getPlugin(),  team.getColor().name()));

            if(player != null) {

                boolean isWall = block.hasMetadata("Wall") && block.getMetadata("Wall").get(0).asBoolean();
                if(!isWall) {

                    player.incrementPoints(1d);

                }

            }

        }

    }

    public net.minecraft.server.v1_13_R2.World nmsWorld() { return ((CraftWorld)world).getHandle(); }

    private boolean rollbackEnabled = true;
    public boolean isRollbackEnabled() { return rollbackEnabled; }

    public void paint(Vector vector, SplatoonPlayer player) {

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

        }

    }

    private Map map = null;
    public Map getMap() { return map; }

    public boolean isPaintable(Team team, Block block) {

        if(block.hasMetadata("Trail")) {

            Color color = Color.valueOf(block.getMetadata("Trail").get(0).asString());
            Team team1 = colorToTeamMap.get(color);
            if(!team1.equals(team)) { return true; }

        }
        if(block.hasMetadata("Team")) {

            if(block.getType().name().contains("CONCRETE")) { return false; }

            Color color = Color.valueOf(block.getMetadata("Team").get(0).asString());
            if(color == team.getColor()) { return false; } else {

                return true;

            }

        }
        return block.hasMetadata("Paintable");

    }

    public boolean isEnemyTurf(Block block, Team team) {

        if(block.hasMetadata("Trail")) {

            Color color = Color.valueOf(block.getMetadata("Trail").get(0).asString());
            Team team1 = colorToTeamMap.get(color);
            if(!team1.equals(team)) { return true; }

        }
        if(block.hasMetadata("Team")) {

            Color color = Color.valueOf(block.getMetadata("Team").get(0).asString());
            if(color == team.getColor()) { return false; } else {

                return true;

            }

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

        return block.hasMetadata("Paintable");

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

                        player.getMatch().paint(vector, player);

                    }

                }

            }

        }

    }

    public boolean isOwnedByTeam(Block block, Team team) {

        if(block.getType().equals(team.getColor().getWool()) || block.getType().equals(team.getColor().getSponge())) {

            return true;

        } else {

            if(block.hasMetadata("Trail")) {

                Color color = Color.valueOf(block.getMetadata("Trail").get(0).asString());
                System.out.println("Col: " + color);
                Team foundTeam = colorToTeamMap.get(color);
                return foundTeam.equals(team);

            }

        }
        return false;

    }

    public ArrayList<SplatoonPlayer> getPlayers(Team team1) {

        ArrayList<SplatoonPlayer> players = new ArrayList<>();
        for(SplatoonPlayer player : registeredPlayers) {

            if(player.getTeam().equals(team1)) {

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

            blocks.add(block);
            relative.setMetadata("Trail", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), team.getColor().name()));

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

                    if(block1.hasMetadata("Trail")) { block1.removeMetadata("Trail", XenyriaSplatoon.getPlugin()); }

                }

            }

        }

    }

}
