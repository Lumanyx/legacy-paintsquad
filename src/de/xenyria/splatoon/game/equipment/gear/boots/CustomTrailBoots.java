package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class CustomTrailBoots extends FootGear {

    public CustomTrailBoots() {
        super(203, Brand.INKLINE, "Spezial-Wanderstiefel", SpecialEffect.SPECIAL_CHARGE_UP, Material.LEATHER_BOOTS, Color.PURPLE, 2, 6000);
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
