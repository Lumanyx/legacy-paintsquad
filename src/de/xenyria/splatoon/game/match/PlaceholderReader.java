package de.xenyria.splatoon.game.match;

import de.xenyria.schematics.internal.XenyriaSchematic;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.map.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PlaceholderReader {

    public static void main(String[] args) {

        ArrayList<Integer> ints = new ArrayList<>();

        ints.add(1);
        ints.add(0);
        Collections.sort(ints);
        for(int i : ints) {

            System.out.println(i);

        }
        System.out.println(Material.AIR.isEmpty());

    }

    public static ArrayList<Map.TeamSpawn> getSpawns(Vector offset, XenyriaSchematic schematic) {

        ArrayList<Integer> teamIDs = new ArrayList<>();
        HashMap<Integer, Location> teamToSpawnMap = new HashMap<>();
        HashMap<Integer, Float> teamDirectionMap = new HashMap<>();

        for(StoredPlaceholder placeholder : schematic.getStoredPlaceholders()) {

            if(placeholder.type == SchematicPlaceholder.Splatoon.SPAWN_POINT) {

                float yaw = Float.parseFloat(placeholder.getData().get("yaw"));
                int teamID = Integer.parseInt(placeholder.getData().get("teamid"));
                Location location = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                        offset.getBlockX() + placeholder.x, offset.getY() + placeholder.y, offset.getZ() + placeholder.z);
                location.setYaw(yaw);
                teamIDs.add(teamID);
                teamToSpawnMap.put(teamID, location);
                teamDirectionMap.put(teamID, yaw);

            }

        }

        Collections.sort(teamIDs);
        ArrayList<Map.TeamSpawn> spawns = new ArrayList<>();
        for(int i : teamIDs) {

            Map.TeamSpawn spawn = new Map.TeamSpawn(i, teamDirectionMap.get(i), teamToSpawnMap.get(i));
            spawns.add(spawn);

        }

        return spawns;

    }

}
