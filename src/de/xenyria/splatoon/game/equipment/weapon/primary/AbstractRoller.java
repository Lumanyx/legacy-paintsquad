package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponRoller;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.RollerWeaponModel;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.WeaponModel;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.projectile.RollerVectorHurtPoint;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public abstract class AbstractRoller extends SplatoonPrimaryWeapon implements AIWeaponRoller {

    private float[] nextSprayYawValues, nextSprayPitchValues;
    public float[] nextSprayYaw() { return nextSprayYawValues; }
    public float[] nextSprayPitch() { return nextSprayPitchValues; }
    public float getImpulse() { return impulse; }

    public float peakRollSpeed, minRollSpeed, thickness, rollUsage, splatUsage, impactDamage, splatDamage;

    public AbstractRoller(int id, String name, float peakRollSpeed, float minRollSpeed, float thickness, float rollUsage, float splatUsage, float impactDamage, float splatDamage) {

        super(id, name);
        this.peakRollSpeed = peakRollSpeed;
        this.minRollSpeed = minRollSpeed;
        this.thickness = thickness;
        this.rollUsage = rollUsage;
        this.splatUsage = splatUsage;
        this.impactDamage = impactDamage;
        this.splatDamage = splatDamage;

        nextSprayPitchValues = new float[projectileCount()];
        nextSprayYawValues = new float[projectileCount()];

    }

    @Override
    public void assign(SplatoonPlayer player) {

        super.assign(player);
        createModel();

    }

    public void createModel() {

        model = new RollerWeaponModel(getPlayer(), "roller", getPlayer().getWorld(), getPlayer().getLocation());

    }

    private WeaponModel model;
    public void setModel(WeaponModel model) { this.model = model; }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.ROLLER;
    }

    public int projectileCount() {

        int i = 0;
        float range = 50f;
        for(float f = 0; f < range; f+=10f) {

            i++;

        }
        return i;

    }

    public float impulse = 0.3f;

    public final float ROLLER_PITCH_OFFSET = 10f;

    public float getPitchOffset() { return ROLLER_PITCH_OFFSET; }

    public float splatRange = 50f;

    public void calculateAIRelevantInformations() {

        float range = splatRange;
        float beginYaw = getPlayer().yaw() - (range / 2);
        int i = 0;

        for(float f = 0; f < range; f+=10f) {

            float endYaw = beginYaw+f;
            float pitch = getPlayer().pitch() - ROLLER_PITCH_OFFSET;
            nextSprayYawValues[i] = endYaw;
            nextSprayPitchValues[i] = pitch;
            i++;

        }

    }

    public void fireSplat(Trajectory[] trajectory, Location[] plannedHitLocation) {

        splatPhase = false;

        float range = splatRange;
        float beginYaw = getPlayer().yaw() - (range / 2);
        int i = 0;
        for(float f = 0; f < range; f+=10f) {

            float endYaw = beginYaw+f;

            InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.withDamage(splatDamage);
            projectile.withReason(DamageReason.WEAPON);
            projectile.setDrippingRatio(2);
            if(trajectory == null || trajectory[i] == null) {

                projectile.spawn(getPlayer().getEyeLocation(), endYaw, getPlayer().pitch() - ROLLER_PITCH_OFFSET, impulse);

            } else {

                EntityNPC npc = (EntityNPC) getPlayer();
                projectile.spawn(trajectory[i], npc.getShootingLocation(false), plannedHitLocation[i]);

            }
            i++;

        }

        rolling = true;
        rollTicks = 0;
        getPlayer().enableWalkSpeedOverride();
        getPlayer().setOverrideWalkSpeed(minRollSpeed);
        getPlayer().getLocation().getWorld().playSound(getPlayer().getLocation(), Sound.BLOCK_WOOD_BREAK, 2f, 2f);

    }

    @Override
    public void syncTick() {

        calculateAIRelevantInformations();
        model.tick();

        if(rollTicks > 0) {

            if (getPlayer().isSquid() || getPlayer().isSplatted()) {

                splatTicks = ticksToSplat;
                splatPhase = false;
                rolling = false;
                if (getPlayer() instanceof SplatoonHumanPlayer) {
                    ((SplatoonHumanPlayer) getPlayer()).updateInventory();
                } else {

                    ((EntityNPC) getPlayer()).updateInventory();

                }
                rollTicks = 0;

                if (model.isActive()) {

                    model.remove();

                }

            }

        }

        if(splatPhase) {

            splatTicks--;
            if(splatTicks < 1) {

                splatPhase = false;

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    // Splatprojectiles
                    fireSplat(null, null);
                    SplatoonHumanPlayer player = (SplatoonHumanPlayer)getPlayer();
                    Player player1 = player.getPlayer();
                    player1.getInventory().setItemInMainHand(asItemStack());

                } else {

                    EntityNPC npc = (EntityNPC) getPlayer();
                    float range = splatRange;
                    Trajectory[] trajectories = new Trajectory[projectileCount()];
                    Location[] hitLocations = new Location[projectileCount()];

                    int i = 0;
                    for(float f = 0; f < range; f+=10f) {

                        AIWeaponManager.TrajectoryTargetPair trajectory = npc.getWeaponManager().trajectoryToTarget(impulse, (-(f/2)) + f);

                        if(trajectory != null && trajectory.trajectory != null) {

                            trajectories[i] = trajectory.trajectory;
                            hitLocations[i] = trajectory.target;

                        }

                        i++;

                    }
                    fireSplat(trajectories, hitLocations);
                    npc.updateInventory();

                }

            }

        } else if(rolling) {

            rollTicks++;
            if((!isSelected() || getPlayer().isSplatted()) && model.isActive()) {

                model.remove();
                return;

            }

            int maxTimeTillFullSpeed = 200;
            double factor = 0d;
            if(rollTicks > maxTimeTillFullSpeed) { factor = 1d; } else {

                factor = ((float)rollTicks / (float)maxTimeTillFullSpeed);

            }
            getPlayer().setOverrideWalkSpeed((float) (minRollSpeed + (peakRollSpeed * factor)));

            if(!getPlayer().hasEnoughInk(rollUsage)) {

                getPlayer().notEnoughInk();
                rolling = false;
                if(getPlayer() instanceof SplatoonHumanPlayer) { ((SplatoonHumanPlayer)getPlayer()).updateInventory(); } else {

                    ((EntityNPC)getPlayer()).updateInventory();

                }
                getPlayer().disableWalkSpeedOverride();

            } else {

                if(getPlayer().isShooting()) {

                    getPlayer().removeInk(rollUsage);
                    Vector frontVector = getPlayer().getLocation().toVector();
                    frontVector = frontVector.add(new Vector(0, .25, 0));

                    Location plyrClone = getPlayer().getLocation().clone();
                    plyrClone.setPitch(0f);
                    Vector dir = plyrClone.getDirection();
                    frontVector.add(dir);

                    float halfThickness = thickness / 2;

                    plyrClone.setYaw(plyrClone.getYaw() + 90f);

                    Vector edgeVec = frontVector.clone().add(plyrClone.getDirection().multiply(halfThickness));
                    Vector startVec = edgeVec.clone();
                    ArrayList<Vector> hurtVectors = new ArrayList<>();

                    for (float i = 0; i < (thickness * 2); i++) {

                        //if(edgeVec.distance(startVec) <= thickness) {

                        Block block = getPlayer().getWorld().getBlockAt(
                                (int) edgeVec.getBlockX(), (int) edgeVec.getBlockY(), (int) edgeVec.getBlockZ()
                        );

                        Block below = block.getRelative(BlockFace.DOWN);

                        if (getPlayer().getMatch().isPaintable(getPlayer().getTeam(), below)) {

                            getPlayer().getMatch().paint(getPlayer(), new Vector(below.getX(), below.getY(), below.getZ()), getPlayer().getTeam());

                        } else if (getPlayer().getMatch().isPaintable(getPlayer().getTeam(), block)) {

                            getPlayer().getMatch().paint(getPlayer(), new Vector(block.getX(), block.getY(), block.getZ()), getPlayer().getTeam());

                        }
                        edgeVec.add(plyrClone.getDirection().clone().multiply(-0.35));
                        hurtVectors.add(edgeVec.clone());

                        //}

                    }



                    for(Vector hurtVec : hurtVectors) {

                        Location location = getPlayer().getLocation().clone();
                        location = location.set(hurtVec.getX(), hurtVec.getY(), hurtVec.getZ());
                        RollerVectorHurtPoint vecH = new RollerVectorHurtPoint(getPlayer(), this, getPlayer().getMatch(), location, impactDamage);
                        for(HitableEntity entity : getPlayer().getMatch().getHitableEntities()) {

                            if(entity.isHit(vecH)) {

                                entity.onProjectileHit(vecH);
                                if(!entity.isDead()) {

                                    Vector vector = getPlayer().getVelocity();

                                    Vector current = getPlayer().getLocation().toVector();
                                    Vector target = entity.getLocation().toVector();
                                    current.setY(0); target.setY(0);
                                    Vector direction = target.subtract(current).normalize();

                                    vector = vector.add(direction.multiply(-1));
                                    getPlayer().setVelocity(vector);

                                }

                            }

                        }

                    }

                } else {

                    rolling = false;
                    if(getPlayer() instanceof SplatoonHumanPlayer) { ((SplatoonHumanPlayer)getPlayer()).updateInventory(); } else {

                        ((EntityNPC)getPlayer()).updateInventory();

                    }
                    getPlayer().disableWalkSpeedOverride();

                }

            }

        }

    }

    private int ticksToSplat = 8;
    public int getTicksToSplat() { return ticksToSplat; }

    public int getTicksToRoll() {

        return ticksToSplat * 3;

    }

    @Override
    public void asyncTick() {

        if(isSelected() && getPlayer().isShooting()) {

            if(!rolling && !splatPhase) {

                if (getPlayer().hasEnoughInk(splatUsage)) {

                    getPlayer().removeInk(splatUsage);
                    splatPhase = true;
                    splatTicks = ticksToSplat;

                }

            }

        }

    }

    public boolean splatPhase = false;
    public int splatTicks = 0;
    public int rollTicks = 0;


    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.IRON_SHOVEL;
    }

    @Override
    public void shoot() {

    }

    public boolean rolling = false;
    public boolean isRolling() {

        return rolling;

    }

}
