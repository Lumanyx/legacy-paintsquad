package de.xenyria.splatoon.ai.weapon;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeapon;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponRoller;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponShooter;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.SplatBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EnumItemSlot;
import net.minecraft.server.v1_13_R2.Item;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityEquipment;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

public class AIWeaponManager {

    private EntityNPC npc;
    public AIWeaponManager(EntityNPC npc) {

        this.npc = npc;

    }

    private Trajectory targetTrajectory = null;
    public Trajectory getTargetTrajectory() { return targetTrajectory; }

    public boolean canHitBlock(Location location, Vector target, Block block) {

        SplatoonPrimaryWeapon primaryWeapon = npc.getEquipment().getPrimaryWeapon();
        if(primaryWeapon instanceof AIWeapon) {

            if(primaryWeapon instanceof AIWeaponShooter) {

                AIWeaponShooter shooter = (AIWeaponShooter)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(location, target.toLocation(npc.getWorld()), shooter.getImpulse(), npc.getMatch(), npc.getTeam(), block, npc);
                if(result.isTargetReached() || (result.getHitLocation() != null && result.getHitLocation().toVector().distance(target) <= 0.51)) {

                    return true;

                }

                return false;

            } else if(primaryWeapon instanceof AIWeaponRoller) {

                AIWeaponRoller roller = (AIWeaponRoller)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(location, target.toLocation(npc.getWorld()), roller.getImpulse(), npc.getMatch(), npc.getTeam(), block, npc);
                if(result.isTargetReached() || (result.getHitLocation() != null && result.getHitLocation().toVector().distance(target) <= 0.51)) {

                    return true;

                }

                return false;

            }

        }

        return false;

    }

