package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public abstract class FootGear extends Gear {

    public FootGear(int originID, Brand brand, String itemName, SpecialEffect baseEffect, Material material, Color color, int maxSubAbilities, int cost) {
        super(originID, GearType.BOOTS, brand, itemName, baseEffect, material, color, maxSubAbilities, cost);
    }

}
