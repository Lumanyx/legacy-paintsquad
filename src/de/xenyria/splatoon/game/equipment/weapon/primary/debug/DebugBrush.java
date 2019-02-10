package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrush;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class DebugBrush extends AbstractBrush {

    public static final int ID = 24;

    public DebugBrush() {
        super(ID, "Quasto", 0.1f, 0.3f, 1f, 0.2f, 8f, 17f, 34f);
        splatTicks = 4;
        impulse = 0.4f;
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }
}
