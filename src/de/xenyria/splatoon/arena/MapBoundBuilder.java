package de.xenyria.splatoon.arena;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MapBoundBuilder {

    private Vector offset = new Vector(0,0,0);
    public Vector getOffset() { return offset; }

    public int openSize() { return open.size(); }
    public int foundSize() { return paintableSurfaces.size(); }

    public class BlockPos {

        public final int x,y,z;
        public BlockPos(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            return ((BlockPos)obj).x == x && ((BlockPos)obj).y == y && ((BlockPos)obj).z == z;
        }

        @Override
        public int hashCode() { return new HashCodeBuilder().append(x).append(y).append(z).toHashCode(); }

    }

    private BlockFace[] faces = new BlockFace[]{

            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST

    };

    private int minX,minY,minZ,maxX,maxY,maxZ;
    private Location origin;

    public Vector localize(int x, int y, int z) {

        return new Vector(x-offset.getX(), y-offset.getY(), z-offset.getZ());

    }

    public MapBoundBuilder(Vector offset, Location location, Vector a, Vector b) {

        this.offset = offset;
        this.origin = localize((int)location.getX(), (int)location.getY(), (int)location.getZ()).toLocation(location.getWorld());

        Vector min = localize((int)a.getX(), (int)a.getY(), (int)a.getZ());
        Vector max = localize((int)b.getX(), (int)b.getY(), (int)b.getZ());

        minX = (int)Math.min(min.getX(), max.getX());
        minY = (int)Math.min(min.getY(), max.getY());
        minZ = (int)Math.min(min.getZ(), max.getZ());
        maxX = (int)Math.max(min.getX(), max.getX());
        maxY = (int)Math.max(min.getY(), max.getY());
        maxZ = (int)Math.max(min.getZ(), max.getZ());

    }

    private ArrayList<AxisAlignedBB> aabbs = new ArrayList<>();
    private ArrayList<BlockPos> iteratedBlockPosList = new ArrayList<>();

    private  boolean bool = false;
    public void highlightAABBs() {

        if(!bool) {

            for(Player player : Bukkit.getOnlinePlayers()) {

                player.sendMessage("aabbs: " + aabbs.size());
                for(BlockPos pos : paintableSurfaces) {

                    player.sendBlockChange(new Location(player.getWorld(), offset.getX() + pos.x, offset.getY() + pos.y, offset.getZ() + pos.z), CraftBlockData.newData(Material.BLACK_STAINED_GLASS, ""));
                }

            }
            bool = true;

        }


    }

    public boolean passable(Material material) {

        return material.isEmpty() || material.name().contains("CARPET") || AABBUtil.isPassable(material);

    }

    public boolean vectorsGenerated() {

        return open.isEmpty();

    }

    private ArrayList<BlockPos> paintableSurfaces = new ArrayList<>();
    public ArrayList<BlockPos> getPaintableSurfaces() { return paintableSurfaces; }

    private ThreeDimensionalArray<Material> materialArray = new ThreeDimensionalArray<>();
    private HashMap<ChunkCoordIntPair, net.minecraft.server.v1_13_R2.Chunk> chunkMap = new HashMap<>();

    public Material getMaterial(int x, int y, int z) {

        ChunkCoordIntPair pair = new ChunkCoordIntPair(((int)offset.getX() + x) >> 4, ((int)offset.getZ() + z) >> 4);
        if(!chunkMap.containsKey(pair)) {

            World world = XenyriaSplatoon.getArenaProvider().getArenaWorld();
            WorldServer server = ((CraftWorld)world).getHandle();
            net.minecraft.server.v1_13_R2.Chunk chunk = server.getChunkAtWorldCoords(new BlockPosition(
                    (int)offset.getX() + x, (int)offset.getY() + y, (int)offset.getZ() + z
            ));

            chunkMap.put(pair, chunk);

            return CraftBlockData.fromData(chunkMap.get(pair).getBlockData(new BlockPosition((int)offset.getX() + x, (int)offset.getY() + y, (int)offset.getZ() + z))).getMaterial();

        } else {

            return CraftBlockData.fromData(chunkMap.get(pair).getBlockData(new BlockPosition((int)offset.getX() + x, (int)offset.getY() + y, (int)offset.getZ() + z))).getMaterial();

        }

    }

    public class BoundNode {

        private final int x,y,z;

        private BlockFace lastDir = null;
        public BlockFace getLastDir() { return lastDir; }
        public void setLastDir(BlockFace face) { this.lastDir = face; }

        public BoundNode(int x, int y, int z, boolean passable) {

            this.x = x;
            this.y = y;
            this.z = z;
            this.passable = passable;

        }

        private boolean passable;

        private boolean closed;
        public boolean isClosed() { return closed; }
        public void close() { closed = true; }


    }
    private ThreeDimensionalArray<BoundNode> grid = new ThreeDimensionalArray<>();
    public boolean doesNodeExist(int x, int y, int z) {

        if(grid.exists(x,y,z)) {

            return true;


        }
        return false;

    }
    public BoundNode getNode(int x, int y, int z) {

        BoundNode node = grid.get(x,y,z);
        if(node != null) { return node; } else {

            node = new BoundNode(x,y,z, passable(XenyriaSplatoon.getArenaProvider().getArenaWorld().getBlockAt(
                    ((int)offset.getX() + x),
                    ((int)offset.getY() + y),
                    ((int)offset.getZ() + z)
            ).getType()));
            grid.set(node, x,y,z);
            return node;

        }

    }

    private ArrayList<BoundNode> open = new ArrayList<>();

    public BoundNode[] getNeighbours(BoundNode base) {

        BoundNode[] nodes = new BoundNode[6];
        int i = 0;
        for(BlockFace face : faces) {

            int targetX = base.x+face.getModX();
            int targetY = base.y+face.getModY();
            int targetZ = base.z+face.getModZ();

            if(face != base.lastDir && targetX >= minX && targetX <= maxX && targetY >= minY && targetY <= maxY && targetZ >= minZ && targetZ <= maxZ) {

                BoundNode node = getNode(targetX, targetY, targetZ);
                if(node.closed) { continue; }

                nodes[i] = node;
                i++;

            }

        }
        return nodes;

    }

    private ArrayList<BlockPos> ignoredPos = new ArrayList<>();

    public void generatePossibleVectors() {

        int iters = 0;
        if(open.isEmpty()) {

            open.add(getNode((int) origin.getX(), (int) origin.getY(), (int) origin.getZ()));

        }
        while (!open.isEmpty()) {

            BoundNode base = open.get(0);
            base.closed = true;
            open.remove(0);
            iters++;

            for(BoundNode node : getNeighbours(base)) {

                if(node != null) {

                    if (node.passable) {

                        if(!open.contains(node)) {

                            open.add(node);

                        }

                    } else {

                        BlockPos pos = new BlockPos(node.x, node.y, node.z);
                        if (!paintableSurfaces.contains(pos)) {

                            paintableSurfaces.add(new BlockPos(node.x, node.y, node.z));

                        }


                    }

                }

            }

        }

        /*
        BlockPos pos = new BlockPos((int)origin.getX(), (int)origin.getY(), (int)origin.getZ());
        remainingPositions.add(pos);
        int iterCount = 0;

        while (!remainingPositions.isEmpty() && iterCount < 3000) {

            iterCount++;
            BlockPos pos1 = remainingPositions.get(0);
            remainingPositions.remove(0);

            for (BlockFace face : faces) {

                int targetX = pos1.x+face.getModX();
                int targetY = pos1.y+face.getModY();
                int targetZ = pos1.z+face.getModZ();
                BlockPos newPos = new BlockPos(targetX, targetY, targetZ);

                if(!ignoredPos.contains(newPos) && !remainingPositions.contains(newPos)) {

                    if (targetX >= minX && targetX <= maxX && targetY >= minY && targetY <= maxY && targetZ >= minZ && targetZ <= maxZ) {

                        if (passable(getMaterial(targetX, targetY, targetZ))) {

                            /*if(iterCount >= 2999) {

                                XenyriaSplatoon.getArenaProvider().getArenaWorld().spawnParticle(
                                        Particle.VILLAGER_HAPPY, new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                                                targetX, targetY, targetZ), 0
                                );

                            }*/

                            /*remainingPositions.add(newPos);

                        } else {

                            if(!paintableSurfaces.contains(newPos)) { paintableSurfaces.add(newPos); }
                            ignoredPos.add(newPos);

                        }

                    }

                }

            }

        }*/

    }

}
