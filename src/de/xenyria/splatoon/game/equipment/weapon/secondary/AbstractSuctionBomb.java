package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.SuctionBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.SuctionBombProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public abstract class AbstractSuctionBomb extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    private float radius;

    @Override
    public double getImpulse() { return SuctionBomb.IMPULSE; }

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        SuctionBombProjectile projectile = new SuctionBombProjectile(getPlayer(), this, getPlayer().getMatch());
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);
        getPlayer().getMatch().queueProjectile(projectile);

    }

    public AbstractSuctionBomb(int id, String name, float radius) {

        super(id, name);
        this.radius = radius;

    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.GRENADE;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            SuctionBombProjectile projectile = new SuctionBombProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.spawn(SuctionBomb.IMPULSE, getPlayer().getShootingLocation(true), getPlayer().getLocation().getDirection());
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

        setNextInkUsage(70d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.SLIME_SPAWN_EGG;
    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void shoot() {



    }

}
