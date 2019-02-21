package de.xenyria.splatoon.game.equipment.weapon.special.bombrush;

import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;

public class SuctionBombRush extends AbstractBombRush {

    public static final int ID = 39;

    public SuctionBombRush() {
        super(ID, "Haftbombenhagel", "§7Gibt dir eine unbegrenzte Anzahl\n§7Haftbomben für einen kurzen Zeitraum.", AbstractBombRush.REQUIRED_POINTS, BombType.SUCTIONBOMB);
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
