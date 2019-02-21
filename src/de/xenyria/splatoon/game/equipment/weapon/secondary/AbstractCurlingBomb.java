package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.CurlingBombProjectile;
import de.xenyria.splatoon.game.projectile.DamageDealingProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;

public abstract class AbstractCurlingBomb extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    private float radius;

    public AbstractCurlingBomb(int id, String name, float radius) {

        super(id, name);
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

            CurlingBombProjectile projectile = new CurlingBombProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.spawn(getPlayer().getLocation());
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

        setNextInkUsage(40d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.FLINT;
    }

    private long lastThrow = 0;
    public int lastThrowSeconds() { return (int) ((System.currentTimeMillis() - lastThrow) / 1000f); }

    @Override
    public void shoot() {



    }

}
