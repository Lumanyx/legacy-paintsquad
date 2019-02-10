package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class BlackHoodie extends BodyGear {
    public BlackHoodie() {
        super(101, Brand.SKALOP, "Schwarzer Hoodie", SpecialEffect.INK_RESISTANCE_UP, Material.LEATHER_CHESTPLATE, Color.GRAY, 2, 3800);
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
