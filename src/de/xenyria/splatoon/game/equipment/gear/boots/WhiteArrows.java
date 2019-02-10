package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class WhiteArrows extends FootGear {

    public WhiteArrows() {
        super(205, Brand.TAKOROKA, "Weiße Pfeilschuhe", SpecialEffect.SPECIAL_LOSE, Material.IRON_BOOTS, Color.WHITE, 2, 3500);
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
