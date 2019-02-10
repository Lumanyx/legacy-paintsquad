package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class OctoLayeredLS extends BodyGear {

    public OctoLayeredLS() {
        super(108, Brand.CUTTLEGEAR, "Okto-Lagen-Shirt", SpecialEffect.INK_SAVER_MAIN, Material.LEATHER_CHESTPLATE, Color.RED, 3, 13000);
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
