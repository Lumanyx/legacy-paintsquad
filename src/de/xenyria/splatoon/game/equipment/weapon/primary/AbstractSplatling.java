package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.equipment.weapon.util.SprayUtil;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.PacketPlayOutAbilities;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class AbstractSplatling extends SplatoonPrimaryWeapon {

    private float maxDamage;
    private long chargeBegin, chargeTarget, chargeDuration, lastChargeUpdate;
    private float fullUsage;
    public int projectilesPerFullCharge = 20;

    public void cleanUp() {

        projectilesToFire = 0;
        lastChargeUpdate = 0;
        chargeTarget = 0;
        chargeBegin = 0;

    }

    private int projectilesInThisRound = 0;
    private int projectilesToFire = 0;

    private int projectileTicker = 0;
    public int ticksPerProjectile = 1;

    public boolean isCharging() {

        return System.currentTimeMillis() - lastChargeUpdate < 420 && projectilesToFire == 0;

    }

    public AbstractSplatling(int id, String name, float fullUsage, long chargeTime, float maxDamage) {

        super(id, name);
        this.chargeDuration = chargeTime;
        this.maxDamage = maxDamage;
        this.fullUsage = fullUsage;

    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() { return PrimaryWeaponType.SPLATLING; }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    public float maxYawOffset = 7f;
    public float maxPitchOffset = 7f;
    public float impulse = 1.05f;
    private boolean chargeSoundPlayed = false;

    public int getSlownessModifier() {

        return 3;

    }

    @Override
    public void syncTick() {

        if(isSelected()) {

            if (projectilesToFire > 0) {

                chargeSoundPlayed = false;
                float percentage = ((float)projectilesToFire / (float)projectilesPerFullCharge);
                percentage *= 100f;
                percentage = (float) Math.ceil(percentage);

                String filled = getPlayer().getTeam().getColor().prefix() + "§l";
                String notFilled = "§8§l";
                for (int i = 0; i < 100; i += 5) { if (i <= percentage) { filled += "*"; } else { notFilled += "*"; } }
                getPlayer().sendActionBar(filled + notFilled);

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    Player player = ((SplatoonHumanPlayer)getPlayer()).getPlayer();

                    if(!player.hasPotionEffect(PotionEffectType.SLOW)) {

                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOW, (projectilesToFire * ticksPerProjectile) + 3, getSlownessModifier(), false, false, false
                        ));
                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAbilities(
                                ((CraftPlayer)player).getHandle().abilities
                        ));

                    }

                }

                projectileTicker++;
                if(projectileTicker > ticksPerProjectile) {

                    projectileTicker = 0;
                    projectilesToFire--;
                    double inkPerProjectile = fullUsage / projectilesPerFullCharge;

                    if (getPlayer().hasEnoughInk((float) inkPerProjectile)) {

                        getPlayer().removeInk((float) inkPerProjectile);
                        InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
                        projectile.withDamage(maxDamage);
                        projectile.setPaintBelowRatio(3, 6, 1);
                        float yaw = SprayUtil.addSpray(getPlayer().yaw(), maxYawOffset);
                        float pitch = SprayUtil.addSpray(getPlayer().pitch(), maxPitchOffset);
                        projectile.spawn(getPlayer().getShootingLocation(true).clone().add(0, -0.4, 0), yaw, pitch, impulse);
                        getPlayer().getMatch().queueProjectile(projectile);

                        if(projectilesToFire < 1) {

                            chargeTarget = 0;

                        }

                    } else {

                        projectilesToFire = 0;
                        chargeTarget = 0;

                        if(getPlayer() instanceof SplatoonHumanPlayer) {

                            Player player = ((SplatoonHumanPlayer) getPlayer()).getPlayer();
                            player.removePotionEffect(PotionEffectType.SLOW);

                        }

                    }

                }

            } else {

                long remainingMillis = chargeTarget - System.currentTimeMillis();
                float percentage = 1f - (((float) remainingMillis) / (float) chargeDuration);

                if (chargeTarget == 0) {
                    percentage = 0;
                }

                float percentageAlt = percentage*100;
                if(percentageAlt >= 5) {

                    if(percentageAlt >= 99) {

                        if(!chargeSoundPlayed) {

                            if(getPlayer() instanceof SplatoonHumanPlayer) {

                                SplatoonHumanPlayer player = (SplatoonHumanPlayer)getPlayer();
                                player.getPlayer().playSound(player.getPlayer().getLocation(),
                                        Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);


                            }
                            chargeSoundPlayed = true;

                        }

                    }

                    if (isSelected()) {

                        percentage *= 100f;
                        percentage = (float) Math.floor(percentage);
                        if (percentage < 4) {
                            percentage = 0;
                        }

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

                //}

            }

        } else {

            chargeSoundPlayed = false;
            if(projectilesToFire > 0) {

                projectilesToFire = 0;

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    Player player = ((SplatoonHumanPlayer) getPlayer()).getPlayer();

                    player.removePotionEffect(PotionEffectType.SLOW);
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAbilities(
                            ((CraftPlayer) player).getHandle().abilities
                    ));

                }

            }

        }

    }

    @Override
    public void asyncTick() {

        if(projectilesToFire != 0) { return; }

        if(!isCharging() && chargeTarget == 0) {

            if (getPlayer().isShooting() && isSelected()) {

                if(getPlayer().hasEnoughInk((float)(fullUsage))) {

                    chargeBegin = System.currentTimeMillis();
                    chargeTarget = chargeBegin + chargeDuration;
                    lastChargeUpdate = System.currentTimeMillis();

                } else {

                    getPlayer().notEnoughInk();

                }

            }

        } else {

            if(projectilesToFire > 0 || chargeTarget == 0) { return; }

            if(!getPlayer().isShooting()) {

                chargeBegin = 0;
                boolean fullyCharged = System.currentTimeMillis() >= chargeTarget;
                long remainingMillis = chargeTarget - System.currentTimeMillis();
                float percentage = 1f - (((float) remainingMillis) / (float) chargeDuration);
                projectilesToFire = (int) ((float)projectilesPerFullCharge * percentage);
                projectilesInThisRound = projectilesToFire;

                if(dealtSlowness) {

                    getPlayer().disableWalkSpeedOverride();
                    dealtSlowness = false;

                }

            } else {

                if(isSelected()) { lastChargeUpdate = System.currentTimeMillis(); }
                projectilesToFire = 0;
                if(!dealtSlowness && isSelected()) {

                    getPlayer().enableWalkSpeedOverride();
                    dealtSlowness = true;
                    getPlayer().setOverrideWalkSpeed(0.08f);

                }

            }

        }

    }

    private boolean dealtSlowness = false;

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.IRON_HORSE_ARMOR;
    }

    @Override
    public void shoot() {

    }

}
