package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class ChocoLayeredLS extends BodyGear {

    public ChocoLayeredLS() {
        super(104, Brand.TAKOROKA, "Schoko-Lagen-Shirt", SpecialEffect.INK_SAVER_SUB, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                56, 42, 30
        ), 1, 1600);
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
