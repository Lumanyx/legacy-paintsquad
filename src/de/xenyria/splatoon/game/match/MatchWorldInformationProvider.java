package de.xenyria.splatoon.game.match;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.core.array.TwoDimensionalHashMap;
import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;

import java.util.List;

public class MatchWorldInformationProvider {

    private TwoDimensionalHashMap<ThreeDimensionalArray<AxisAlignedBB>> cachedHitboxes = new TwoDimensionalHashMap<>();
    private TwoDimensionalHashMap<ThreeDimensionalArray<PassableLocation>> passableLocations = new TwoDimensionalHashMap<>();

    private class PassableLocation {

        public final int x,y,z;
        public final Material material;
        public PassableLocation(int x, int y, int z, Material material) {

            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;

        }

    }

    public World nmsWorld() { return match.nmsWorld(); }

    public final AxisAlignedBB[] EMPTY = new AxisAlignedBB[]{};

    public AxisAlignedBB[] getBoundingBoxes(int x, int y, int z, boolean passableCheck) {

        final int cX = x >> 4, cZ = z >> 4;

        World world = nmsWorld();
        if(passableCheck) {

            ThreeDimensionalArray array = passableLocations.get(cX,cZ);
            if(array != null && array.exists(x,y,z)) {

                return EMPTY;

            } else {

                IBlockData data = world.getType(new BlockPosition(x,y,z));
                Material material = CraftBlockData.fromData(data).getMaterial();
                if(AABBUtil.isPassable(material)) {

                    if(array != null) {

                        array.set(new PassableLocation(x,y,z,material), x,y,z);

                    } else {

                        array = new ThreeDimensionalArray<PassableLocation>();
                        array.set(new PassableLocation(x,y,z,material), x,y,z);
                        passableLocations.set(cX, cZ, array);

                    }

                    return EMPTY;

                } else {

                    return getBoundingBoxes(x,y,z, false);

                }

            }

        } else {

            ThreeDimensionalArray array = cachedHitboxes.get(cX, cZ);
            if(array != null) {

                AxisAlignedBB[] aabbs = (AxisAlignedBB[]) array.get(x,y,z);
                if(aabbs != null) {

                    return aabbs;

                } else {

                    aabbs = positionToHitboxes(x,y,z);
                    array.set(aabbs, x, y, z);
                    return aabbs;

                }

            } else {

                array = new ThreeDimensionalArray();
                AxisAlignedBB[] aabbs = positionToHitboxes(x,y,z);
                array.set(aabbs, x,y,z);
                cachedHitboxes.set(cX, cZ, array);
                return aabbs;

            }

        }

    }

    public AxisAlignedBB[] positionToHitboxes(int x, int y, int z) {

        AxisAlignedBB[] aabbs = EMPTY;
        World world = nmsWorld();

        IBlockData data = world.getType(new BlockPosition(x,y,z));
        VoxelShape shape = data.getCollisionShape(world, new BlockPosition(x,y,z));
        aabbs = EMPTY;
        if(!shape.isEmpty()) {

            List list = shape.d();
            aabbs = new AxisAlignedBB[list.size()];
            int i = 0;
            for(Object o : list) {

                AxisAlignedBB aabb = (AxisAlignedBB)o;
                AxisAlignedBB absoluteAABB = new AxisAlignedBB(
                        x+aabb.minX, y+aabb.minY, z+aabb.minZ,
                        x+aabb.maxX, y+aabb.maxY, z+aabb.maxZ
                );
                aabbs[i] = absoluteAABB;
                i++;

            }

        }
        return aabbs;

    }

    private Match match;
    public MatchWorldInformationProvider(Match match) {

        this.match = match;

    }

}
