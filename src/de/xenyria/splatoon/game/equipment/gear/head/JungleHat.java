package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class JungleHat extends HeadGear {
    public JungleHat() {
        super(7, Brand.FIREFIN, "Tarnhut", SpecialEffect.INK_SAVER_MAIN, Material.LEATHER_HELMET, Color.RED, 3, 11000);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return 5;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
