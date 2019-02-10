package de.xenyria.splatoon.ai.pathfinding.grid;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;

import java.util.ArrayList;
import java.util.List;

public class NodeGrid {

    public static ArrayList<Material> UNSAFE_MATERIALS = new ArrayList<>();
    static {

        UNSAFE_MATERIALS.add(Material.IRON_BARS);
        for(Material material : Material.values()) {

            String name = material.name().toUpperCase();
            if(name.contains("TRAP_DOOR") || name.contains("BUTTON")) { UNSAFE_MATERIALS.add(material); }

        }
        UNSAFE_MATERIALS.remove(Material.IRON_TRAPDOOR);

    }

    public NodeGrid(SquidAStar aStar) {

        this.pathfinder = aStar;

    }

    private ThreeDimensionalArray<Node> nodes = new ThreeDimensionalArray<>();
    private ThreeDimensionalArray<Boolean> passableMaterials = new ThreeDimensionalArray<>();

    public ThreeDimensionalArray<Node> getNodes() { return nodes; }

    private ThreeDimensionalArray<AxisAlignedBB[]> hitboxCache = new ThreeDimensionalArray<>();

    private SquidAStar pathfinder;
    public org.bukkit.World getWorld() { return pathfinder.getWorld(); }

    public Node fallCheck(int x, int y, int z, BoundingBoxDimensions dimensions) {

        for(int i = y; i > 0; i--) {

            Result result = isValidPosition(x, i, z, dimensions);
            if(result == Result.OK) {

                return getNode(x, i, z);

            } else if(result == Result.NO_SPACE) {

                return null;

            }

        }
        return null;

    }

