package de.xenyria.splatoon.game.equipment.weapon.special.stingray;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.StingRayModel;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.scoreboard.EntityHighlightController;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.function.Predicate;

public class StingRay extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public static final int ID = 9;

    public StingRay() {

        super(ID, "Hochdruckverunreiniger", "Steuere einen Tintenstrahl welcher Gegner\ndurch mehrere Wände hinweg ausschaltet.", 200);

    }

    public boolean modelVisible() { return model.isActive(); }

    private int firingTicks = 0;
    private StingRayModel model;

    @Override
    public void assign(SplatoonPlayer player) {

        super.assign(player);
        model = new StingRayModel(getPlayer(), getPlayer().getLocation(), this);

    }

    @Override
    public boolean isActive() {
        return firingTicks > 0;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {

        if(getPlayer().isSplatted() && isActive()) {

            if(model.isActive()) { model.remove(); }
            firingTicks = 0;
            getPlayer().disableWalkSpeedOverride();
            return;

        }

        if(!isSelected() && model.isActive()) { model.remove(); }

        if(isActive() && getPlayer() instanceof SplatoonHumanPlayer) {

            SplatoonHumanPlayer player = ((SplatoonHumanPlayer)getPlayer());

            int slot = getPlayer().heldItemSlot();
            if(slot < 2) {

                player.getPlayer().getInventory().setHeldItemSlot(2);

            } else if(slot > 3) {

                player.getPlayer().getInventory().setHeldItemSlot(3);

            }

        }

        if(firingTicks > 0) {

            if(isSelected() && getPlayer().isShooting()) {

                float remSecs = ((float) firingTicks / 20f);
                DecimalFormat format = new DecimalFormat("#.#");
                String seconds = format.format(remSecs);
                getPlayer().sendActionBar("§e" + seconds + " Sekunde(n) §7verbleiben.");

            } else if(isSelected() && !getPlayer().isShooting()) {

                model.resetPosition();
                getPlayer().sendActionBar("§6§lRechtsklick §7zum abfeuern des H.-d.-verunreiniger");

            }

            firingTicks--;
            if(isSelected()) {

                if(!model.isActive()) { model.spawn(); }
                model.tick();

                World world = getPlayer().getWorld();
                Location location = model.noozleLocation();
                float highlightDistance = 96;
                Location hitPosition = location.clone().add(getPlayer().getEyeLocation().getDirection().clone().multiply(highlightDistance));

                if(getPlayer().isShooting()) {

                    //if (result != null && result.getHitEntity() != null) {

                        //highlightDistance = (float) location.toVector().distance(result.getHitPosition());

                        RayProjectile projectile = new RayProjectile(getPlayer(), this, getPlayer().getMatch(),
                                new Location(getPlayer().getWorld(), location.getX(),
                                        location.getY(), location.getZ()), location.getDirection(), 12f);

                        HitableEntity entity = projectile.getHitEntity(96d, new Predicate<HitableEntity>() {
                            @Override
                            public boolean test(HitableEntity hitableEntity) {

                                boolean friendlyFire = (hitableEntity instanceof SplatoonPlayer && ((SplatoonPlayer) hitableEntity).getTeam().equals(getPlayer().getTeam()));
                                return !friendlyFire;

                            }
                        }, false, false);
                        if(entity != null) {

                            entity.onProjectileHit(projectile);

                            if(entity instanceof TentaMissleTarget && getPlayer() instanceof SplatoonHumanPlayer) {

                                SplatoonHumanPlayer player = ((SplatoonHumanPlayer)getPlayer());

                                TentaMissleTarget target = (TentaMissleTarget) entity;
                                EntityHighlightController.TeamEntry entry = player.getHighlightController().getEntry(entity.getEntityID());
                                if (entry != null) {

                                    if (entry.ticks < 20) {

                                        entry.ticks += 20;

                                    }

                                } else {

                                    ChatColor chatColor = ChatColor.DARK_GRAY;
                                    if (target.getTeam() != null) {

                                        chatColor = ChatColor.getByChar(target.getTeam().getColor().getColor());

                                    }

                                    player.getHighlightController().addEntry(new EntityHighlightController.TeamEntry(target.getName(),
                                            chatColor, target.getNMSEntity(), 20));

                                }

                            }

                        }

                    //}

                    Vector dir = location.getDirection();
                    Location start = location.clone();
                    for (double y = 0; y < highlightDistance; y += .25) {

                        start = start.add(dir);
                        SplatoonServer.broadcastColorParticle(getPlayer().getWorld(),
                                start.getX(), start.getY(), start.getZ(), getPlayer().getTeam().getColor(), 0.5f);

                        if (RandomUtil.random(4) && getPlayer().getMatch().isPaintable(getPlayer().getTeam(), (int)start.getX(), (int)start.getY(), (int)start.getZ())) {

                            getPlayer().getMatch().paint(getPlayer(), start.toVector(), getPlayer().getTeam());

                        }

                    }

                }

            } else {

                if(model.isActive()) { model.remove(); }
                if(setWalkSpeedOverride) {

                    getPlayer().disableWalkSpeedOverride();
                    setWalkSpeedOverride = false;

                }

            }

            if(firingTicks < 1 || getPlayer().isSplatted()) {

                firingTicks = 0;
                if(model.isActive()) { model.remove(); }
                if(setWalkSpeedOverride) {

                    getPlayer().disableWalkSpeedOverride();
                    setWalkSpeedOverride = false;

                }
                if(getPlayer() instanceof EntityNPC) {

                    ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd();

                }

            }

        } else {

            if(model.isActive()) { model.remove(); }

        }

    }

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting() && isSelected() && !isActive() && getPlayer().hasControl()) {

            if (getPlayer().getSpecialPoints() >= getRequiredPoints()) {

                activateCall();

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
        return Material.LEVER;
    }

    @Override
    public void shoot() {

    }

    public void cleanUp() {

        if(model != null) {

            model.removeForcefully();

        }
        firingTicks = 0;

    }

    public void activateCall() {

        getPlayer().enableWalkSpeedOverride();
        setWalkSpeedOverride = true;
        getPlayer().setOverrideWalkSpeed(0.1f);
        getPlayer().resetSpecialGauge();
        firingTicks = 170;
        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7aktiviert den §eHochdruckverunreiniger§7!");

    }
    private boolean setWalkSpeedOverride = false;

    @Override
    public void activate() {

        activateCall();
        ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponBegin();

    }

    public Location noozleLocation() { return model.noozleLocation(); }

}
