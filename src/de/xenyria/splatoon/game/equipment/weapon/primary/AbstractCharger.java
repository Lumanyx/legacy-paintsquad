package de.xenyria.splatoon.game.equipment.weapon.primary;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponCharger;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.BlockUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.function.Predicate;

public abstract class AbstractCharger extends SplatoonPrimaryWeapon implements AIWeaponCharger {

    @Override
    public float nextSprayPitch() { return 0; }
    public float nextSprayYaw() { return 0; }

    @Override
    public long estimatedChargeTimeForTargetDistance(double distance) {

        long totalTime = chargeDuration;
        double mod = (distance/getRange());
        return (long) (mod*(totalTime)) + 200;

    }

    public double getRange() {
        return range-2f;
    }

    public float maxDamage, range;
    private long chargeBegin, chargeTarget, chargeDuration, lastChargeUpdate;
    private float fullUsage;
    public boolean isCharging() {

        return System.currentTimeMillis() - lastChargeUpdate < 420;

    }

    public AbstractCharger(int id, String name, float fullUsage, long chargeTime, float maxDamage) {

        super(id, name);
        range = 12.5f;
        this.chargeDuration = chargeTime;
        this.maxDamage = maxDamage;
        this.fullUsage = fullUsage;

    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() { return PrimaryWeaponType.CHARGER; }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {



    }

    public void enableStoreCharge(long maxStoreMillis) {

        storeCharge = true;
        this.maxStore = maxStoreMillis;

    }

    private boolean storeCharge;
    private long maxStore;
    private boolean storeFlag;
    private long storeBeginTime;

    private boolean zoomPacketSent;
    private boolean zoom;
    private float zoomModificator;
    public void enableZoom(float zoomModificator) {

        zoom = true;
        this.zoomModificator = zoomModificator;

    }

    @Override
    public void asyncTick() {

        if(!isCharging()) {

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

            if(!getPlayer().isShooting()) {

                if(zoom) {

                    if(zoomPacketSent) {

                        if(getPlayer() instanceof SplatoonHumanPlayer) {

                            ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle().playerConnection.sendPacket(
                                    new PacketPlayOutAbilities(

                                            ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle().abilities

                                    )
                            );
                            zoomPacketSent = false;

                        }

                    }

                }

                chargeBegin = 0;
                boolean fullyCharged = System.currentTimeMillis() >= chargeTarget;
                long remainingMillis = chargeTarget - System.currentTimeMillis();
                if(remainingMillis > 0 || fullyCharged) {

                    if(isSelected()) {

                        float percentage = 1f - (((float) remainingMillis) / (float) chargeDuration);
                        if (percentage > 1 || fullyCharged) {
                            percentage = 1f;
                        }

                        getPlayer().removeInk(fullUsage * percentage);
                        final float fPercentage = percentage;
                        lastChargeUpdate = 0;
                        final float maxRange = range * fPercentage;

                        Location begin = getPlayer().getShootingLocation(true).clone().add(getPlayer().getLocation().getDirection().clone().multiply(0.5));
                        Location target = begin.clone().add(getPlayer().getLocation().getDirection().multiply(maxRange));

                        float dmg = maxDamage * fPercentage;
                        if (!fullyCharged) { dmg /= 2; }
                        RayProjectile projectile = new RayProjectile(getPlayer(), this, getPlayer().getMatch(), begin, getPlayer().getEyeLocation().getDirection(), dmg);
                        Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                            double hitRange = maxRange;
                            HitableEntity foundEntity = projectile.getHitEntity(maxRange, true);
                            boolean hit = foundEntity != null;
                            Block hitWall = null;

                            if(foundEntity != null) {

                                foundEntity.onProjectileHit(projectile);

                                if(fullyCharged) {

                                    getPlayer().getMatch().colorSquare(
                                            BlockUtil.ground(foundEntity.getLocation(), 256), getPlayer().getTeam(), getPlayer(), 4);

                                }
                                MinecraftKey key;
                                RayTraceResult result = projectile.rayTrace(foundEntity.aabb(), fPercentage*range);

                                if(result != null && result.getHitPosition() != null) {

                                    hitRange = begin.toVector().distance(result.getHitPosition());

                                }

                                Vector hitLocation = null;
                                if(result != null && result.getHitPosition() != null) {

                                    hitLocation = result.getHitPosition();

                                } else {

                                    hitLocation = begin.clone().add(getPlayer().getEyeLocation().getDirection().clone().multiply(hitRange)).toVector();

                                }

                                if(getPlayer() instanceof SplatoonHumanPlayer) {

                                    ((SplatoonHumanPlayer) getPlayer()).getPlayer().playSound(((SplatoonHumanPlayer) getPlayer()).getPlayer().getLocation(),
                                            Sound.BLOCK_GLASS_BREAK, 1f, 2f);
                                    ((SplatoonHumanPlayer) getPlayer()).getPlayer().playSound(((SplatoonHumanPlayer) getPlayer()).getPlayer().getLocation(),
                                            Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                                    getPlayer().hitMark(hitLocation.toLocation(((SplatoonHumanPlayer) getPlayer()).getPlayer().getWorld()));


                                }
                            } else {

                                RayTraceResult result = getPlayer().getMatch().getWorldInformationProvider().rayTraceBlocks(begin.toVector(), getPlayer().getLocation().getDirection().clone(), maxRange,true);
                                if(result != null) {

                                    hitRange = (float) result.getHitPosition().distance(begin.toVector());

                                    if(result.getHitBlock() != null) {

                                        hitWall = result.getHitBlock().getRelative(result.getHitBlockFace().getOppositeFace());

                                    }

                                }

                            }

                            ArrayList<Block> blocks = RayProjectile.rayCastBlocks(hitRange, begin, getPlayer().getLocation().getDirection().clone(), getPlayer().getMatch());

                            storeBeginTime = 0;
                            storeFlag = false;

                            for(double d = 0; d < hitRange; d+=0.25) {

                                Vector vec = begin.clone().add(getPlayer().getLocation().getDirection().clone().multiply(d)).toVector();
                                SplatoonServer.broadcastColorizedBreakParticle(getPlayer().getWorld(), vec.getX(), vec.getY(), vec.getZ(), getPlayer().getTeam().getColor());

                            }

                            int i = 0;
                            for (Block block : blocks) {

                                Location location = block.getLocation();
                                if (getPlayer().getMatch().isPaintable(getPlayer().getTeam(), block.getX(), block.getY(), block.getZ())) {

                                    getPlayer().getMatch().paint(getPlayer(), location.toVector(), getPlayer().getTeam());
                                    getPlayer().getMatch().colorSquare(location.getBlock(), getPlayer().getTeam(), getPlayer(), 2);
                                    getPlayer().getMatch().colorSquare(location.getBlock(), getPlayer().getTeam(), getPlayer(), 2);

                                }

                                i++;

                            }

                            if(hitWall != null) {

                                Block block1 = hitWall;
                                for(int x = 0; x < 16; x++) {

                                    block1 = block1.getRelative(BlockFace.DOWN);
                                    if(getPlayer().getMatch().isPaintable(getPlayer().getTeam(), block1.getX(), block1.getY(), block1.getZ())) {

                                        getPlayer().getMatch().paint(getPlayer(), block1.getLocation().toVector(), getPlayer().getTeam());
                                        getPlayer().getMatch().colorSquare(block1, getPlayer().getTeam(), getPlayer(), 1);
                                        getPlayer().getMatch().colorSquare(block1, getPlayer().getTeam(), getPlayer(), 1);

                                    } else { break; }

                                }

                            }

                        });

                    } else {

                        chargeBegin = 0;
                        lastChargeUpdate = 0;

                    }

                }

                if(dealtSlowness) {

                    getPlayer().disableWalkSpeedOverride();
                    dealtSlowness = false;

                }

            } else {

                if(!dealtSlowness && isSelected()) {

                    getPlayer().enableWalkSpeedOverride();
                    dealtSlowness = true;
                    getPlayer().setOverrideWalkSpeed(0.08f);

                }

                if(zoom) {

                    if(!zoomPacketSent && getPlayer() instanceof SplatoonHumanPlayer) {

                        PacketPlayOutAbilities abilities = new PacketPlayOutAbilities(
                                ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle().abilities
                        );
                        ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle().playerConnection.sendPacket(abilities);
                        zoomPacketSent = true;

                    }

                }

                long remainingMillis = chargeTarget - System.currentTimeMillis();

                if(getPlayer().heldItemSlot() == 2) {

                    if(storeCharge) {

                        lastChargeUpdate = System.currentTimeMillis();

                    }

                } else if(isSelected()) {

                    lastChargeUpdate = System.currentTimeMillis();

                }

                boolean fullCharge = remainingMillis <= 1;
                if(!fullCharge) {

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


                        notifySoundTicks++;
                        if(notifySoundTicks > 5) {


                        }

                        getPlayer().sendActionBar(filled + notFilled);
                        if(remainingMillis <= 3) {

                            if(getPlayer() instanceof SplatoonHumanPlayer) {

                                SplatoonHumanPlayer player = (SplatoonHumanPlayer)getPlayer();
                                player.getPlayer().playSound(player.getPlayer().getLocation(),
                                            Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);


                            }

                        }

                    }

                } else {

                    if(storeCharge) {

                        if (!isSelected() && getPlayer().heldItemSlot() == 2) {

                            if (!storeFlag) {

                                storeBeginTime = System.currentTimeMillis();
                                storeFlag = true;
                                if(getPlayer() instanceof SplatoonHumanPlayer) {

                                    ((SplatoonHumanPlayer) getPlayer()).getPlayer().playSound(((SplatoonHumanPlayer) getPlayer()).getPlayer().getLocation(),
                                            Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2f);
                                    ((SplatoonHumanPlayer) getPlayer()).getPlayer().sendActionBar("§7Konzentrator geladen!");

                                }

                            } else {

                                long timeSinceBegin = System.currentTimeMillis() - storeBeginTime;
                                if (timeSinceBegin > maxStore) {

                                    storeBeginTime = 0;
                                    lastChargeUpdate = 0;
                                    storeFlag = false;
                                    getPlayer().sendActionBar("§cKonzentrator entladen.");

                                }

                            }

                        }

                    }

                }

            }

        }

    }

    private int notifySoundTicks = 0;
    private boolean dealtSlowness = false;

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(fullUsage);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.GOLDEN_HOE;
    }

    @Override
    public void shoot() {

    }

    public float getZoomModificator() { return zoomModificator; }
    public boolean hasZoom() { return zoom; }
}
