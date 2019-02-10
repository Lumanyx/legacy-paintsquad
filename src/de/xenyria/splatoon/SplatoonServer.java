package de.xenyria.splatoon;

import de.xenyria.splatoon.game.color.Color;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SplatoonServer {

    public static void broadcastColorParticle(World world, double x, double y, double z, Color color, float size) {

        broadcastColorParticle(world, x, y, z, 0f,0f, 0f, color, size);

    }


    public static void broadcastColorParticle(World world, double x, double y, double z, float v, float v1, float v2, Color color, float size) {

        final Particle.DustOptions options = new Particle.DustOptions(color.getBukkitColor(), size);
        for(Player player : Bukkit.getOnlinePlayers()) {

            if(player.getWorld() == world && player.getLocation().toVector().distance(new Vector(x,y,z)) < 64) {

                player.spawnParticle(Particle.REDSTONE, x, y, z, 0, v, v1, v2, options);

            }

        }


    }


    public static void broadcastColorizedBreakParticle(World world, double x, double y, double z, Color color) {

        broadcastColorizedBreakParticle(world, x,y,z,0,0,0, color);

    }
    public static void broadcastColorizedBreakParticle(World world, double x, double y, double z, double x1, double y1, double z1, Color color) {

        CraftBlockData data = CraftBlockData.fromData(CraftMagicNumbers.getBlock(color.getWool()).getBlockData());
        for(Player player : Bukkit.getOnlinePlayers()) {

            if(player.getWorld() == world && player.getLocation().toVector().distance(new Vector(x,y,z)) < 64) {

                player.spawnParticle(Particle.BLOCK_CRACK, x,y,z, 0, x1, y1, z1, data);

            }

        }

    }

    public static void broadcastColorParticleExplosion(World world, double x, double y, double z, Color color) {

        CraftBlockData data = CraftBlockData.fromData(CraftMagicNumbers.getBlock(color.getWool()).getBlockData());
        for(Player player : Bukkit.getOnlinePlayers()) {

            if(player.getWorld() == world) {

                player.spawnParticle(Particle.BLOCK_CRACK, x,y,z, 5, data);

            }

        }

    }


}
