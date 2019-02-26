package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.SprinklerProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public abstract class AbstractSprinkler extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    private float radius;

    public AbstractSprinkler(int id, String name, float radius) {

        super(id, name);
        this.radius = radius;

    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.SPRINKLER;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    public static double IMPULSE = 0.8d;

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        SprinklerProjectile projectile = new SprinklerProjectile(getPlayer(), this, getPlayer().getMatch());
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);
        getPlayer().getMatch().queueProjectile(projectile);

    }

    @Override
    public double getImpulse() {
        return IMPULSE;
    }

    @Override
    public void syncTick() {

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            SprinklerProjectile projectile = new SprinklerProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.spawn(IMPULSE, getPlayer().getShootingLocation(true), getPlayer().getLocation().getDirection());
            getPlayer().getMatch().queueProjectile(projectile);

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

        setNextInkUsage(10d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.PRISMARINE_SHARD;
    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void shoot() {



    }
}
