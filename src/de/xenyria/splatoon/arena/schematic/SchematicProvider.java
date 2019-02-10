package de.xenyria.splatoon.arena.schematic;

import de.xenyria.schematics.internal.XenyriaSchematic;
import de.xenyria.splatoon.XenyriaSplatoon;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class SchematicProvider {

    public SchematicProvider() {


    }

    private static ConcurrentHashMap<String, XenyriaSchematic> storedSchematics = new ConcurrentHashMap<>();

    public static XenyriaSchematic loadSchematic(String name) throws Exception {

        if(!storedSchematics.containsKey(name)) {

            XenyriaSchematic schematic = XenyriaSchematic.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + name));
            storedSchematics.put(name, schematic);
            return schematic;

        } else {

            return storedSchematics.get(name);

        }

    }

}
