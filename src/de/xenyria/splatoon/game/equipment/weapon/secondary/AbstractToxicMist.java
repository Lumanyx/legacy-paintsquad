package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.projectile.DetectorProjectile;
import de.xenyria.splatoon.game.projectile.ToxicMistProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public abstract class AbstractToxicMist extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    public AbstractToxicMist(int id, String name) {
        super(id, name);
    }

    @Override
    public double getImpulse() {
        return 0.725d;
    }

    @Override
    public void syncTick() {

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            ToxicMistProjectile projectile = new ToxicMistProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.spawn(0.725d, getPlayer().getShootingLocation(true));

        }
        projectilesToSpawn -= copy;

    }

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        DetectorProjectile projectile = new DetectorProjectile(getPlayer(), this, getPlayer().getMatch());
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);

    }

    private long lastProjectileIncrement;
    private int projectilesToSpawn;
    private float activationDelay, radius;

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting() && isSelected() && getPlayer().canUseSecondaryWeapon()) {

            if(lastThrowSeconds() > 1) {

                if (getPlayer().getInk() >= getNextInkUsage()) {

                    getPlayer().removeInk(getNextInkUsage());
                    lastProjectileIncrement = System.currentTimeMillis();
                    lastThrow = lastProjectileIncrement;
                    projectilesToSpawn++;

                } else {

                    getPlayer().notEnoughInk();

                }

            }

        }

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.SPLASH_POTION;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(45d);

    }

}
