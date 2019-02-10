package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class LegacyFootGear extends FootGear {
    public LegacyFootGear(int originID, Brand brand, String itemName, SpecialEffect baseEffect, Material material, Color color, int maxSubAbilities, int cost) {
        super(originID, brand, itemName, baseEffect, material, color, maxSubAbilities, cost);
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
