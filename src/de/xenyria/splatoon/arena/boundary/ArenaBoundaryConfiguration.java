package de.xenyria.splatoon.arena.boundary;

import de.xenyria.io.reader.ByteArrayReader;
import de.xenyria.io.reader.ByteArrayWriter;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.arena.MapBoundBuilder;
import de.xenyria.splatoon.game.util.AABBUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ArenaBoundaryConfiguration {

    public ArenaBoundaryBlock[] getPaintableSurfaces() { return paintableSurfaces; }

    public static class ArenaBoundaryBlock {

        public final int x;
        public final int y;
        public final int z;
        public final boolean wall;
        public ArenaBoundaryBlock(int x, int y, int z, boolean wall) {

            this.x = x;
            this.y = y;
            this.z = z;
            this.wall = wall;

        }

    }

    public static ArenaBoundaryConfiguration fromMapBounds(MapBoundBuilder builder, ArenaData data) {

        ArenaBoundaryConfiguration configuration = new ArenaBoundaryConfiguration();
        ArrayList<ArenaBoundaryBlock> blocks = new ArrayList<>();
        for(MapBoundBuilder.BlockPos pos : builder.getPaintableSurfaces()) {

            Location location = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(), pos.x, pos.y, pos.z);
            location = location.add(builder.getOffset());
            Block block = location.getBlock();

            boolean paintable = data.isPaintable(block.getType());
            if(paintable) {

                boolean below = configuration.isBlocked(block, BlockFace.DOWN);
                boolean above = configuration.isBlocked(block, BlockFace.UP);

                if (above || below) {

                    if (above && below) {

                        blocks.add(new ArenaBoundaryBlock(pos.x, pos.y, pos.z, true));
                        continue;

                    }

                    /*
                    if ((
                            (below && !builder.doesNodeExist(block.getX(), block.getY() - 1, block.getZ())) ||
                                    (above && !builder.doesNodeExist(block.getX(), block.getY() + 1, block.getZ())))) {

                        // Untere Seite blockiert, allerdings gehört der untere Knoten nicht zum Schlachtfeld
                        blocks.add(new ArenaBoundaryBlock(pos.x, pos.y, pos.z, true));
                        continue;

                    }

                    int blockCounter = 0;
                    for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {

                        if(configuration.isBlocked(block, face)) {

                            blockCounter++;

                        }

                    }

                    if(blockCounter <= 3) {

                        blocks.add(new ArenaBoundaryBlock(pos.x, pos.y, pos.z, true));

                    } else {

                        blocks.add(new ArenaBoundaryBlock(pos.x, pos.y, pos.z, false));

                    }*/

                }

                blocks.add(new ArenaBoundaryBlock(pos.x, pos.y, pos.z, false));

            }

        }
        configuration.paintableSurfaces = blocks.toArray(new ArenaBoundaryBlock[]{});
        return configuration;

    }

    public boolean isBlocked(Block block, BlockFace face) {

        Block rel = block.getRelative(face);
        if(!rel.isEmpty() && !AABBUtil.isPassable(rel.getType())) {

            return true;

        }
        return false;

    }

    public static ArenaBoundaryConfiguration fromFile(File file) throws Exception {

        FileInputStream stream = new FileInputStream(file);
        byte[] data = new byte[stream.available()];
        stream.read(data);
        stream.close();
        ByteArrayReader reader = new ByteArrayReader(data);
        int entryCount = reader.readInt();
        ArenaBoundaryBlock[] configurations = new ArenaBoundaryBlock[entryCount];
        for(int i = 0; i < entryCount; i++) {

            configurations[i] = new ArenaBoundaryBlock(
                    reader.readInt(), reader.readInt(), reader.readInt(), reader.readBoolean()
            );

        }
        ArenaBoundaryConfiguration configuration = new ArenaBoundaryConfiguration();
        configuration.paintableSurfaces = configurations;
        return configuration;

    }

    public void save(File file) throws Exception {

        ByteArrayWriter writer = new ByteArrayWriter(4);
        writer.writeInt(paintableSurfaces.length);
        for(ArenaBoundaryBlock block : paintableSurfaces) {

            writer.writeInt(block.x);
            writer.writeInt(block.y);
            writer.writeInt(block.z);
            writer.writeBoolean(block.wall);

        }
        byte[] bytes = writer.bytes();
        FileOutputStream stream = new FileOutputStream(file);
        stream.write(bytes);
        stream.close();

    }

    private ArenaBoundaryBlock[] paintableSurfaces;

}
