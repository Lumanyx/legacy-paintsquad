package de.xenyria.splatoon.game.equipment.gear.boots;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class ArmorBootsReplica extends FootGear {

    public ArmorBootsReplica() {
        super(200, Brand.CUTTLEGEAR, "Rüstungsstiefel Replik", SpecialEffect.INK_SAVER_MAIN, Material.LEATHER_BOOTS, Color.BLACK, 3, 12000);
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
