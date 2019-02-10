package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplatling;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class MiniSplatling extends AbstractSplatling {

    public static final int ID = 33;

    public MiniSplatling() {

        super(ID, "Klecks-Splatling", 23, 700, 18);
        this.impulse = (float) 0.62;
        this.maxPitchOffset = 5f;
        this.maxYawOffset = 13f;
        this.ticksPerProjectile = 1;
        this.projectilesPerFullCharge = 12;

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.MINI_SPLATLING;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
