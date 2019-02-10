package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class CrazyArrows extends FootGear {

    public CrazyArrows() {
        super(202, Brand.TAKOROKA, "Bunte Pfeilschuhe", SpecialEffect.STEALTH_JUMP, Material.LEATHER_BOOTS, null, 3, 12000);
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
