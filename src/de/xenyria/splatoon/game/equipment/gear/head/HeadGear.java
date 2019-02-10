package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public abstract class HeadGear extends Gear {

    public HeadGear(int originID, Brand brand, String itemName, SpecialEffect baseEffect, Material material, Color color, int abilities, int cost) {

        super(originID, GearType.HELMET, brand, itemName, baseEffect, material, color, abilities, cost);

    }

}
