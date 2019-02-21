package de.xenyria.splatoon;

import de.xenyria.splatoon.game.color.Color;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SplatoonServer {

    public static void applyGameRules(World world) {

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setFullTime(6000);

        world.setAmbientSpawnLimit(0);
        world.setAnimalSpawnLimit(0);
        world.setWaterAnimalSpawnLimit(0);
        world.setMonsterSpawnLimit(0);

        world.setTicksPerAnimalSpawns(0);
        world.setTicksPerMonsterSpawns(0);

        world.setStorm(false);
        world.setWeatherDuration(0);
        world.setThunderDuration(0);
        world.setPVP(false);
        world.setThundering(false);
        world.setDifficulty(Difficulty.PEACEFUL);

    }

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
