package de.xenyria.splatoon.game.match.blocks;

import de.xenyria.core.array.ConcurrentThreeDimensionalArray;
import de.xenyria.core.array.ConcurrentTwoDimensionalMap;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class BlockFlagManager {

    private Match match;
    public BlockFlagManager(Match match) {

        this.match = match;

    }

    private ConcurrentTwoDimensionalMap<BlockChunk> chunks = new ConcurrentTwoDimensionalMap<>();
    public BlockChunk getChunkIfExists(int x, int z) { return chunks.get(x,z); }
    public BlockFlag getBlockIfExist(int x, int y, int z) {

        BlockChunk chunk = chunks.get(x>>4,z>>4);
        if(chunk != null) {

            return chunk.getIfExists(x,y,z);

        }
        return null;

    }
    public BlockFlag getBlock(Vector offset, int x, int y, int z) {

        BlockChunk chunk = chunks.get(x>>4,z>>4);
        if(chunk == null) {

            chunk = new BlockChunk();
            chunks.set(x>>4,z>>4, chunk);

        }
        BlockFlag flag = chunk.getIfExists(x,y,z);
        if(flag == null) {

            flag = chunk.createBlock(offset, x,y,z);

        }
        return flag;

    }

    public void reset() {

        for(BlockChunk chunk : chunks.getItems()) {

            for(ConcurrentThreeDimensionalArray.ThreeDimensionalArrayItem<BlockFlag> flag : chunk.flags.getItems()) {

                BlockFlag flag1 = flag.getT();
                flag1.setTeamID(BlockFlag.NO_TEAM);
                flag1.setTrail(false);

            }

        }

    }

    public static class BlockChunk {

        private ConcurrentThreeDimensionalArray<BlockFlag> flags = new ConcurrentThreeDimensionalArray<>();
        public BlockFlag getIfExists(int x, int y, int z) {

            return flags.get(x&15, y, z&15);

        }
        public BlockFlag createBlock(Vector mapOffset, int absX, int absY, int absZ) {

            BlockFlag flag = new BlockFlag(
                    (short) ((int)mapOffset.getX()-absX),
                    (short) ((int)mapOffset.getY()-absY),
                    (short) ((int)mapOffset.getZ()-absZ));
            flags.set(flag, absX&15,absY,absZ&15);
            return flag;

        }

    }

    // Ähnliche Funktionsweise wie die Metadaten für Blöcke in der Bukkit API, nur effizienter
    private static final byte PAINTABLE = (byte)0;
    private static final byte WALL = (byte)1;
    private static final byte TRAIL = (byte)2;
    private static final byte FLAG_CNT = 3;

    public static class BlockFlag {

        public static final byte ON = (byte)1;
        public static final byte NO_TEAM = (byte)-1;

        public BlockFlag(short x, short y, short z) {

            this.x = x;
            this.y = y;
            this.z = z;

        }

        public Vector toPosition(Match match) {

            return match.getOffset().add(new Vector(x,y,z));

        }
        public Location toLocation(Match match) {

            return toPosition(match).toLocation(match.getWorld());

        }

        // Offsets
        public final short x,y,z;
        public byte teamID = NO_TEAM;
        private final byte[] flags = new byte[FLAG_CNT];
        public boolean isPaintable() { return flags[PAINTABLE] == (byte)1 || flags[WALL] == ON; }
        public boolean isOwnedByTeam(Team team) { return teamID == team.getID(); }
        public void setPaintable(boolean value) { flags[PAINTABLE] = value ? (byte)1 : (byte)0; }
        public void setWall(boolean value) { flags[WALL] = value ? (byte)1 : (byte)0; }
        public void setTrail(boolean value) { flags[TRAIL] = value ? (byte)1 : (byte)0; }
        public void setFlag(byte id, boolean value) {

            flags[id] = value ? (byte)1 : (byte)0;

        }
        public void setTeamID(byte id) { this.teamID = id; }

        public boolean isTrail() { return flags[TRAIL] == ON; }

        public boolean hasSetTeam() { return teamID != NO_TEAM; }

        public byte getTeamID() { return teamID; }

        public boolean isWall() { return flags[WALL] == ON; }
    }


}
