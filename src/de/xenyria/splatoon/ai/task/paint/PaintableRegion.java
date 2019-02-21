package de.xenyria.splatoon.ai.task.paint;

import com.google.common.hash.HashCode;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.match.ai.MatchAIManager;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.BlockPosition;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class PaintableRegion {

    public PaintableRegion(World world, MatchAIManager manager, double centerX, double centerY, double centerZ) {

        minX = (int) Math.min(centerX-2.5,centerX+2.5);
        minY = (int) Math.min(centerY-2.5,centerY+2.5);
        minZ = (int) Math.min(centerZ-2.5,centerZ+2.5);
        maxX = (int) Math.max(centerX-2.5,centerX+2.5);
        maxY = (int) Math.max(centerY-2.5,centerY+2.5);
        maxZ = (int) Math.max(centerZ-2.5,centerZ+2.5);
        this.manager = manager;
        center = new Vector(centerX, centerY, centerZ);

    }

    public static void main(String[] args) {

        Coordinate coordinate = Coordinate.fromWorldCoordinates(-6, -1, 0);
        System.out.println(coordinate.x + " " + coordinate.y + " " + coordinate.z);

    }

    public int foundBlocks() { return paintableBlocks.size(); }
    public ArrayList<Block> getPaintableBlocks() { return paintableBlocks; }

    public Vector getMax() { return new Vector(maxX, maxY, maxZ); }
    public Vector getMin() { return new Vector(minX, minY, minZ); }

    public Coordinate getCoordinate() {

        return Coordinate.fromWorldCoordinates(center.getBlockX(), center.getBlockY(), center.getBlockZ());

    }

    private ArrayList<Node> floorCoordinates = new ArrayList<>();
    public ArrayList<Node> getFloorCoordinates() { return floorCoordinates; }

    public static class Coordinate {

        public static Coordinate fromWorldCoordinates(int x, int y, int z) {

            return new Coordinate((int)Math.floor((double)x/5), (int)Math.floor((double)y/5), (int)Math.floor((double)z/5));

        }

        private final int x,y,z;
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }

        public Coordinate(int x, int y, int z) {

            this.x = x;
            this.y = y;
            this.z = z;

        }

        @Override
        public boolean equals(Object obj) {
            return  ((Coordinate)obj).x == x &&
                    ((Coordinate)obj).y == y &&
                    ((Coordinate)obj).z == z;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(x).append(y).append(z).toHashCode();
        }

        public Vector center() {
            return new Vector((x*5) + 2.5, (y*5) + 2.5, (z*5) + 2.5);
        }
    }

    private Vector start, end;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    private ArrayList<Block> paintableBlocks = new ArrayList<>();

    private MatchAIManager manager;
    public MatchAIManager getManager() { return manager; }

    public int getUnpaintedBlocks() {

        int amount = 0;
        for(Block block : paintableBlocks) {

            if(!block.hasMetadata("Team")) {

                amount++;

            }

        }
        return amount;

    }
    public Block[] getPaintableBlocks(Color color) {

        String teamName = color.name();
        ArrayList<Block> enemy = new ArrayList<>();
        ArrayList<Block> unpainted = new ArrayList<>();
        for(Block block : paintableBlocks) {

            BlockFlagManager.BlockFlag flag = manager.getMatch().getBlockFlagManager().getBlockIfExist(block.getX(), block.getY(), block.getZ());
            if(!flag.hasSetTeam()) {

                if(flag.isPaintable()) {

                    unpainted.add(block);

                }

            } else {

                Team team = manager.getMatch().getTeam(block);
                if(!teamName.equalsIgnoreCase(team.getColor().name())) {

                    enemy.add(block);

                }

            }

        }
        Block[] paintableBlocks = new Block[enemy.size() + unpainted.size()];
        int i = 0;
        for(Block block : enemy) { paintableBlocks[i] = block; i++; }
        for(Block block : unpainted) { paintableBlocks[i] = block; i++; }
        return paintableBlocks;

    }

    private Vector center = new Vector();
    public Vector getCenter() { return center; }

    public HashMap<Block, ArrayList<Vector>> visibleFrom = new HashMap<>();

    public PaintableRegion(World world, MatchAIManager manager, Vector start, Vector end, ArrayList<Node> possiblePositions) {

        this.start = start;
        this.end = end;
        this.manager = manager;
        minX = (int) Math.min(start.getX() - 2, end.getX()) - 2;
        minY = (int) Math.min(start.getY() - 2, end.getY()) - 2;
        minZ = (int) Math.min(start.getZ() - 2, end.getZ()) - 2;
        maxX = (int) Math.max(start.getX() - 2, end.getX()) + 2;
        maxY = (int) Math.max(start.getY() - 2, end.getY()) + 2;
        maxZ = (int) Math.max(start.getZ() - 2, end.getZ()) + 2;
        double w1 = maxX - minX;
        double w2 = maxY - minY;
        double w3 = maxZ - minZ;
        center = new Vector(minX + (w1/2), minY + (w2/2), minZ + (w3/2));

        for(int x = minX; x <= maxX; x++) {

            for(int y = minY; y <= maxY; y++) {

                for(int z = minZ; z <= maxZ; z++) {

                    Block block = world.getBlockAt(x,y,z);
                    BlockFace[] faces = new BlockFace[]{
                            BlockFace.UP,
                            BlockFace.DOWN,
                            BlockFace.NORTH,
                            BlockFace.EAST,
                            BlockFace.SOUTH,
                            BlockFace.WEST
                    };

                    if(manager.getMatch().isPaintable(block)) {

                        int blockedFaceCounter = 0;
                        for(BlockFace face : faces) {

                            Block cursor = block.getRelative(face);
                            if(cursor.getType() != Material.AIR) {

                                blockedFaceCounter++;

                            }

                        }
                        //if(blockedFaceCounter <= 4) {

                            paintableBlocks.add(block);

                        //}

                    }

                }

            }

        }

        int possibleNodes = possiblePositions.size();
        HashMap<Block, Integer> blockVisibilityScore = new HashMap<>();
        int hits = 0;
        ArrayList<BlockPosition> iteratedPositions = new ArrayList<>();
        for(Block block : paintableBlocks) {

            for(Node node : possiblePositions) {

                if(node.x >= minX && node.y >= minY && node.z >= minZ && node.x <= maxX && node.y <= maxY && node.z <= maxZ) {

                    if(!iteratedPositions.contains(new BlockPosition(node.x, node.y, node.z))) {

                        iteratedPositions.add(new BlockPosition(node.x, node.y, node.z));
                        if (node.getType() == TransitionType.WALK) {

                            floorCoordinates.add(node);

                        }

                        Vector vector = node.toVector().clone().add(new Vector(0, 1.62, 0));
                        if (hasLineOfSight(manager.getMatch().getWorld(), vector, block)) {

                            hits++;
                            if (visibleFrom.containsKey(block)) {

                                visibleFrom.get(block).add(vector);

                            } else {

                                ArrayList<Vector> vectors = new ArrayList<>();
                                vectors.add(vector);
                                visibleFrom.put(block, vectors);

                            }

                        }
                        blockVisibilityScore.put(block, hits);

                    }

                }

            }

        }

        TreeMap<Block, Integer> sortedBlockList = new TreeMap<>(new Comparator<Block>() {
            @Override
            public int compare(Block o1, Block o2) {

                return Integer.compare(blockVisibilityScore.get(o1), blockVisibilityScore.get(o2));

            }
        });
        sortedBlockList.putAll(blockVisibilityScore);

        paintableBlocks.clear();
        int minAvailabilityCount = 2;

        for(Map.Entry<Block, Integer> entry : sortedBlockList.entrySet()) {

            if(entry.getValue() >= minAvailabilityCount) {

                paintableBlocks.add(entry.getKey());

            }

        }

        // Sort
        ArrayList<Block> sortedBlocks = new ArrayList<>();
        if(sortedBlocks.size() != paintableBlocks.size()) {

            sortedBlocks.add(paintableBlocks.get(0));
            int iterCount = 0;

            while (sortedBlocks.size() != paintableBlocks.size()) {

                Block lastBlock = sortedBlocks.get(sortedBlocks.size() - 1);

                double highestDistYet = 0d;
                Block blockWithHighestDist = null;

                for(Block block : paintableBlocks) {

                    if(!sortedBlocks.contains(block)) {

                        double newDist = block.getLocation().distance(lastBlock.getLocation());

                        if (blockWithHighestDist == null || newDist > highestDistYet) {

                            blockWithHighestDist = block;
                            highestDistYet = newDist;

                        }

                    }

                }
                if(blockWithHighestDist != null) {

                    sortedBlocks.add(blockWithHighestDist);

                }
                iterCount++;

            }

            paintableBlocks = sortedBlocks;

        }

    }

    public static boolean hasLineOfSight(World world, Vector position, Block target) {

        Vector targetVec = target.getLocation().toVector().add(new Vector(.5, .5, .5));

        Vector direction = targetVec.subtract(position).normalize();
        if(VectorUtil.isValid(direction)) {

            RayTraceResult result = world.rayTraceBlocks(position.toLocation(world), direction, targetVec.distance(position));
            if(result != null) {

                Block block = result.getHitBlock();
                if(block != null) {

                    if(block.getX() == target.getX() && block.getY() == target.getY() && block.getZ() == target.getZ()) {

                        return true;

                    }

                }

            }
            return false;

        } else {

            return false;

        }

    }

    private HashMap<Byte, Integer> teamToTurfCounter = new HashMap<>();
    public void incrementTurfCounter(byte team) {

        if(!teamToTurfCounter.containsKey((team))) {

            teamToTurfCounter.put(team, 1);

        } else {

            teamToTurfCounter.put(team, teamToTurfCounter.get(team)+1);

        }

    }

    public int getCoveredBlockCount(Team team) {

        int counter = 0;
        String colorName = team.getColor().name();
        for(Block block : paintableBlocks) {

            if(!manager.getMatch().isWall(block)) {

                if (manager.getMatch().belongsToTeam(block, team)) {

                    counter++;

                }

            }

        }
        return counter;

    }

    public double coverage(Team team) {

        int painted = getCoveredBlockCount(team);
        int total = paintableBlocks.size();

        return ((double)painted / (double)total) * 100d;

    }

}
