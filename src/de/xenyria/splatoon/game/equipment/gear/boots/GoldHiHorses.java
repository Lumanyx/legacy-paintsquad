package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class GoldHiHorses extends FootGear {

    public GoldHiHorses() {
        super(204, Brand.ZINK, "Goldene High-Top-Sneaker", SpecialEffect.SUPER_JUMP, Material.GOLDEN_BOOTS, Color.BLACK, 3, 7000);
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
