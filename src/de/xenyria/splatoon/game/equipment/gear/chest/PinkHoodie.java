package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class PinkHoodie extends BodyGear {

    public PinkHoodie() {
        super(109, Brand.SPLASH_MOB, "Pinker Hoodie", SpecialEffect.BOMB_DEFENSE_UP, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                100, 65, 66
        ), 2, 7000);
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
