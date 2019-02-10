package de.xenyria.splatoon.ai.pathfinding.grid;

import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.VoxelShape;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;

public class NodeBlockData {

    private IBlockData data;
    private double height;

    public NodeBlockData(World world, Node node, NodeGrid grid) {

        data = ((CraftWorld)world).getHandle().getType(new BlockPosition(node.x, node.y, node.z));
        height = grid.getHeightAt(node.x, node.y, node.z);

    }

    public double getHeight() { return height; }

    public Material material() {

        return CraftBlockData.fromData(data).getMaterial();

    }

    public double getSquidHeight() {

        if(AABBUtil.isPassable(CraftBlockData.fromData(data).getMaterial())) {

            return 0d;

        } else {

            return height;

        }

    }

}
