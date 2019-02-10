package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class OctoHeadphones extends HeadGear {

    public OctoHeadphones() {
        super(6, Brand.CUTTLEGEAR, "Studio-Oktohörer", SpecialEffect.INK_RECOVERY, Material.LEATHER_HELMET, Color.RED, 3, 9000);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return 4;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
