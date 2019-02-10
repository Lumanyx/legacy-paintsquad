package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractDualies;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;

public class DebugDualies extends AbstractDualies {

    public DebugDualies() {
        super(11, "Protodoppler", 120);
        setImpulse(.65f);
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.DUALIES;
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(1.2);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.WOODEN_HOE;
    }

    @Override
    public void shoot() {

    }
}
