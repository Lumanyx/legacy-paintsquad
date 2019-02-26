package de.xenyria.splatoon.tutorial;

import de.xenyria.schematics.internal.XenyriaSchematic;
import de.xenyria.schematics.internal.access.XenyriaSchematicWorldAccess;
import de.xenyria.schematics.internal.change.BlockChange;
import de.xenyria.schematics.internal.history.BlockChangeHistory;
import de.xenyria.schematics.internal.history.SchematicBlockAccess;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.arena.placeholder.ArenaPlaceholder;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TutorialManager {

    private XenyriaSchematic schematic;

    private static boolean enabled = true;
    public static boolean isEnabled() { return enabled; }

    public TutorialManager() throws Exception {

        if(enabled) {

            //schematic = XenyriaSchematic.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "sp_tutorial.xsc"));

            Bukkit.unloadWorld("sp_tutorial", false);

            // Alle Dateien aus der Tutorialwelt löschen
            File folder = new File(System.getProperty("user.dir") + File.separator + "sp_tutorial");
            FileUtils.deleteDirectory(folder);

            World world = Bukkit.createWorld(new WorldCreator("sp_tutorial").type(WorldType.FLAT).generator(new ChunkGenerator() {
                @Override
                public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                    CraftChunkData data = new CraftChunkData(world);
                    return data;
                }
            }));
            SplatoonServer.applyGameRules(world);
            //world.setAutoSave(false);

            XenyriaSplatoon.getXenyriaLogger().log("TutorialManager bereit!");
            createClusters();

        } else {

            XenyriaSplatoon.getXenyriaLogger().warn("TutorialManager wird nicht initialisiert - Nicht aktiv.");

        }

    }

    public World getWorld() { return Bukkit.getWorld("sp_tutorial"); }
    private ArrayList<TutorialMatch> matches = new ArrayList<>();

    public void createClusters() {

        ArrayList<ChunkCoordIntPair> pairs = new ArrayList<>();
        int x = 0;
        for(int i = 0; i < 1; i++) {

            ArrayList<Color> colors = Color.getRandomColors(2);
            Color primary = colors.get(0);
            Color secondary = colors.get(1);

            ChunkCoordIntPair pair = new ChunkCoordIntPair(1, 1);

            ArrayList<ArenaPlaceholder> placeholders = new ArrayList<>();
            placeholders.add(new ArenaPlaceholder() { public Material getTriggeringMaterial() {
                return Material.ORANGE_TERRACOTTA; }
                public Material getReplacement() { return primary.getClay(); }
                public boolean handleFlagData() { return false; }
                public void addFlags(ArenaProvider.ArenaGenerationTask.FlagData flag) {}
            });
            placeholders.add(new ArenaPlaceholder() { public Material getTriggeringMaterial() {
                return Material.BLUE_TERRACOTTA; }
                public Material getReplacement() { return primary.getClay(); }
                public boolean handleFlagData() { return false; }
                public void addFlags(ArenaProvider.ArenaGenerationTask.FlagData flag) {}
            });
            placeholders.add(new ArenaPlaceholder() { public Material getTriggeringMaterial() {
                return Material.ORANGE_WOOL; }
                public Material getReplacement() { return primary.getWool(); }
                public boolean handleFlagData() { return true; }
                public void addFlags(ArenaProvider.ArenaGenerationTask.FlagData flag) {

                flag.addFlag(BlockFlagManager.PAINTABLE); flag.setTeamID((byte)0);

            }
            });
            placeholders.add(new ArenaPlaceholder() { public Material getTriggeringMaterial() {

                return Material.BLUE_WOOL; }
                public Material getReplacement() { return secondary.getWool(); }
                public boolean handleFlagData() { return true; }
                public void addFlags(ArenaProvider.ArenaGenerationTask.FlagData flag) {

                flag.addFlag(BlockFlagManager.PAINTABLE); flag.setTeamID((byte)1);

            }
            });


            ArenaProvider.ArenaGenerationTask task = new ArenaProvider.ArenaGenerationTask(getWorld(), "tutorial_map.xsc", new Vector(i*300, 64, 0), null, null, placeholders);
            long begin = System.nanoTime();
            task.work();
            long end = System.nanoTime();

            XenyriaSplatoon.getXenyriaLogger().log("Cluster #" + i + " generiert. (Dauerte " + ((end - begin) / 1000000f) + " ms)");
            TutorialMatch match = new TutorialMatch(getWorld(), new Vector(i*300,64,0), task.getSchematic().getStoredPlaceholders());
            match.handleFlags(task.getFlagData());

            Team team = new Team(0, primary);
            Team enemy = new Team(1, secondary);
            match.registerTeam(team);
            match.registerTeam(enemy);
            matches.add(match);

            try {

                Vector offset = new Vector(i * 300, 64, 0);
                ArenaBoundaryConfiguration configuration = ArenaBoundaryConfiguration.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + "tutorial_map.sbounds"));
                for(ArenaBoundaryConfiguration.ArenaBoundaryBlock block : configuration.getPaintableSurfaces()) {

                    Vector realPos = offset.clone().add(new Vector(block.x, block.y, block.z));
                    BlockFlagManager.BlockFlag flag = match.getBlockFlagManager().getBlock(offset, realPos.getBlockX(), realPos.getBlockY(), realPos.getBlockZ());
                    flag.setPaintable(true);
                    flag.setWall(block.wall);

                }

            } catch (Exception e) {

                e.printStackTrace();

            }

            x+=300;

        }

        for(ChunkCoordIntPair pair : pairs) {

            Chunk chunk = getWorld().getChunkAt(pair.x, pair.z);
            ((CraftWorld)getWorld()).getHandle().updateBrightness(EnumSkyBlock.SKY, new BlockPosition(0,0,0), ((CraftChunk)chunk).getHandle());

        }

    }

    public TutorialMatch getFreeCluster() {

        for(TutorialMatch match : matches) {

            if(match.getHumanPlayers().isEmpty()) {

                return match;

            }

        }
        return null;

    }

}
