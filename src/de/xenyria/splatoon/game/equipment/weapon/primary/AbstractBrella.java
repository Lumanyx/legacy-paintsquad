package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.equipment.weapon.util.SprayUtil;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.BrellaModel;
import de.xenyria.splatoon.game.objects.DetachedBrella;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public abstract class AbstractBrella extends SplatoonPrimaryWeapon {

    private final double maxHP;
    private long holdTime, shotDelay, holdStart;
    private int projectiles, damage;

    public double hp;
    private double impulse;
    private double maxSpray;

    public boolean attached = true;

    public AbstractBrella(int id, String name, long holdTime, double brellaHP, long shotDelay, int projectiles, double damage, double impulse, double maxSpray) {

        super(id, name);
        this.maxHP = brellaHP;
        this.hp = brellaHP;
        this.holdTime = holdTime;
        this.shotDelay = shotDelay;
        this.projectiles = projectiles;
        this.damage = (int) damage;
        this.impulse = impulse;
        this.maxSpray = maxSpray;

    }

    public void syncTick() {

        if(!isSelected()) {

            holdBegin = 0;

        }
        if(brellaPunishTicks > 0) {

            brellaPunishTicks--;
            if(brellaPunishTicks < 1) {

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    Player player = ((SplatoonHumanPlayer)getPlayer()).getPlayer();

                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.2f);
                    player.sendActionBar("§7Der Schirm ist wieder einsatzbereit!");

                }

            }

        }


        if(model != null) {

            model.tick();

            if(isHolding() && millisToBeginHold() > 850 && !destructionPunishTicksActive()) {

                if(!model.isActive()) { model.spawn(); }

            } else {

                if(attached && model.isActive()) {

                    model.remove();

                }

            }

            if(model.isActive() && attached) {

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer)getPlayer()).getPlayer().sendActionBar("§6Rechtsklick §egedrückt halten §7um den Schirm zu lösen.");

                }

            } else if(model.isActive() && !attached) {

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer)getPlayer()).getPlayer().sendActionBar("§7Schirm ist abgelöst!");

                }

            }

        }

        if(queuedShots > 0) {

            int toSpawn = queuedShots;
            for(int i = 0; i < toSpawn; i++) {

                for(int y = 0; y < projectiles; y++) {

                    InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
                    projectile.setPaintBelowRatio(2, 2, 1);
                    projectile.withDamage(damage);
                    projectile.spawn(getPlayer().getLocation().clone().add(0, 1, 0),
                            SprayUtil.addSpray(getPlayer().yaw(), (float) maxSpray), getPlayer().pitch(), (float) impulse);
                    getPlayer().getMatch().queueProjectile(projectile);

                }

            }
            queuedShots-=toSpawn;

        }

        if(brellaObject != null) {

            brellaObject.onTick();

        }

    }

    public boolean destructionPunishTicksActive() { return brellaPunishTicks > 0; }
    private long lastShoot;
    public long millisToLastShoot() { return System.currentTimeMillis() - lastShoot; }

    private DetachedBrella brellaObject;
    private int queuedShots = 0;
    private boolean firstShotFlag;
    private long holdBegin;

    private boolean storedDetach = false;
    public boolean hasToDetach() { return storedDetach; }

    private long lastHoldUpdate;
    public boolean isHolding() {

        return (System.currentTimeMillis() - lastHoldUpdate) < 400;

    }

    private boolean resetFlag = false;
    public void asyncTick() {

        if(isSelected() && getPlayer().isShooting()) {

            if(resetFlag) {

                holdBegin = 0;
                if(!getPlayer().isShooting()) {

                    firstShotFlag = false;

                }
                resetFlag = false;

            }

            if(holdBegin == 0) {

                firstShotFlag = false;
                holdBegin = System.currentTimeMillis();

            }

            if(attached) {

                if (!firstShotFlag) {

                    lastShoot = System.currentTimeMillis();
                    queuedShots++;
                    firstShotFlag = true;
                    return;

                }

                long holdBeginMillis = System.currentTimeMillis() - holdBegin;
                if(holdBeginMillis > holdTime) {

                    storedDetach = true;
                    hp = maxHP;

                }

            } else {

                if(millisToLastShoot() > shotDelay) {

                    lastShoot = System.currentTimeMillis();
                    queuedShots++;

                }

            }

            lastHoldUpdate = System.currentTimeMillis();

        } else {

            resetFlag = true;

        }

    }

    private BrellaModel model;

    public void assign(SplatoonPlayer player) {

        super.assign(player);
        model = new BrellaModel(player, getPlayer().getLocation(), this);
        brellaObject = new DetachedBrella(getPlayer(), getPlayer().getMatch(), this);
        getPlayer().getMatch().addGameObject(brellaObject);

    }

    public BrellaModel getModel() {
        return model;
    }

    public void removeBrella() {


        brellaPunishTicks = 60;
        if(getPlayer() instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer)getPlayer()).getPlayer().sendActionBar("§7Der Schirm hat sich aufgelöst!");

        }

        brellaObject.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE,
                brellaObject.getLocation(), 0);
        brellaObject.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE,
                brellaObject.getLocation(), 0);
        if(model.isActive()) {

            model.remove();

        }
        storedDetach = false;
        attached = true;
        if(getPlayer().isShooting()) {

            lastShoot = 0;

        }

    }

    public void registerDetach() {

        storedDetach = false;
        attached = false;

    }

    private int brellaPunishTicks;
    public void resetBrella() {

        if(hp <= 0) {

            brellaPunishTicks = 150;

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                ((SplatoonHumanPlayer)getPlayer()).getPlayer().sendActionBar("§7Der Schirm wurde zerstört!");

            }

            brellaObject.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE,
                    brellaObject.getLocation(), 0);
            brellaObject.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE,
                    brellaObject.getLocation(), 0);

        }

        storedDetach = false;
        attached = true;
        holdBegin = 0;
        hp = maxHP;

    }

    public double getDamage() { return damage; }

    public int millisToBeginHold() { return (int) (System.currentTimeMillis() - holdBegin); }
}
