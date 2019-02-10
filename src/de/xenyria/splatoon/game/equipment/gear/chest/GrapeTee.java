package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class GrapeTee extends BodyGear {

    public GrapeTee() {
        super(105, Brand.SKALOP, "Lila Shirt", SpecialEffect.INK_RECOVERY, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                77, 29, 66
        ), 2, 4500);
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
