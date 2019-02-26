package de.xenyria.splatoon.game.util;

import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class AABBUtil {

    public static double MAX_WRAP_HEIGHT = 1.1875d;
    public static Vector resolveWrap(World world, Vector position, AxisAlignedBB bb) {

        return resolveWrap(world, position, bb, true);

    }
    public static Vector resolveWrap(World world, Vector position, AxisAlignedBB bb, boolean allowPassableMaterial) {

        WorldServer server = ((CraftWorld)world).getHandle();
        BlockPosition position1 = getBlockingPos(world, bb);
        IBlockData data = server.getType(position1);
        if(!allowPassableMaterial) {

            if(AABBUtil.isPassable(CraftBlockData.createData(data).getMaterial())) {

                return null;

            }

        }

        VoxelShape shape = data.getCollisionShape(server, position1);
        if(!shape.isEmpty()) {

            double highestY = Math.max(shape.getBounds().minY, shape.getBounds().maxY);

            Vector origPos = position.clone();
            double heightPerStep = 0.0625;
            for (int i = 0; i <= (MAX_WRAP_HEIGHT / heightPerStep); i++) {

                double newY = (i+1) * heightPerStep;
                if(newY <= highestY) {

                    Vector currentPosition = origPos.clone();
                    currentPosition = currentPosition.add(new Vector(0, newY, 0));
                    AxisAlignedBB newBB = new AxisAlignedBB(bb.minX, bb.minY + newY, bb.minZ, bb.maxX, bb.maxY + newY, bb.maxZ);
                    if (hasSpace(world, newBB)) {

                        return currentPosition;

                    }

                }

            }

        }
        return null;

    }

    public static boolean hasSpace(World world, AxisAlignedBB bb) {

        return getBlockingPos(world, bb) == null;

    }

    public static BlockPosition getBlockingPos(World world, AxisAlignedBB bb) {

        int minX = (int) Math.min(Math.floor(bb.minX) - 1, Math.floor(bb.maxX) - 1);
        int minY = (int) Math.min(Math.floor(bb.minY) - 1, Math.floor(bb.maxY) - 1);
        int minZ = (int) Math.min(Math.floor(bb.minZ) - 1, Math.floor(bb.maxZ) - 1);
        int maxX = (int) Math.max(Math.ceil(bb.minX) + 1, Math.ceil(bb.maxX) + 1);
        int maxY = (int) Math.max(Math.ceil(bb.minY) + 1, Math.ceil(bb.maxY) + 1);
        int maxZ = (int) Math.max(Math.ceil(bb.minZ) + 1, Math.ceil(bb.maxZ) + 1);

        WorldServer server = ((CraftWorld)world).getHandle();

        for(int x = minX; x <= maxX; x++) {

            for(int y = minY; y <= maxY; y++) {

                for(int z = minZ; z <= maxZ; z++) {

                    BlockPosition position = new BlockPosition(x,y,z);
                    IBlockData data = server.getType(position);
                    VoxelShape shape = data.getCollisionShape(server, position);
                    if(!shape.isEmpty()) {

                        for(Object bb2 : shape.d()) {

                            AxisAlignedBB bb1 = (AxisAlignedBB) bb2;

                            double newMinBlockX = Math.min(x+bb1.minX, x+bb1.maxX);
                            double newMinBlockY = Math.min(y+bb1.minY, y+bb1.maxY);
                            double newMinBlockZ = Math.min(z+bb1.minZ, z+bb1.maxZ);
                            double newMaxBlockX = Math.max(x+bb1.minX, x+bb1.maxX);
                            double newMaxBlockY = Math.max(y+bb1.minY, y+bb1.maxY);
                            double newMaxBlockZ = Math.max(z+bb1.minZ, z+bb1.maxZ);

                            AxisAlignedBB blockBB = new AxisAlignedBB(newMinBlockX, newMinBlockY, newMinBlockZ, newMaxBlockX, newMaxBlockY, newMaxBlockZ);
                            if(blockBB.c(bb)) { return position; }

                        }

                    }

                }

            }

        }
        return null;

    }

    private static ArrayList<Material> passable = new ArrayList<>();

    static {

        passable.add(Material.IRON_BARS);
        passable.add(Material.IRON_TRAPDOOR);
        passable.add(Material.TRIPWIRE);
        for(Material material : Material.values()) {

            if(material.name().contains("CARPET")) {

                passable.add(material);

            }

        }

    }

    public static double getHeight(BlockPosition position, net.minecraft.server.v1_13_R2.World world) {

        IBlockData data = world.getTypeIfLoaded(position);
        VoxelShape shape = data.getCollisionShape(world, position);
        if(shape != null && !shape.isEmpty()) {

            double highestY = 0d;
            for(Object o : shape.d()) {

                AxisAlignedBB aabb = (AxisAlignedBB) o;
                if(aabb.maxY > highestY) {

                    highestY = aabb.maxY;

                }

            }
            return highestY;

        }
        return 0d;

    }

    public static boolean isPassable(Material type) {

        return passable.contains(type);

    }

}
