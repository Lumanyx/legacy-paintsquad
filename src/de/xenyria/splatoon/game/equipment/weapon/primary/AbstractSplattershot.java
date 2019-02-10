package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponShooter;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.equipment.weapon.util.SprayUtil;
import org.bukkit.Location;
import org.bukkit.Sound;

public abstract class AbstractSplattershot extends SplatoonPrimaryWeapon implements AIWeaponShooter {

    private double baseDamage = 24d;
    public double getBaseDamage() { return baseDamage; }
    public void setBaseDamage(double baseDamage) { this.baseDamage = baseDamage; }

    private float impulse = 0.45f;
    public float getImpulse() { return impulse; }
    public void setImpulse(float impulse) { this.impulse = impulse; }

    private float nextYawSpray, nextPitchSpray;


    public float nextSprayYaw() {

        return nextYawSpray;

    }
    public float nextSprayPitch() {

        return nextPitchSpray;

    }

    public void calculateNextSprayValues() {

        nextPitchSpray = SprayUtil.calculateSpray(maxSpray);
        nextYawSpray = SprayUtil.calculateSpray(maxSpray);

    }

    private float maxSpray = 7f;
    public float getMaxSpray() { return maxSpray; }
    public void setMaxSpray(float maxSpray) { this.maxSpray = maxSpray; }

    public AbstractSplattershot(int id, String name, long shotDelay) {

        super(id, name);
        this.shotDelay = shotDelay;

    }

    public void onValidFireTick() {

        EntityNPC npc = (EntityNPC) getPlayer();
        AIWeaponManager.TrajectoryTargetPair trajectory = npc.getWeaponManager().trajectoryToTarget(getImpulse());
        if(trajectory != null) {

            spawnProjectile(trajectory.trajectory, trajectory.target);

        } else {

            spawnProjectile(null, null);

        }

    }

    public void spawnProjectile(Trajectory trajectory, Location plannedHitLocation) {

        InkProjectile projectile = null;
        projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());

        projectile.withReason(DamageReason.WEAPON);
        projectile.withDamage(baseDamage);
        projectile.setDrippingRatio(20);
        projectile.setPaintBelowRatio(10, 5, 1);
        getPlayer().getLocation().getWorld().playSound(getPlayer().getLocation(),
                Sound.BLOCK_WOOL_HIT, 1f, 2f);

        if(trajectory == null) {

            projectile.spawn(getPlayer().getShootingLocation(true).clone().add(0, -0.5, 0), getPlayer().yaw() + nextYawSpray,
                    getPlayer().pitch() + nextPitchSpray, impulse);

        } else {

            projectile.spawn(trajectory, getPlayer().getShootingLocation(true).clone(), plannedHitLocation);

        }

    }

    private long shotDelay;

    public void syncTick() {

        //if(getPlayer().isShooting()) {

            int copy = projectilesToSpawn;
            for (int i = 0; i < copy; i++) {

                if(assignedToNPC()) {

                    onValidFireTick();

                } else {

                    spawnProjectile(null, null);

                }

            }
            projectilesToSpawn -= copy;

            calculateNextSprayValues();

        //}

    }


    private int projectilesToSpawn = 0;
    private long lastProjectileIncrement = 0;

    public void asyncTick() {

        if(getPlayer().isShooting() && isSelected() && getPlayer().canUseMainWeapon()) {

            if ((lastProjectileIncrement + shotDelay) < System.currentTimeMillis()) {

                if (getPlayer().getInk() >= getNextInkUsage()) {

                    getPlayer().removeInk(getNextInkUsage());
                    lastProjectileIncrement = System.currentTimeMillis();
                    projectilesToSpawn++;

                } else {

                    getPlayer().notEnoughInk();

                }

            }

        }

    }

}
