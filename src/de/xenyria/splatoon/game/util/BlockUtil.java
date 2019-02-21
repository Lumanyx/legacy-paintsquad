package de.xenyria.splatoon.game.util;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;

public class BlockUtil {

    public static Block ground(Location location, int maxDepth) {

        World world = ((CraftWorld)location.getWorld()).getHandle();
        Chunk chunk = world.getChunkIfLoaded((int)location.getX()>>4,(int)location.getZ()>>4);
        if(chunk != null) {

            int y = 0;
            int origY = location.getBlockY();
            BlockPosition position = new BlockPosition(location.getBlockX(),location.getBlockY(),location.getBlockZ());
            while (y < maxDepth && (origY-y) > 0) {

                IBlockData data = chunk.getType(position);
                if(data != null) {

                    Material material = CraftBlockData.fromData(data).getMaterial();
                    if (material != Material.AIR) {

                        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY()-y, location.getBlockZ()).getBlock();

                    }

                }

                y++;
                position = position.add(0, -1, 0);

            }
            return new Location(location.getWorld(), location.getBlockX(), location.getBlockY()-y, location.getBlockZ()).getBlock();

        } else {

            return location.getBlock();

        }

    }

}
