package de.xenyria.splatoon.arena;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.schematics.internal.BlockCoordinate;
import de.xenyria.schematics.internal.XenyriaSchematic;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.servercore.spigot.util.WorldUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.builder.ArenaBuilder;
import de.xenyria.splatoon.arena.placeholder.ArenaPlaceholder;
import de.xenyria.splatoon.arena.placeholder.StoredTeamPlaceholder;
import de.xenyria.splatoon.arena.schematic.SchematicProvider;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import net.minecraft.server.v1_13_R2.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.generator.CraftChunkData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ArenaProvider {

    private World arenaWorld;
    public World getArenaWorld() { return arenaWorld; }

    public ArenaProvider() {

        // Alle vorherigen Dateien zur Welt löschen
        try {

            File folder = new File(System.getProperty("user.dir") + File.separator + "sp_arena");
            FileUtils.deleteDirectory(folder);

        } catch (Exception e) {

            XenyriaSplatoon.getXenyriaLogger().error("Arena-Welt konnte nicht gelöscht werden!", e);

        }

        arenaWorld = Bukkit.createWorld(new WorldCreator("sp_arena").generator(new ChunkGenerator() {

            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {

                CraftChunkData data = new CraftChunkData(world);
                return data;

            }

        }));
        SplatoonServer.applyGameRules(arenaWorld);
        arenaWorld.setAutoSave(false);

    }


    private Vector lastOffset = new Vector(0,64,0);

    public ArenaGenerationTask requestArena(int id, MatchType type) {

        ArenaData data = XenyriaSplatoon.getArenaRegistry().getArenaData(id);
        ArrayList<ArenaPlaceholder> placeholders = new ArrayList<>();

        ArenaGenerationTask task = new ArenaGenerationTask(arenaWorld, data.getMap().get(type) + ".xsc", lastOffset, null, XenyriaSplatoon.getArenaRegistry().getArenaData(id), placeholders);
        lastOffset = lastOffset.clone().add(new Vector(1000, 0, 0));
        XenyriaSplatoon.getArenaBuilder().queueTask(task);
        return task;

    }

    public ArenaGenerationTask requestArena(int id, Match match) {

        ArenaData data = XenyriaSplatoon.getArenaRegistry().getArenaData(id);
        ArrayList<ArenaPlaceholder> placeholders = new ArrayList<>();
        for(StoredTeamPlaceholder placeholder : data.getPlaceholders()) { placeholders.add(placeholder.toPlaceholder(match)); }

        ArenaGenerationTask task = new ArenaGenerationTask(arenaWorld, data.getMap().get(match.getMatchType()) + ".xsc", lastOffset, match, XenyriaSplatoon.getArenaRegistry().getArenaData(id), placeholders);
        lastOffset = lastOffset.clone().add(new Vector(1000, 0, 0));
        XenyriaSplatoon.getArenaBuilder().queueTask(task);
        return task;

    }

    public static class ArenaGenerationTask {

        private boolean done;
        public boolean isDone() { return done; }

        private float progress;
        public float getProgress() { return progress; }

        private Vector offset;
        public Vector getOffset() { return offset; }

        private ArrayList<ArenaPlaceholder> placeholders;
        private Match match;

        private World world;
        public ArenaGenerationTask(World world, String schematic, Vector offset, Match match, ArenaData data, ArrayList<ArenaPlaceholder> placeholders) {

            this.world = world;
            this.match = match;
            this.schematicName = schematic;
            this.offset = offset;
            this.data = data;
            this.placeholders = placeholders;

        }

        private String schematicName;

        private boolean loaded = false;
        private XenyriaSchematic schematic;
        public XenyriaSchematic getSchematic() { return schematic; }

        private boolean successful = false;
        private int placedBlocks = 0;
        private ArrayList<ThreeDimensionalArray.ThreeDimensionalArrayItem<BlockCoordinate>> coordinates;
        private HashMap<ChunkCoordIntPair, Chunk> chunkReferences = new HashMap<>();
        private ArrayList<BlockCoordinate> syncCoords = new ArrayList<>();
        private HashMap<BlockCoordinate, ArenaPlaceholder.Metadata> metadata = new HashMap<>();
        private ArrayList<FlagData> flagData = new ArrayList<>();
        public ArrayList<FlagData> getFlagData() { return flagData; }

        private int minX,minY,minZ,maxX,maxY,maxZ;
        private long calculateBegin = 0;
        public float elapsedTime() {

            return (System.nanoTime() - calculateBegin) / 1000000f;

        }

        public static class FlagData {

            private int x,y,z;
            public int getX() { return x; }
            public int getY() { return y; }
            public int getZ() { return z; }

            public FlagData(int x, int y, int z) {

                this.x = x;
                this.y = y;
                this.z = z;

            }

            private byte teamID = -1;
            public byte getTeamID() { return teamID; }
            public void setTeamID(byte id) { this.teamID = id; }

            private ArrayList<Byte> enabledFlags = new ArrayList<>();
            public void addFlag(byte v) { enabledFlags.add(v); }

            public ArrayList<Byte> getFlags() { return enabledFlags; }
        }

        public void work() {

            if(!exec) {

                if (calculateBegin == 0) { calculateBegin = System.nanoTime(); }
                if (!loaded) {

                    try {

                        schematic = SchematicProvider.loadSchematic(schematicName);
                        coordinates = schematic.getBlockCoordinates().getItems();
                        loaded = true;

                        Vector vec1 = null;
                        Vector vec2 = null;
                        for(StoredPlaceholder placeholder : schematic.getStoredPlaceholders()) {

                            if(placeholder.type == SchematicPlaceholder.Splatoon.MAP_BOUND) {

                                if (vec1 == null) {

                                    vec1 = new Vector(placeholder.x, placeholder.y, placeholder.z);
                                    vec1 = vec1.add(offset);

                                } else if(vec2 == null) {

                                    vec2 = new Vector(placeholder.x, placeholder.y, placeholder.z);
                                    vec2 = vec2.add(offset);

                                }

                            }

                        }

                        if(vec1 != null && vec2 != null) {

                            min = new Vector(
                                    Math.min(vec1.getX(), vec2.getX()),
                                    Math.min(vec1.getY(), vec2.getY()),
                                    Math.min(vec1.getZ(), vec2.getZ())
                            );
                            max = new Vector(
                                    Math.max(vec1.getX(), vec2.getX()),
                                    Math.max(vec1.getY(), vec2.getY()),
                                    Math.max(vec1.getZ(), vec2.getZ())
                            );

                        } else {

                            XenyriaSplatoon.getXenyriaLogger().warn("Es fehlt ein/zwei Mapbound-Punkt(e) in der §bSchematic " + schematicName);

                        }

                    } catch (Exception e) {

                        e.printStackTrace();
                        done = true;
                        successful = false;
                        return;

                    }

                }
                int placedInIter = 0;
                int targetIndex = 0;
                final net.minecraft.server.v1_13_R2.World world1 = ((CraftWorld) world).getHandle();

                for (ThreeDimensionalArray.ThreeDimensionalArrayItem<BlockCoordinate> item : coordinates) {

                    BlockCoordinate coordinate = item.getT();

                    int realX = (int) offset.getX() + coordinate.x;
                    int realY = (int) offset.getY() + coordinate.y;
                    int realZ = (int) offset.getZ() + coordinate.z;

                    int cX = realX >> 4;
                    int cZ = realZ >> 4;

                    ChunkCoordIntPair pair = new ChunkCoordIntPair(cX, cZ);
                    if (!chunkReferences.containsKey(pair)) {

                        Chunk chunk = null;
                        if(world.equals(XenyriaSplatoon.getArenaProvider().arenaWorld)) {

                            chunk = ((CraftChunk) ArenaBuilder.forceChunkLoaded(pair)).getHandle();

                        } else {

                            chunk = ((CraftChunk)world.getChunkAt(pair.x, pair.z)).getHandle();

                        }

                        chunkReferences.put(pair, chunk);

                    }

                    Chunk chunk = chunkReferences.get(pair);

                    ChunkSection section = chunk.getSections()[realY >> 4];
                    IBlockData data = item.getT().data.getState();
                    for (ArenaPlaceholder placeholder : placeholders) {

                        if (placeholder.getTriggeringMaterial().equals(item.getT().material)) {

                            data = CraftBlockData.newData(placeholder.getReplacement(), "").getState();
                            if(placeholder.handleFlagData()) {

                                FlagData data1 = new FlagData(realX, realY,realZ);
                                placeholder.addFlags(data1);
                                flagData.add(data1);

                            }

                        }

                    }

                    if (data.getBlock() instanceof ITileEntity) {

                        syncCoords.add(item.getT());
                        placedInIter++;

                    } else {

                        if (section == null) {

                            section = new ChunkSection(realY >> 4 << 4, world1.worldProvider.g(), chunk, world1, true);
                            chunk.getSections()[realY >> 4] = section;
                            for (int x = 0; x < 16; x++) {

                                for (int y = 0; y < 16; y++) {

                                    for (int z = 0; z < 16; z++) {

                                        section.getSkyLightArray().a(x, y, z, 15);
                                        section.getEmittedLightArray().a(x, y, z, 0);

                                    }

                                }

                            }

                            section.setType(realX & 15, realY & 15, realZ & 15, data);

                        } else {

                            section.setType(realX & 15, realY & 15, realZ & 15, data);

                        }

                        section.getSkyLightArray().a(realX & 15, realY & 15, realZ & 15, coordinate.skyLight);
                        section.getEmittedLightArray().a(realX & 15, realY & 15, realZ & 15, coordinate.emitLight);
                        placedInIter++;
                        progress = ((float) placedBlocks / (float) coordinates.size()) * 100;

                    }


                }
                exec = true;
                Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                    for (BlockCoordinate coordinate : syncCoords) {

                        int realX = (int) offset.getX() + coordinate.x;
                        int realY = (int) offset.getY() + coordinate.y;
                        int realZ = (int) offset.getZ() + coordinate.z;
                        Chunk chunk = chunkReferences.get(new ChunkCoordIntPair(realX >> 4, realZ >> 4));
                        chunk.setType(new BlockPosition(realX, realY, realZ), coordinate.data.getState(), true);
                        chunk.getSections()[realY >> 4].getSkyLightArray().a(realX & 15, realY & 15, realZ & 15, coordinate.skyLight);
                        chunk.getSections()[realY >> 4].getEmittedLightArray().a(realX & 15, realY & 15, realZ & 15, coordinate.emitLight);

                    }
                    for(Map.Entry<BlockCoordinate, ArenaPlaceholder.Metadata> metadata : metadata.entrySet()) {

                        BlockCoordinate coordinate = metadata.getKey();
                        Block block = world.getBlockAt(coordinate.x, coordinate.y, coordinate.z);
                        block.setMetadata(metadata.getValue().key, metadata.getValue().value);

                    }

                    if(match != null) {

                        match.updateBounds(new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ));

                    }
                    done = true;
                    successful = true;

                });

                placedBlocks += placedInIter;

            }

        }
        private boolean exec = false;

        public boolean isSuccessful() { return successful; }

        private Vector min,max;
        public Vector getMin() {
            return min;
        }
        public Vector getMax() {
            return max;
        }

        private ArenaData data;
        public ArenaData getArenaData() { return data; }

    }

}
