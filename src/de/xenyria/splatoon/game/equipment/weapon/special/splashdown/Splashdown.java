package de.xenyria.splatoon.game.equipment.weapon.special.splashdown;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

public class Splashdown extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public static final int ID = 10;
    public Splashdown() {
        super(ID, "Tintenschock", "Führt eine mächtige Stampfattacke aus.", 190);
    }

    private boolean doneFlag = true;

    @Override
    public boolean isActive() {
        return !doneFlag;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    private boolean appliedVelocity = false;
    private int downTicker = 0;
    private int impactTicker = 0;
    private boolean waitForImpact = false;
    private float radius = 8f;
    private Location useLoc;

    @Override
    public void syncTick() {

        if(isActive()) {

            if(getPlayer().isSplatted()) {

                waitForImpact = false;
                downTicker = 0;
                appliedVelocity = false;
                impactTicker = 0;
                doneFlag = true;
                useLoc = null;
                if(getPlayer() instanceof EntityNPC) { ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd(); }
                getPlayer().disableWalkSpeedOverride();

                return;

            }

            if (!appliedVelocity) {

                getPlayer().enableWalkSpeedOverride();
                getPlayer().setOverrideWalkSpeed(0f);
                getPlayer().setVelocity(new Vector(0, 1.5, 0));
                downTicker = 20;
                appliedVelocity = true;

                if(useLoc == null) {

                    if(getPlayer().isOnGround()) {

                        useLoc = getPlayer().getLocation();

                    } else {

                        Location location = getPlayer().getLocation();
                        for(int i = 0; i < 30; i++) {

                            location = location.add(0, -1, 0);
                            if(location.getBlock().getType().isSolid()) {

                                useLoc = location.clone().add(0, 1, 0);
                                break;

                            }

                        }
                        if(useLoc == null) {

                            useLoc = location;

                        }

                    }

                }

            }

            if (downTicker > 0) {

                for(float yaw = 0f; yaw < 360; yaw+=10f) {

                    Location dummy = useLoc.clone();
                    dummy.setPitch(0f);
                    dummy.setYaw(yaw);
                    Vector dir = dummy.getDirection();
                    Location particle = dummy.clone().add(dir.clone().multiply(radius));
                    SplatoonServer.broadcastColorParticle(getPlayer().getWorld(),
                            particle.getX(), particle.getY(), particle.getZ(), getPlayer().getTeam().getColor(), 1f);

                }

                downTicker--;
                if(downTicker < 1) {

                    getPlayer().setVelocity(new Vector(0, -2.5, 0));
                    waitForImpact = true;

                }

            }

            if(waitForImpact && getPlayer() instanceof EntityNPC) {

                EntityNPC npc = (EntityNPC) getPlayer();
                npc.move(0, -1d, 0);

            }

            Location xzCur = getPlayer().getLocation().clone();
            xzCur.setY(0);
            Location xzTar = useLoc.clone();
            xzTar.setY(0);
            double dist = xzCur.distance(xzTar);
            if(dist > 0.05) {

                Vector velo = getPlayer().getVelocity().clone();
                Vector newDir = xzTar.toVector().subtract(xzCur.toVector()).multiply(.25);
                newDir = velo.setX(newDir.getX()).setZ(newDir.getZ());
                getPlayer().setVelocity(newDir);

            }

            if(waitForImpact) {

                impactTicker++;

                if(getPlayer().isOnGround()) {

                    BombProjectile projectile = new BombProjectile(getPlayer(), this, getPlayer().getMatch(), radius, 0, 180, false);
                    projectile.spawn(0, getPlayer().getLocation());
                    getPlayer().getWorld().playSound(getPlayer().getLocation(),
                            Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.2f);
                    appliedVelocity = false;
                    doneFlag = true;
                    waitForImpact = false;
                    downTicker = 0;
                    impactTicker = 0;
                    useLoc = null;
                    if(getPlayer() instanceof EntityNPC) { ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd(); }
                    getPlayer().disableWalkSpeedOverride();

                } else {

                    if(impactTicker > 200) {

                        end();
                        return;

                    }

                }

            }

        }

    }

    public void end() {

        appliedVelocity = false;
        doneFlag = true;
        waitForImpact = false;
        downTicker = 0;
        impactTicker = 0;
        useLoc = null;
        getPlayer().disableWalkSpeedOverride();

    }

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting() && getPlayer().getSpecialPoints() >= getRequiredPoints() && !isActive() && isSelected() && getPlayer().hasControl()) {

            activate();

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
        return Material.TNT;
    }

    @Override
    public void shoot() {

    }

    public void cleanUp() {

        if(!doneFlag) {

            appliedVelocity = false;
            doneFlag = true;
            waitForImpact = false;
            downTicker = 0;
            impactTicker = 0;
            useLoc = null;
            getPlayer().disableWalkSpeedOverride();

        }

    }

    @Override
    public void activate() {

        getPlayer().addInk(100d);
        getPlayer().resetSpecialGauge();
        doneFlag = false;
        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7setzt den §eTintenschock §7ein!");

    }

}
