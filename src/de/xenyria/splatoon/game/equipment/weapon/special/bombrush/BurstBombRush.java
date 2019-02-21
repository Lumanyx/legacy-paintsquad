package de.xenyria.splatoon.game.equipment.weapon.special.bombrush;

import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class BurstBombRush extends AbstractBombRush {

    public static final int ID = 40;

    public BurstBombRush() {
        super(ID, "Instabombenhagel", "§7Gibt dir eine unbegrenzte Anzahl\n§7Instabomben für einen kurzen Zeitraum.", AbstractBombRush.REQUIRED_POINTS, BombType.BURSTBOMB);
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
