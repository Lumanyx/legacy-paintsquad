package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.projectile.RainMakerProjectile;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class RainMaker extends SplatoonPrimaryWeapon {

    public RainMaker() {
        super(19, "Goldfischkanone");
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.MODE_EXCLUSIVE;
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

    }

    private long chargeBegin;
    private long lastChargeUpdate;
    private long chargeTarget;
    private float range;
    private long chargeDuration = 700;

    public boolean isCharging() {

        return System.currentTimeMillis() - lastChargeUpdate < 420;

    }

    private boolean dealtSlowness;

    @Override
    public void asyncTick() {

        if(!isCharging()) {

            if (getPlayer().isShooting() && isSelected()) {

                chargeBegin = System.currentTimeMillis();
                chargeTarget = chargeBegin + chargeDuration;
                lastChargeUpdate = System.currentTimeMillis();

            }

        } else {

            if(!getPlayer().isShooting()) {

                chargeBegin = 0;
                boolean fullyCharged = System.currentTimeMillis() >= chargeTarget;
                long remainingMillis = chargeTarget - System.currentTimeMillis();
                if(remainingMillis > 0 || fullyCharged) {

                    if(isSelected()) {

                        float percentage = 1f - (((float) remainingMillis) / (float) chargeDuration);
                        if (percentage > 1 || fullyCharged) { percentage = 1f; }
                        lastChargeUpdate = 0;
                        RainMaker maker = this;

                        Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                            RainMakerProjectile projectile = new RainMakerProjectile(getPlayer(), maker, getPlayer().getMatch());
                            projectile.launch(getPlayer().getShootingLocation(true), getPlayer().getEyeLocation().getDirection(), 0.91d, fullyCharged);
                            getPlayer().getMatch().queueProjectile(projectile);

                        });

                        if(dealtSlowness) {

                            getPlayer().disableWalkSpeedOverride();
                            dealtSlowness = false;

                        }

                    } else {

                        chargeBegin = 0;
                        lastChargeUpdate = 0;

                    }

                }

            } else {

                if(!dealtSlowness && isSelected()) {

                    getPlayer().enableWalkSpeedOverride();
                    dealtSlowness = true;
                    getPlayer().setOverrideWalkSpeed(0.08f);

                }

                long remainingMillis = chargeTarget - System.currentTimeMillis();
                if(isSelected()) {

                    lastChargeUpdate = System.currentTimeMillis();

                }

                boolean fullCharge = remainingMillis < 1;
                if(remainingMillis > 0) {

                    if(isSelected()) {

                        float percentage = 1f - (((float) remainingMillis) / (float) chargeDuration);
                        float ratio = percentage / 3f;
                        percentage *= 100f;
                        percentage = (float) Math.ceil(percentage);

                        int barCount = 10;
                        String filled = getPlayer().getTeam().getColor().prefix() + "§l";
                        String notFilled = "§8§l";
                        for (int i = 0; i < 100; i += 5) {

                            if (i <= percentage) {

                                filled += "*";

                            } else {

                                notFilled += "*";

                            }

                        }

                        getPlayer().sendActionBar(filled + notFilled);

                    }

                }

            }

        }

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.GOLDEN_HORSE_ARMOR;
    }

    @Override
    public void shoot() {

    }
}
