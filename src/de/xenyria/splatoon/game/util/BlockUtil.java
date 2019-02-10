package de.xenyria.splatoon.game.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockUtil {

    public static Block ground(Location location, int maxDepth) {

        int y = 0;
        Location cursor = location.clone();
        while (y < maxDepth && cursor.getY() > 0) {

            cursor = cursor.add(0, -1, 0);
            if(cursor.getBlock().getType() != Material.AIR) {

                return cursor.getBlock();

            }
            y++;

        }
        return cursor.getBlock();

    }

}
