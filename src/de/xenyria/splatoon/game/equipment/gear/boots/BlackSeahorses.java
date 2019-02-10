package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class BlackSeahorses extends FootGear {

    public BlackSeahorses() {
        super(201, Brand.ZINK, "Schwarze Low-Top-Sneaker", SpecialEffect.SWIM_SPEED_UP, Material.LEATHER_BOOTS, Color.GRAY, 2, 5000);
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
