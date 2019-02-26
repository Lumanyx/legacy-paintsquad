package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractToxicMist;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class ToxicMist extends AbstractToxicMist {

    public static final int ID = 36;

    public ToxicMist() {
        super(ID, "Sepitox-Nebel");
    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.GRENADE;
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
