package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class ArmorJacketReplica extends BodyGear {

    public ArmorJacketReplica() {
        super(100, Brand.CUTTLEGEAR, "Rüstungsjacke-Replik", SpecialEffect.SPECIAL_CHARGE_UP, Material.LEATHER_CHESTPLATE, Color.BLACK, 3, 14000);
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
