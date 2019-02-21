package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.BurstBomb;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.BurstBombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public abstract class AbstractBurstBomb extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    private float radius, damage;

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        BurstBombProjectile projectile = new BurstBombProjectile(getPlayer(), this, getPlayer().getMatch(), radius, damage);
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);
        getPlayer().getMatch().queueProjectile(projectile);

    }

    public AbstractBurstBomb(int id, String name, float radius, float damage) {

        super(id, name);
        this.radius = radius;
        this.damage = damage;

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

            BurstBombProjectile projectile = new BurstBombProjectile(getPlayer(), this, getPlayer().getMatch(), radius, damage);
            projectile.spawn(BurstBomb.IMPULSE, getPlayer().getShootingLocation(false));
            getPlayer().getMatch().queueProjectile(projectile);

        }
        projectilesToSpawn -= copy;

    }

    private long lastProjectileIncrement;
    private int projectilesToSpawn;

    @Override
    public void asyncTick() {

        boolean throwBomb = false;
        if(getPlayer() instanceof EntityNPC) {

            throwBomb = getPlayer().isShooting();

        } else {

            throwBomb = ((SplatoonHumanPlayer)getPlayer()).millisSinceLastInteraction() < 50;

        }

        if(throwBomb && isSelected() && getPlayer().canUseSecondaryWeapon()) {

            if(System.currentTimeMillis() - lastThrow > 200) {

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

        setNextInkUsage(33d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.SUGAR;
    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void shoot() {



    }

}
