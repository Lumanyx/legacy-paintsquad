package de.xenyria.splatoon.game.map;

import de.xenyria.splatoon.commands.AIDebugCommand;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;

public class Map {

    public Map() {

    }

    private Boundaries boundaries = new Boundaries();
    public Boundaries getBoundaries() { return boundaries; }

    private ArrayList<TeamSpawn> spawns = new ArrayList<>();
    public ArrayList<TeamSpawn> getSpawns() { return spawns; }

    private Vector mapOffset = new Vector(0,0,0);
    public Vector getMapOffset() { return mapOffset; }

    public void pasteSpawn(World world, TeamSpawn spawn, Team team) {

        Location location = spawn.getPosition();
        for(de.xenyria.splatoon.game.map.Map.TeamSpawn.PositionWithMaterial material : de.xenyria.splatoon.game.map.Map.TeamSpawn.getSpawnSchematic()) {

            Vector playerPos = new Vector(
                    (int)mapOffset.getX() + location.getX(),
                    (int)mapOffset.getY() + location.getY(),
                    (int)mapOffset.getZ() + location.getZ());

            playerPos = playerPos.add(material.relativePosition);
            Material toUse = material.material;

            if(toUse == Material.WHITE_WOOL) {

                toUse = team.getColor().getWool();

            }

            world.getBlockAt(playerPos.getBlockX(), playerPos.getBlockY(),
                    playerPos.getBlockZ()).setType(toUse);

        }

    }

    private IntroductionCamera introductionCamera = new IntroductionCamera();
    public IntroductionCamera getIntroductionCamera() { return introductionCamera; }

    public static class IntroductionCamera {

        public static final int INTRODUCTION_TICKS = 360;

        private Vector start = new Vector(-35, 119, 179);
        public Vector getStart() { return start; }
        public Vector getEnd() { return end; }
        public Vector getFocus() { return focus; }

        private Vector end = new Vector(-56, 121, 176);
        private Vector focus = new Vector(-86, 108, 197);

        public void setFocus(Vector vector) { this.focus = vector; }
        public void setStart(Vector vector) { this.start = vector; }
        public void setEnd(Vector vector) { this.end = vector; }
    }

    public static class TeamSpawn {

        public static class PositionWithMaterial {

            public Vector relativePosition;
            public Material material;
            public PositionWithMaterial(Vector vector, Material material) {

                this.relativePosition = vector;
                this.material = material;

            }

        }

        private static ArrayList<PositionWithMaterial> spawnSchematic = new ArrayList<>();
        public static ArrayList<PositionWithMaterial> getSpawnSchematic() { return spawnSchematic; }

        private static HashMap<BlockFace, Vector[]> spawnPositionOffsets = new HashMap<>();
        public static Vector[] getSpawnPositionOffsets(float yaw) {

            yaw%=360;

            BlockFace faceToUse;
            if(yaw == 0f) {

                faceToUse = BlockFace.SOUTH;

            } else if(yaw == 90f) {

                faceToUse = BlockFace.WEST;

            } else if(yaw == 180f) {

                faceToUse = BlockFace.NORTH;

            } else {

                faceToUse = BlockFace.EAST;

            }
            return spawnPositionOffsets.get(faceToUse);

        }

        static {

            spawnPositionOffsets.put(BlockFace.WEST, new Vector[]{
                    new Vector(3, 1, 1),
                    new Vector(4, 1, 2),
                    new Vector(4, 1, 3),
                    new Vector(3, 1, 4)
            });
            spawnPositionOffsets.put(BlockFace.EAST, new Vector[]{
                    new Vector(2, 1, 1),
                    new Vector(1, 1, 2),
                    new Vector(1, 1, 3),
                    new Vector(2, 1, 4)
            });
            spawnPositionOffsets.put(BlockFace.NORTH, new Vector[]{
                    new Vector(1, 1, 3),
                    new Vector(2, 1, 4),
                    new Vector(3, 1, 4),
                    new Vector(4, 1, 3)
            });
            spawnPositionOffsets.put(BlockFace.SOUTH, new Vector[]{
                    new Vector(1, 1, 2),
                    new Vector(2, 1, 1),
                    new Vector(3, 1, 1),
                    new Vector(4, 1, 2)
            });

            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 1), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 1), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 2), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 2), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 3), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 3), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 4), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 4), Material.WHITE_WOOL));

            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 0, 3), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 0, 2), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 0, 3), Material.WHITE_WOOL));
            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 0, 2), Material.WHITE_WOOL));

            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 0, 1), Material.SMOOTH_STONE));
            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 0, 1), Material.SMOOTH_STONE));
            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 0, 4), Material.SMOOTH_STONE));
            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 0, 4), Material.SMOOTH_STONE));

            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 1, 1), Material.REDSTONE_TORCH));
            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 1, 1), Material.REDSTONE_TORCH));
            spawnSchematic.add(new PositionWithMaterial(new Vector(4, 1, 4), Material.REDSTONE_TORCH));
            spawnSchematic.add(new PositionWithMaterial(new Vector(1, 1, 4), Material.REDSTONE_TORCH));

            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 0), Material.STONE_SLAB));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 0), Material.STONE_SLAB));

            spawnSchematic.add(new PositionWithMaterial(new Vector(0, 0, 2), Material.STONE_SLAB));
            spawnSchematic.add(new PositionWithMaterial(new Vector(0, 0, 3), Material.STONE_SLAB));

            spawnSchematic.add(new PositionWithMaterial(new Vector(5, 0, 2), Material.STONE_SLAB));
            spawnSchematic.add(new PositionWithMaterial(new Vector(5, 0, 3), Material.STONE_SLAB));

            spawnSchematic.add(new PositionWithMaterial(new Vector(2, 0, 5), Material.STONE_SLAB));
            spawnSchematic.add(new PositionWithMaterial(new Vector(3, 0, 5), Material.STONE_SLAB));

        }

        private Location position;
        public Location getPosition() { return position; }

        private float direction;
        public float getDirection() { return direction; }

        public Location introVector(World world) {

            Location location = new Location(world, 0,0,0);
            location.setYaw(direction - 22.5f);

            Vector dirVec = location.getDirection().clone();
            Vector realPos = position.toVector().add(new Vector(3, 2.25, 3));

            Vector target = realPos.clone().add(dirVec.clone().multiply(5));

            Location location1 = new Location(world, target.getX(), target.getY(), target.getZ());
            location1.setDirection(realPos.clone().subtract(target));
            location1 = location1.add(0, -2.25, 0);

            location1.setPitch(-8.5f);

            return location1;

        }

        private int teamID;
        public int getTeamID() { return teamID; }

        public TeamSpawn(int teamID, float direction, Location position) {

            this.teamID = teamID;
            this.position = position;
            this.direction = direction;

        }

    }

    public class Boundaries {

        private Vector min = new Vector(6, 65, -4), max = new Vector(-52, 74, 56);
        public Vector getMin() { return min; }
        public Vector getMax() { return max; }

    }

}
