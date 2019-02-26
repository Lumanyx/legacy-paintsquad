package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.autobomb.AutobombProjectile;
import org.bukkit.Location;
import org.bukkit.Material;

public class Autobomb extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    public static final double IMPULSE = BurstBomb.IMPULSE;
    public static int ID = 41;

    public Autobomb() {
        super(ID, "Robobombe");
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
    public void syncTick() {

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            AutobombProjectile projectile = new AutobombProjectile(getPlayer(), this, getPlayer().getMatch());
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

        setNextInkUsage(55d);

    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.CHICKEN_SPAWN_EGG;
    }

    @Override
    public void shoot() {

    }

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

        AutobombProjectile projectile = new AutobombProjectile(getPlayer(), this, getPlayer().getMatch());
        projectile.spawn(trajectory, getPlayer().getShootingLocation(false), target);

    }

    @Override
    public double getImpulse() {
        return IMPULSE;
    }
}