    public static final BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};

    public Node swimCheck(int nX, int nY, int nZ, BoundingBoxDimensions dimensions, Team team, SquidAStar star, boolean canClimbUnpainted) {

        org.bukkit.World world = star.getWorld();

        if(team == null) {

            canClimbUnpainted = true;

        }

        Block base = star.getWorld().getBlockAt(nX, nY-1, nZ);
        BlockFace preferredFace = null;
        for(BlockFace face : faces) {

            Block block = base.getRelative(face);
            if((team != null && star.getMatch().isOwnedByTeam(block, team)) || (canClimbUnpainted && star.getMatch().isPaintable(block))) {

                preferredFace = face;
                break;

            }

        }

        ArrayList<BlockPosition> wallSwimReferences = new ArrayList<>();
        if(preferredFace != null) {

            int climbed = 0;
            int y = nY;
            Node lastNode = null;
            while (true) {

                if (y >= 256) {
                    return null;
                }

                Block block = world.getBlockAt(nX, y, nZ).getRelative(preferredFace);
                boolean colored = false;
                if(team != null) {

                    colored = star.getMatch().isOwnedByTeam(block, star.getTeam());

                }

                if(!canClimbUnpainted) {

                    if (colored) {

                        wallSwimReferences.add(new BlockPosition(nX, y, nZ));

                    }

                } else {

                    colored = pathfinder.getMatch().isPaintable(block);
                    if (colored) {

                        wallSwimReferences.add(new BlockPosition(nX, y, nZ));

                    }

                }

                //if (star.getMatch().isOwnedByTeam(block, star.getTeam())) {
                double cenX = nX + .5;
                double cenZ = nZ + .5;
                double wH = dimensions.width / 2f;

                AxisAlignedBB aabb = new AxisAlignedBB(cenX - wH, y, cenZ - wH, cenX + wH, y + dimensions.height, cenZ + wH);
                if (hasSpace(aabb, true)) {

                    Result result = isValidPosition(nX, y, nZ, dimensions);
                    if (result == Result.OK) {

                        Node node = getNode(nX, y, nZ);
                        node.getAdditionalData().put("wallNodes", wallSwimReferences);

                        if(climbed >= 1) {

                            return node;

                        } else {

                            node.getAdditionalData().clear();
                            return null;

                        }

                    }

                    if (!colored) {

                        for (BlockFace face : faces) {

                            Result result1 = isValidPosition(nX + face.getModX(), y, nZ + face.getModZ(), dimensions);
                            if (result1 == Result.OK) {

                                Node node = getNode(nX + face.getModX(), y, nZ + face.getModZ());
                                node.getAdditionalData().put("wallNodes", wallSwimReferences);
                                if(climbed >= 1) {

                                    return node;

                                } else {

                                    node.getAdditionalData().clear();
                                    return null;

                                }

                            }

                        }
                        return null;

                    }

                } else {

                    return null;

                }

                y++;
                climbed++;

            }

        }

        return null;

    }

    public static class BoundingBoxDimensions {

        public double width = 0.3d, height = 1.8;

    }

    public enum Result {

        OK,
        OK_SQUID,
        NO_SPACE,
        NO_FLOOR;

    }
    public Result isValidPosition(Node node, BoundingBoxDimensions bb) {

        return isValidPosition(node.x, node.y, node.z, bb);

    }

    public Result isValidPosition(int x, int y, int z, BoundingBoxDimensions bb) {

        double cenX = x + .5;
        double cenZ = z + .5;

        double height = getHeightAt(x,y,z);
        boolean standFlag = isStandable(x,y,z);
        double totalHeight = height+y;


        // In einem Block stehen ist nur dann möglich wenn dieser eine Höhe von weniger als 1 hat.
        if(height < 1 && height > 0) {

            if(hasSpace(new AxisAlignedBB(cenX - (bb.width / 2), totalHeight, cenZ - (bb.width / 2),
                    cenX + (bb.width / 2), (totalHeight + bb.height) + bb.height, cenZ + (bb.width / 2d)), false)) {

                if(standFlag) {

                    return Result.OK;

                } else {

                    return Result.NO_FLOOR;

                }

            } else {

                return Result.NO_SPACE;

            }

        } else if (height == 0) {

            double heightBelow = getHeightAt(x, y - 1, z);
            boolean bottomStandable = isStandable(x,y-1,z);
            if(heightBelow == 1d && bottomStandable) {

                if(hasSpace(new AxisAlignedBB(cenX - (bb.width / 2), totalHeight, cenZ - (bb.width / 2),
                        cenX + (bb.width / 2), totalHeight + bb.height, cenZ + (bb.width / 2d)), false)) {

                    return Result.OK;

                } else {

                    return Result.NO_SPACE;

                }

            } else {

                if(hasSpace(new AxisAlignedBB(cenX - (bb.width / 2), totalHeight, cenZ - (bb.width / 2),
                        cenX + (bb.width / 2), totalHeight + bb.height, cenZ + (bb.width / 2d)), false)) {

                    return Result.NO_FLOOR;

                } else {

                    return Result.NO_SPACE;

                }

            }

        } else {

            return Result.NO_SPACE;

        }

    }

    private boolean isStandable(int x, int y, int z) {

        double highestY = 0d;
        AxisAlignedBB[] bbs = getHitboxes(x,y,z);

        if(bbs.length == 1) {

            AxisAlignedBB bb = bbs[0];
            double w1 = bb.maxX - bb.minX;
            double w2 = bb.maxZ - bb.minZ;

            double min = 1;
            if((w1*w2) < min) {

                return false;

            }

        }
        return true;

    }

    public boolean isPassable(int x, int y, int z) {

        return AABBUtil.isPassable(getWorld().getBlockAt(x,y,z).getType());

    }
    public boolean isValidInSquidForm(int x, int y, int z, BoundingBoxDimensions bb) {

        double cenX = x + .5;
        double cenZ = z + .5;

        boolean passable = isPassable(x, y-1, z);
        double height = getHeightAt(x,y,z);
        double totalHeight = height+y;
        if(height == 1 || passable) {

            if (hasSpace(new AxisAlignedBB(cenX - (bb.width / 2), totalHeight, cenZ - (bb.width / 2),
                    cenX + (bb.width / 2), totalHeight + bb.height, cenZ + (bb.width / 2d)), true)) {

                double heightBelow = getHeightAt(x, y - 1, z);
                if(heightBelow == 1d && !isPassable(x, y - 1, z)) {

                    return true;

                }

            } else {

                return false;

            }

        }
        return false;

    }

    private ThreeDimensionalArray<Boolean> knownSpaceQueriesNormal = new ThreeDimensionalArray<>();
    private ThreeDimensionalArray<Boolean> knownSpaceQueriesSquidForm = new ThreeDimensionalArray<>();

    public boolean hasSpace(AxisAlignedBB bb, boolean squidForm) {

        int cenX = (int) ((bb.maxX - bb.minX) / 2);
        int cenY = (int) bb.minY;
        int cenZ = (int) ((bb.maxZ - bb.minZ) / 2);

        if(!squidForm) {

            double minX = Math.min(Math.floor(bb.minX), Math.ceil(bb.maxX));
            double minY = Math.min(Math.floor(bb.minY), Math.ceil(bb.maxY));
            double minZ = Math.min(Math.floor(bb.minZ), Math.ceil(bb.maxZ));
            double maxX = Math.max(Math.floor(bb.minX), Math.ceil(bb.maxX));
            double maxY = Math.max(Math.floor(bb.minY), Math.ceil(bb.maxY));
            double maxZ = Math.max(Math.floor(bb.minZ), Math.ceil(bb.maxZ));

            for(double x = minX; x <= maxX; x++) {

                for(double y = minY; y <= maxY; y++) {

                    for(double z = minZ; z <= maxZ; z++) {

                        for(AxisAlignedBB bb1 : getHitboxes((int)x,(int)y,(int)z)) {

                            if(bb1.c(bb)) { return false; }

                        }

                    }

                }

            }
            return true;

        } else {

            double minX = Math.min(Math.floor(bb.minX), Math.ceil(bb.maxX));
            double minY = Math.min(Math.floor(bb.minY), Math.ceil(bb.maxY));
            double minZ = Math.min(Math.floor(bb.minZ), Math.ceil(bb.maxZ));
            double maxX = Math.max(Math.floor(bb.minX), Math.ceil(bb.maxX));
            double maxY = Math.max(Math.floor(bb.minY), Math.ceil(bb.maxY));
            double maxZ = Math.max(Math.floor(bb.minZ), Math.ceil(bb.maxZ));

            for (double x = minX; x <= maxX; x++) {

                for (double y = minY; y <= maxY; y++) {

                    for (double z = minZ; z <= maxZ; z++) {

                        if (squidForm) {

                            if (isPassable((int)x,(int)y,(int)z)) {
                                continue;
                            }

                        }
                        for (AxisAlignedBB bb1 : getHitboxes((int) x, (int) y, (int) z)) {

                            if (bb1.c(bb)) {

                                return false;

                            }

                        }

                    }

                }

            }

            return true;

        }

    }

    public double getHeightAt(int x, int y, int z) {

        double highestY = 0d;
        for(AxisAlignedBB bb : getHitboxes(x,y,z)) {

            double minY = bb.minY;
            double maxY = bb.maxY;
            if(minY == 0 && maxY == 0) { return 0d; } else {

                minY-=y;
                maxY-=y;
                if(minY > highestY) { highestY = minY; }
                if(maxY > highestY) { highestY = maxY; }

            }

        }
        return highestY;

    }

    public AxisAlignedBB[] getHitboxes(int x, int y, int z) {

        AxisAlignedBB[] bb = hitboxCache.get(x,y,z);
        if(bb != null) { return bb; } else {

            World world = ((CraftWorld)pathfinder.getWorld()).getHandle();
            IBlockData data = world.getType(new BlockPosition(x,y,z));
            if(data != null) {

                CraftBlockData data1 = CraftBlockData.createData(data);
                Material material = data1.getMaterial();
                if(AABBUtil.isPassable(material)) {

                    passableMaterials.set(true, x, y, z);

                }

                VoxelShape shape = data.getCollisionShape(world, new BlockPosition(x,y,z));
                if(shape != null && !shape.isEmpty()) {

                    List list = shape.d();
                    AxisAlignedBB[] boundingBoxes = new AxisAlignedBB[list.size()];
                    for(int i = 0; i < list.size(); i++) {

                        AxisAlignedBB bb1 = ((AxisAlignedBB)list.get(i));
                        AxisAlignedBB bb2 = new AxisAlignedBB(x + bb1.minX, y + bb1.minY, z + bb1.minZ, x + bb1.maxX, y + bb1.maxY, z + bb1.maxZ);
                        boundingBoxes[i] = bb2;

                    }
                    hitboxCache.set(boundingBoxes, x, y, z);
                    return boundingBoxes;

                } else {

                    hitboxCache.set(new AxisAlignedBB[]{new AxisAlignedBB(0,0,0,0,0,0)}, x, y, z);
                    return new AxisAlignedBB[]{new AxisAlignedBB(0,0,0,0,0,0)};

                }

            } else {

                hitboxCache.set(new AxisAlignedBB[]{new AxisAlignedBB(0,0,0,0,0,0)}, x, y, z);
                return new AxisAlignedBB[]{new AxisAlignedBB(0,0,0,0,0,0)};

            }

        }

    }

    public Node getNode(int x, int y, int z) {

        Node node = nodes.get(x,y,z);
        if(node != null) { return node; } else {

            Node node1 = new Node(x,y,z);
            nodes.set(node1, x, y, z);
            node1.createData(this);
            node1.calculateCosts(pathfinder);
            return node1;

        }

    }

}
