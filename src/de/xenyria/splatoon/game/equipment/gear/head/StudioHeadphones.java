package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class StudioHeadphones extends HeadGear {

    public StudioHeadphones() {
        super(5, Brand.FORGE, "Studio-Kopfhörer", SpecialEffect.INK_SAVER_MAIN, Material.LEATHER_HELMET, Color.WHITE, 2, 4000);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return 3;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
