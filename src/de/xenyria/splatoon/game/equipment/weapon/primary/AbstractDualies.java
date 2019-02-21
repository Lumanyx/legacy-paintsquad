package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.equipment.weapon.util.SprayUtil;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

public abstract class AbstractDualies extends SplatoonPrimaryWeapon {

    private double baseDamage = 17d;
    public double getBaseDamage() { return baseDamage; }
    public void setBaseDamage(double baseDamage) { this.baseDamage = baseDamage; }

    private float impulse = 0.45f;
    public float getImpulse() { return impulse; }
    public void setImpulse(float impulse) { this.impulse = impulse; }

    private float maxSpray = 2f;
    public float getMaxSpray() { return maxSpray; }
    public void setMaxSpray(float maxSpray) { this.maxSpray = maxSpray; }

    public AbstractDualies(int id, String name, long shotDelay) {

        super(id, name);
        this.shotDelay = shotDelay;

    }

    private long shotDelay;
    private boolean weaponFlag;

    private double optimalRange = 3.5;
    public double getOptimalRange() { return optimalRange; }
    public void setOptimalRange(double d) { this.optimalRange = d; }

    public void cleanUp() {

        projectilesToSpawn = 0;

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.DUALIES;
    }

    public void syncTick() {

        //if(getPlayer().isShooting()) {

        dodgeLastUseTicker++;
        if(dodgeLastUseTicker > 20) {

            dodgeUseTicker = 0;

        }

        if(isSelected()) {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer humanPlayer = (SplatoonHumanPlayer) getPlayer();

                Inventory itemStacks = humanPlayer.getPlayer().getInventory();
                if (((PlayerInventory) itemStacks).getItem(EquipmentSlot.OFF_HAND) == null || ((PlayerInventory) itemStacks).getItem(EquipmentSlot.OFF_HAND).getType() == Material.AIR) {

                    ((PlayerInventory) itemStacks).setItem(EquipmentSlot.OFF_HAND, asItemStack());

                }

            }

        } else {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer humanPlayer = (SplatoonHumanPlayer) getPlayer();
                if (humanPlayer.getPlayer().getInventory().getItem(EquipmentSlot.OFF_HAND) != null) {

                    humanPlayer.getPlayer().getInventory().setItem(EquipmentSlot.OFF_HAND, null);

                }

            }

        }

        int copy = projectilesToSpawn;
        for (int i = 0; i < copy; i++) {

            weaponFlag = !weaponFlag;

            Location spawnLoc = getPlayer().getShootingLocation(!weaponFlag);
            Location target = getPlayer().getEyeLocation().clone();
            target = target.add(target.getDirection().clone().multiply(optimalRange));

            Vector direction = target.toVector().subtract(spawnLoc.toVector());
            Location location = new Location(getPlayer().getWorld(), 0,0,0);
            location.setDirection(direction);

            InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.withReason(DamageReason.WEAPON);
            projectile.withDamage(baseDamage);
            projectile.setDrippingRatio(17);
            projectile.setPaintBelowRatio(10, 5, 1);
            projectile.spawn(spawnLoc, SprayUtil.addSpray(location.getYaw(), maxSpray),
                    SprayUtil.addSpray(location.getPitch(), maxSpray), impulse);

        }
        projectilesToSpawn -= copy;

        //}

    }

    private int maxDodgesInRow = 2;
    private double dodgeSpeed = 0.95d;
    private int dodgeTicker = 0;
    private int dodgeUseTicker = 0;
    private int dodgeLastUseTicker = 0;
    private double dodgeUsage = 7d;

    public static final double DODGE_NEGATIVE_Y = -0.3;

    public void dodge() {

        Vector lastDelta = getPlayer().getLastDelta().clone().normalize();
        lastDelta.setY(0);
        if(lastDelta.length() > 0.025 && getPlayer().hasEnoughInk((float)dodgeUsage) && dodgeLastUseTicker > 5) {

            if(dodgeUseTicker < maxDodgesInRow) {

                dodgeTicker = 20;
                dodgeLastUseTicker = 0;
                dodgeUseTicker++;
                Vector newVel = getPlayer().getVelocity();
                newVel = newVel.add(lastDelta.clone().multiply(dodgeSpeed));
                if(newVel.getY() <= 0.01 && newVel.getY() > DODGE_NEGATIVE_Y) {

                    newVel.setY(DODGE_NEGATIVE_Y);

                }
                getPlayer().removeInk((float)dodgeUsage);
                getPlayer().setVelocity(newVel);
                getPlayer().getWorld().playSound(getPlayer().getLocation(),
                        Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 2f);


            }

        }

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
