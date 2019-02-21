package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.SplatBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public abstract class AbstractSplatBomb extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        BombProjectile projectile = new BombProjectile(getPlayer(), this, getPlayer().getMatch(), radius, (int)activationDelay, 145, true);
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);

    }

    private float activationDelay, radius;

    public AbstractSplatBomb(int id, String name, float activationDelay, float radius) {

        super(id, name);
        this.activationDelay = activationDelay;
        this.radius = radius;

    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() { return SecondaryWeaponType.GRENADE; }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            BombProjectile projectile = new BombProjectile(getPlayer(), this, getPlayer().getMatch(), radius, 20, SplatBomb.MAX_DAMAGE, true);
            projectile.spawn(SplatBomb.IMPULSE, getPlayer().getShootingLocation(true));

        }
        projectilesToSpawn -= copy;

    }

    private long lastProjectileIncrement;
    private int projectilesToSpawn;

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
    public boolean canUse() { return !getPlayer().inSuperJump() &&
            !getPlayer().isSquid() && !getPlayer().isSplatted() && !getPlayer().isBeingDragged(); }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(68d);

    }

    @Override
    public double getImpulse() {
        return SplatBomb.IMPULSE;
    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.GUNPOWDER;
    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void shoot() {



    }

}
