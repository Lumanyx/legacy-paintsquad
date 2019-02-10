package de.xenyria.splatoon.game.equipment.gear.chest;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import org.bukkit.Color;
import org.bukkit.Material;

public class CamoZipHoodie extends BodyGear {

    public CamoZipHoodie() {
        super(103, Brand.FIREFIN, "Tarnhoodie", SpecialEffect.FAST_RESPAWN, Material.LEATHER_CHESTPLATE, Color.fromRGB(
                70, 58, 28
        ), 3, 12000);
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
