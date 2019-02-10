package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class LegacyHeadGear extends HeadGear {

    public LegacyHeadGear(int originID, Brand brand, String itemName, SpecialEffect baseEffect, Material material, Color color, int abilities, int cost) { super(originID, brand, itemName, baseEffect, material, color, abilities, cost); }

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
