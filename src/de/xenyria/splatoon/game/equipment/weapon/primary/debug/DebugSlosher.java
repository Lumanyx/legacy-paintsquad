package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSlosher;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class DebugSlosher extends AbstractSlosher {

    public DebugSlosher() {
        super(15, "Protoschwapper");
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {

        return PrimaryWeaponType.SLOSHER;

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

    }

    @Override
    public void shoot() {

    }
}
