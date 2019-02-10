package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class HeroJacketReplica extends BodyGear {

    public HeroJacketReplica() {
        super(106, Brand.CUTTLEGEAR, "Helden-Jacke-Replik", SpecialEffect.SWIM_SPEED_UP, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                100,100,30
        ), 3, 14000);
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