    public boolean canHitEntity(Location positionA, Location positionB, HitableEntity entity) {

        SplatoonPrimaryWeapon primaryWeapon = npc.getEquipment().getPrimaryWeapon();
        if(primaryWeapon instanceof AIWeapon) {

            if(primaryWeapon instanceof AIWeaponShooter) {

                AIWeaponShooter shooter = (AIWeaponShooter)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(positionA, positionB, shooter.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                if(result.isTargetReached() || (result.getHitEntity() != null && result.getHitEntity() == entity)) {

                    return true;

                }

                return false;

            } else if(primaryWeapon instanceof AIWeaponRoller) {

                AIWeaponRoller roller = (AIWeaponRoller)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(positionA, positionB, roller.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                if(result.isTargetReached() || (result.getHitEntity() != null && result.getHitEntity() == entity)) {

                    return true;

                }

                return false;

            }

        }

        return false;

    }



    public ProjectileExaminer.Result examineProjectile(Vector start, Vector end) {

        SplatoonPrimaryWeapon primaryWeapon = npc.getEquipment().getPrimaryWeapon();
        if(primaryWeapon instanceof AIWeapon) {

            if(primaryWeapon instanceof AIWeaponShooter) {

                AIWeaponShooter shooter = (AIWeaponShooter)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(start.toLocation(npc.getWorld()), end.toLocation(npc.getWorld()), shooter.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                return result;

            } else if(primaryWeapon instanceof AIWeaponRoller) {

                AIWeaponRoller roller = (AIWeaponRoller)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(start.toLocation(npc.getWorld()), end.toLocation(npc.getWorld()), roller.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                return result;

            }

        }

        return null;

    }

    public boolean canHit(Location positionA, Location positionB) {

        SplatoonPrimaryWeapon primaryWeapon = npc.getEquipment().getPrimaryWeapon();
        if(primaryWeapon instanceof AIWeapon) {

            if(primaryWeapon instanceof AIWeaponShooter) {

                AIWeaponShooter shooter = (AIWeaponShooter)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(positionA, positionB, shooter.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                if(result.isTargetReached()) {

                    return true;

                }

                return false;

            } else if(primaryWeapon instanceof AIWeaponRoller) {

                AIWeaponRoller roller = (AIWeaponRoller)primaryWeapon;
                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(positionA, positionB, roller.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                if(result.isTargetReached()) {

                    return true;

                }

                return false;

            }

        }

        return false;

    }

    public boolean getCurrentHandBoolean() {

        // false->right
        return false;

    }

    // TODO
    public double maxWeaponDistance() {

        AIPrimaryWeaponType type = getAIPrimaryWeaponType();
        if(type == AIPrimaryWeaponType.SHOOTER) {

            return 6;

        } else if(type == AIPrimaryWeaponType.ROLLER) {

            return 3;

        }
        return 0d;

    }

    private int shootTicks;
    public void fire(int ticks) {

        shootTicks=ticks;

    }

    public boolean enoughInkToShootPrimaryWeapon() {

        return (npc.getInk() >= (npc.getEquipment().getPrimaryWeapon().getNextInkUsage() + 4d));

    }

    private Location aimLocation;

    public static final double AIM_MAX_ACCURACY = 0.1d;
    public static final double AIM_MIN_ACCURACY = 0.4d;

    public void aim(Vector target) {

        targetBlock = null;
        aimLocation = target.toLocation(npc.getWorld());

    }

    public void aim(Block block) {

        if(block != null) {

            aimLocation = block.getLocation().add(.5, .5, .5);
            targetBlock = block;

        } else {

            aimLocation = null;
            targetBlock = null;

        }

    }

    public void aim(SplatoonPlayer enemy) {

        targetBlock = null;
        double offsetRange = (AIM_MIN_ACCURACY - AIM_MAX_ACCURACY) * (npc.getProperties().getAccuracy() / 100d);

        double minHeight = enemy.getHeight() * .25;
        double maxHeight = enemy.getHeight() * .625;
        double heightOffset = (maxHeight-minHeight) * new Random().nextFloat();

        Vector offset = new Vector(new Random().nextDouble() * offsetRange, minHeight + (new Random().nextDouble() * heightOffset), new Random().nextDouble() * offsetRange);
        aimLocation = enemy.centeredHeightVector().toLocation(npc.getWorld()).add(offset);

    }

    public void tick() {

        if(shootTicks > 0 && aimLocation != null) {

            if(!ItemBuilder.hasValue(npc.getCurrentItemInMainHand(), "WeaponID")) {

                npc.updateCurrentItemInMainHand(npc.getEquipment().getPrimaryWeapon().asItemStack());

            } else {

                int id = ItemBuilder.getIntValue(npc.getCurrentItemInMainHand(), "WeaponID");
                if(id != npc.getEquipment().getPrimaryWeapon().getID()) {

                    npc.updateCurrentItemInMainHand(npc.getEquipment().getPrimaryWeapon().asItemStack());

                }

            }

            npc.setShooting(true);
            shootTicks--;

            Vector start = npc.getEyeLocation().toVector();
            Vector target = aimLocation.toVector();
            Vector direction = target.clone().subtract(start).normalize();
            if(VectorUtil.isValid(direction)) {

                Location location = new Location(npc.getWorld(), 0,0,0);
                location.setDirection(direction);

                float targetYaw = location.getYaw();
                float targetPitch = location.getPitch();

                // Random Offsets miteinberechnen
                AIWeapon weapon = getAIWeaponInterface();
                if(weapon != null) {

                    if(weapon instanceof AIWeaponShooter) {

                        AIWeaponShooter shooter = (AIWeaponShooter) weapon;
                        double nextOffsetYaw = shooter.nextSprayYaw();
                        double nextOffsetPitch = shooter.nextSprayPitch();
                        double ratio = npc.getProperties().getWeaponHandling() / 100d;
                        nextOffsetYaw*=ratio;
                        nextOffsetPitch*=ratio;
                        targetYaw+=nextOffsetYaw;
                        targetPitch+=nextOffsetPitch;
                        npc.updateAngles(targetYaw, targetPitch);

                    } else if(weapon instanceof AIWeaponRoller) {

                        targetYaw = npc.getLocation().getYaw();

                        AIWeaponRoller roller = (AIWeaponRoller) weapon;
                        double offsetPitch = roller.getPitchOffset();
                        boolean negative = offsetPitch < 0;

                        double minOffset = Math.abs(offsetPitch)*.7;
                        double maxOffset = Math.abs(offsetPitch)*1.05;

                        double offsetRange = maxOffset-minOffset;
                        double finalOffset = minOffset+(offsetRange * new Random().nextDouble());

                        if(negative) {

                            npc.updateAngles(targetYaw, targetPitch - (float)finalOffset);

                        } else {

                            npc.updateAngles(targetYaw, targetPitch + (float)finalOffset);

                        }

                    }

                }

            }

        } else {

            npc.setShooting(false);

        }

    }

    public AIWeapon getAIWeaponInterface() {

        if(npc.getEquipment().getPrimaryWeapon() instanceof AIWeapon) {

            return (AIWeapon) npc.getEquipment().getPrimaryWeapon();

        } else {

            return null;

        }

    }

    private Block targetBlock = null;
    public Location getAimLocation() { return aimLocation; }

    public int paintableBlocksPerShoot() {

        AIPrimaryWeaponType type = getAIPrimaryWeaponType();
        if(type == AIPrimaryWeaponType.SHOOTER) {

            return 8;

        }

        return 4;

    }

    public void reset() {

        aimLocation = null;
        targetBlock = null;
        shootTicks = 0;

    }

    public void resetAim() {

        aimLocation = null;
        targetBlock = null;
        shootTicks = 0;
        npc.setShooting(false);

    }

    public int getShootingTicks() { return shootTicks; }


    public class TrajectoryTargetPair {

        public Trajectory trajectory;
        public Location target;
        public TrajectoryTargetPair(Trajectory trajectory, Location target) {

            this.trajectory = trajectory;
            this.target = target;

        }

    }


    public TrajectoryTargetPair trajectoryToTarget(float impulse, float yawOffset) {

        if(aimLocation != null) {

            Location start = npc.getShootingLocation(npc.getWeaponManager().getCurrentHandBoolean());
            Location end = aimLocation;

            Vector direction = end.toVector().subtract(start.toVector()).clone();
            Location location = new Location(npc.getWorld(), 0,0,0);
            location.setDirection(direction);
            location.setYaw(location.getYaw() + yawOffset);

            Vector newTarget = start.toVector().add(location.getDirection().clone().multiply(start.distance(end)));

            ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(start,
                    newTarget.toLocation(npc.getWorld()), impulse, npc.getMatch(), npc.getTeam(), targetBlock, npc);
            if(result != null && result.isTargetReached()) {

                if(targetBlock != null) {

                    return new TrajectoryTargetPair(result.getTrajectory(), targetBlock.getLocation());

                } else {

                    return new TrajectoryTargetPair(result.getTrajectory(), result.getHitLocation());

                }

            }

        }
        return null;

    }

    public TrajectoryTargetPair trajectoryToTarget(double impulse) {

        if(aimLocation != null) {

            ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(npc.getShootingLocation(npc.getWeaponManager().getCurrentHandBoolean()),
                    aimLocation, impulse, npc.getMatch(), npc.getTeam(), targetBlock, npc);
            if(result != null && result.getTrajectory() != null) {

                if(targetBlock != null) {

                    return new TrajectoryTargetPair(result.getTrajectory(), targetBlock.getLocation());

                } else {

                    return new TrajectoryTargetPair(result.getTrajectory(), result.getHitLocation());

                }

            }
        }
        return null;

    }

    public static enum AIPrimaryWeaponType {

        SHOOTER, ROLLER;

    }

    public static enum AISecondaryWeaponType {

        PAINTBOMB,DAMAGEBOMB,BEACON,SPRINKLER;

    }

    public AISecondaryWeaponType getAISecondaryWeaponType() {

        if(npc.getEquipment().getSecondaryWeapon() instanceof SplatBomb) {

            return AISecondaryWeaponType.DAMAGEBOMB;

        }
        return null;

    }

    public AIPrimaryWeaponType getAIPrimaryWeaponType() {

        if(npc.getEquipment().getPrimaryWeapon() instanceof AbstractSplattershot) {

            return AIPrimaryWeaponType.SHOOTER;

        } else if(npc.getEquipment().getPrimaryWeapon() instanceof AbstractRoller) {

            return AIPrimaryWeaponType.ROLLER;

        }
        return null;

    }

}
