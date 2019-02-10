package de.xenyria.splatoon.game.equipment.gear.head;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

import java.awt.*;

public class WhiteHeadband extends HeadGear {

    public WhiteHeadband() {
        super(1, Brand.SQUID_FORCE, "Weißes Stirnband", SpecialEffect.INK_RECOVERY, Material.LEATHER_HELMET, Color.WHITE, 1, 200);
    }

    @Override
    public boolean useCustomModel() {
        return true;
    }

    @Override
    public short getCustomModelDamageValue() {
        return (short)1;
    }

    @Override
    public Material getCustomModelMaterial() {
        return Material.DIAMOND_HOE;
    }
}
