package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class BluePeaksTee extends BodyGear {

    public BluePeaksTee() {
        super(102, Brand.INKLINE, "Eisgipfel-Shirt", SpecialEffect.INK_SAVER_SUB, Material.LEATHER_CHESTPLATE, Color.fromRGB(37, 52, 66), 1, 600);
    }

    @Override
    public boolean useCustomModel() {
        return false;
    }

    @Override
    public short getCustomModelDamageValue() {
        return 0;
    }

    @Override
    public Material getCustomModelMaterial() {
        return null;
    }
}
