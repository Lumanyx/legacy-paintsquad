package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class SportyBobbleHat extends HeadGear {

    public SportyBobbleHat() {
        super(8, Brand.SKALOP, "Winter-Pudelmütze", SpecialEffect.TENACITY, Material.LEATHER_HELMET, Color.PURPLE, 1, 800);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return 6;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
