package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractDetector;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class Detector extends AbstractDetector {

    public static final int ID = 35;

    public Detector() {
        super(ID, "Punktsensor");
    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.DETECTOR;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void shoot() {

    }
}
