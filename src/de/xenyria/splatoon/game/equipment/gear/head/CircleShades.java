package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class CircleShades extends HeadGear {

    public CircleShades() {
        super(4, Brand.ROCKENBERG, "Rundbrille", SpecialEffect.SWIM_SPEED_UP, Material.LEATHER_HELMET, Color.GREEN, 3, 12000);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return (short)2;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
