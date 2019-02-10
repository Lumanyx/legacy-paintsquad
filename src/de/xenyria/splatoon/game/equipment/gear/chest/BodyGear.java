package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public abstract class BodyGear extends Gear {

    public BodyGear(int originID, Brand brand, String itemName, SpecialEffect baseEffect, Material material, Color color, int maxSubAbilities, int cost) {

        super(originID, GearType.CHESTPLATE, brand, itemName, baseEffect, material, color, maxSubAbilities, cost);

    }

}
