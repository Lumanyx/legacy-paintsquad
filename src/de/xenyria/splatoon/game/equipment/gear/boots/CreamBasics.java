package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class CreamBasics extends FootGear {

    public CreamBasics() {
        super(3, Brand.KRAKON, "Weiße Leinenschuhe", SpecialEffect.SPECIAL_LOSE, Material.LEATHER_BOOTS, Color.fromRGB(240, 224, 136), 1, 200);
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
