package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class MintTee extends BodyGear {

    public MintTee() {
        super(107, Brand.SKALOP, "Minzgrünes Shirt", SpecialEffect.BOMB_DEFENSE_UP, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                21, 71, 64
        ), 1, 1200);
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
