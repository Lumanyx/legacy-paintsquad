package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class AerosprayMG extends AbstractSplattershot {

    public static final int ID = 30;

    public AerosprayMG() {

        super(ID, "Airbrush MG", 66);
        setBaseDamage(12d);
        setImpulse(0.51f);
        setMaxSpray(10);

    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.SPLATTERSHOT;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public float getMovementSpeedOffset() {
        return -0.1f;
    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(0.5f);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STONE_HOE;
    }

    @Override
    public void shoot() {

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.AEROSPRAY_MG;
    }
}
