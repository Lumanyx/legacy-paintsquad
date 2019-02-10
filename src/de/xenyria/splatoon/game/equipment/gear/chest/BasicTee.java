package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class BasicTee extends BodyGear {

    public BasicTee() {
        super(2, Brand.SQUID_FORCE, "Alltagsshirt", SpecialEffect.FAST_RESPAWN, Material.LEATHER_CHESTPLATE, Color.YELLOW, 1, 200);
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
