package de.xenyria.splatoon.game.map;

import org.bukkit.Material;

import java.util.ArrayList;

public class PaintDefinition {

    private ArrayList<Material> paintableMaterials = new ArrayList<>();
    public ArrayList<Material> getPaintableMaterials() { return paintableMaterials; }

    public boolean canPaint(Material material) { return paintableMaterials.contains(material); }

}
