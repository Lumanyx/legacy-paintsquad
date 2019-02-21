package de.xenyria.splatoon.ai.task.special;

import de.xenyria.core.math.AngleUtil;
import de.xenyria.core.math.SlowRotation;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.NavigationPoint;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.armor.InkArmor;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.bombrush.AbstractBombRush;
import de.xenyria.splatoon.game.equipment.weapon.special.inkstorm.InkStorm;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;
import de.xenyria.splatoon.game.equipment.weapon.special.stingray.StingRay;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissles;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.StingRayModel;
import de.xenyria.splatoon.game.objects.inkstorm.InkCloud;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.Navigation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class SpecialWeaponManager {

    private EntityNPC npc;

    public SpecialWeaponManager(EntityNPC npc) {

        this.npc = npc;

    }

    private boolean lastTickStatus = false;
    private int targetSwitchTicks = 0;

    private Location inkJetTarget = null;

    public Location getInkJetTarget() {
        return inkJetTarget;
    }

    public void reset() {

        lastTickStatus = false;
        inkJetTarget = null;
        targetSwitchTicks = 0;

    }
    private int rushCheckTicker = 0;

    public void tick() {

        rushCheckTicker++;
        boolean useChance = RandomUtil.random(((int) (npc.getProperties().getAggressiveness() / 10)) / 4);

        SplatoonSpecialWeapon weapon = npc.getEquipment().getSpecialWeapon();
        if (npc.hasControl() && !npc.isSplatted() && npc.isSpecialReady() && npc.getEquipment().getSpecialWeapon() != null && !weapon.isActive()) {

            if (weapon != null && npc.isSpecialReady()) {

                if (weapon instanceof Jetpack) {

                    if (!weapon.isActive()) {

                        // Begründung um die Waffe einzusetzen
                        if (npc.getTargetManager().getTarget() != null && useChance) {

                            AISpecialWeapon specialWeapon = (AISpecialWeapon) weapon;
                            specialWeapon.activate();

                        }

                    }

                } else if(weapon instanceof Splashdown) {

                    if(!npc.getTargetManager().nearbyThreats(npc.getLocation().toVector(), 1.9).isEmpty()) {

                        AISpecialWeapon specialWeapon = (AISpecialWeapon) weapon;
                        specialWeapon.activate();

                    }

                } else if(weapon instanceof InkArmor) {

                    if(!npc.getTargetManager().nearbyThreats(npc.getLocation().toVector(), 8).isEmpty()) {

                        AISpecialWeapon specialWeapon = (AISpecialWeapon) weapon;
                        specialWeapon.activate();

                    }

                } else if(weapon instanceof AbstractBombRush) {

                    if(rushCheckTicker > 8) {

                        rushCheckTicker = 0;
                        boolean unpaintedNearby = false;
                        int lowCoverageCount = 0;
                        for (PaintableRegion region : npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), 10d)) {

                            if (region.coverage(npc.getTeam()) <= 20d) {

                                lowCoverageCount++;

                            }

                        }

                        unpaintedNearby = lowCoverageCount > 3;

                        if (npc.getTargetManager().hasPotentialTarget() || unpaintedNearby) {

                            AISpecialWeapon specialWeapon = (AISpecialWeapon) weapon;
                            specialWeapon.activate();

                        }

                    }

                } else if(weapon instanceof TentaMissles) {

                    if(rushCheckTicker > 8) {

                        rushCheckTicker = 0;
                        TentaMissles missles = (TentaMissles) weapon;
                        int maxTargets = npc.getMatch().getMaxEnemies(npc);
                        int minTargets = 1;
                        minTargets = (maxTargets / 2);
                        if (minTargets < 1) { minTargets = 1; }

                        if (missles.getTargetsInSight().size() >= minTargets) {

                            ((TentaMissles) weapon).activate();

                        }

                    }

                } else if(weapon instanceof StingRay) {

                    if(!npc.getTargetManager().hasTarget()) {

                        if (!npc.getTargetManager().getPossibleTargets().isEmpty()) {

                            npc.getTargetManager().target(npc.getTargetManager().getPossibleTargets().get(0).target);

                        }

                    }
                    TargetManager.Target target = npc.getTargetManager().getTarget();
                    if(target != null) {

                        Vector direction = target.getEnemy().getLocation().toVector().subtract(npc.getEyeLocation().toVector()).normalize();
                        if(VectorUtil.isValid(direction)) {

                            float[] floats = DirectionUtil.directionToYawPitch(direction);
                            npc.updateAngles(floats[0], floats[1]);
                            ((StingRay) weapon).activate();

                        }

                    }

                } else if(weapon instanceof Baller) {

                    if(npc.getTargetManager().getTarget() != null) {

                        ((Baller) weapon).activate();

                    }

                } else if(weapon instanceof InkStorm) {

                    float baseYaw = npc.yaw();
                    float[] offsets = new float[]{baseYaw-90f, baseYaw-45f, baseYaw+0f, baseYaw+45f, baseYaw+90f};

                    ArrayList<Float> possibleOffsets = new ArrayList<>();
                    for(float offset : offsets) {

                        ArrayList<PaintableRegion> regions = new ArrayList<>();
                        Vector direction = DirectionUtil.yawAndPitchToDirection(offset, 0f);
                        Vector cursor = npc.getLocation().toVector();
                        for(int i = 0; i < 15; i++) {

                            cursor = cursor.add(direction);
                            PaintableRegion region = (npc.getMatch().getAIController().getPaintableRegion(PaintableRegion.Coordinate.fromWorldCoordinates(
                                    (int)cursor.getX(), (int)cursor.getY(), (int)cursor.getZ()
                            )));

                            if(region != null) {

                                if(region.coverage(npc.getTeam()) < 50d) {

                                    regions.add(region);

                                }

                            }

                        }
                        if(regions.size() > 2) {

                            possibleOffsets.add(offset);

                        }

                    }

                    if(!possibleOffsets.isEmpty()) {

                        float offset = possibleOffsets.get(0);
                        if(possibleOffsets.size() > 1) {

                            offset = possibleOffsets.get(new Random().nextInt(possibleOffsets.size()-1));

                        }

                        npc.getNMSEntity().yaw = offset;
                        ((InkStorm) weapon).activate();
                        npc.updateAngles(offset, npc.getLocation().getPitch());

                    }

                }

            }

        } else {

            if (weapon instanceof Jetpack && weapon.isActive()) {

                if (!npc.getNavigationManager().isDone()) {

                    NavigationPoint point = npc.getNavigationManager().nextNavigationPoint();
                    if (point != null) {

                        Vector pos1 = point.toVector();
                        Vector pos2 = npc.getLocation().toVector();
                        pos1.setY(0);
                        pos2.setY(0);

                        double delta = pos1.distance(pos2);
                        Vector movement = new Vector(0, 0, 0);

                        if (delta >= 1d) {

                            movement = pos2.clone().subtract(pos1).normalize();

                        } else {

                            npc.getNavigationManager().removeFirstNavigationPoint();

                        }

                        Jetpack jetpack = (Jetpack) weapon;
                        jetpack.handleInput(movement.getX(), movement.getZ());

                    }

                }

                targetSwitchTicks++;

                npc.setShooting(true);
                if (npc.getTargetManager().getTarget() != null) {

                    if (!npc.getTargetManager().getTarget().isDead()) {

                        targetSwitchTicks = 0;
                        Location target = npc.getTargetManager().getTarget().getLastKnownLocation();
                        npc.getWeaponManager().aim(target.toVector());

                    } else {

                        npc.getTargetManager().resetTarget();

                    }

                } else {

                    if (inkJetTarget == null || targetSwitchTicks > 24) {

                        targetSwitchTicks = 0;
                        ArrayList<Location> locations = new ArrayList<>();

                        if (!npc.getTargetManager().getPossibleTargets().isEmpty()) {

                            for (TargetManager.PotentialTarget player : npc.getTargetManager().getPossibleTargets()) {

                                locations.add(player.target.getLocation());

                            }

                        }
                        for (PaintableRegion region : npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), 14d)) {

                            if (region.coverage(npc.getTeam()) <= 50d) {

                                Block[] blocks = region.getPaintableBlocks(npc.getColor());
                                if (blocks.length >= 5) {

                                    locations.add(blocks[new Random().nextInt(blocks.length - 1)].getLocation());

                                }

                            }

                        }

                        if (!locations.isEmpty()) {

                            for (Location location : locations) {

                                if (npc.hasLineOfSight(location)) {

                                    npc.getWeaponManager().aim(location.toVector());
                                    inkJetTarget = location.clone();
                                    break;

                                }

                            }

                        }

                    } else {

                        if (inkJetTarget != null) {

                            Location location = npc.getLocation().clone();
                            Vector direction = inkJetTarget.toVector().clone().subtract(npc.getLocation().toVector()).normalize();
                            Location location1 = new Location(npc.getWorld(), 0, 0, 0);
                            location1.setDirection(direction);
                            npc.getWeaponManager().aim(location.toVector());

                            if (VectorUtil.isValid(direction)) {

                                npc.updateAngles(location1.getYaw(), location1.getPitch());

                            }

                        }

                    }

                }

            } else if (weapon instanceof StingRay && weapon.isActive()) {

                npc.setItemSlot(3);
                npc.setShooting(true);
                if (npc.getTargetManager().getTarget() != null) {

                    if (((StingRay) weapon).modelVisible()) {

                        Vector direction = npc.getTargetManager().getTarget().getLastKnownLocation().toVector().subtract(((StingRay) weapon).noozleLocation().toVector()).normalize();
                        if (VectorUtil.isValid(direction)) {

                            float[] floats = DirectionUtil.directionToYawPitch(direction);
                            SlowRotation rotation = new SlowRotation();
                            float targetYaw = floats[0];
                            float current = npc.yaw();
                            targetYaw = AngleUtil.toValidAngle(targetYaw);
                            current = AngleUtil.toValidAngle(current);

                            rotation.target(targetYaw);
                            rotation.updateAngle(current);

                            rotation.rotate(StingRayModel.turningSpeed);

                            npc.updateAngles(rotation.getAngle(), floats[1]);

                        }

                    }

                } else {

                    if (!npc.getTargetManager().getPossibleTargets().isEmpty()) {

                        npc.getTargetManager().target(npc.getTargetManager().getPossibleTargets().get(0).target);

                    } else {

                        targetSwitchTicks++;
                        if (targetSwitchTicks > 30) {

                            targetSwitchTicks = 0;

                            SlowRotation rotation = new SlowRotation();
                            float targetYaw = npc.getLocation().getYaw() + (-30f + (new Random().nextFloat() * 60));
                            float current = npc.yaw();
                            targetYaw = AngleUtil.toValidAngle(targetYaw);
                            current = AngleUtil.toValidAngle(current);

                            rotation.target(targetYaw);
                            rotation.updateAngle(current);

                            rotation.rotate(StingRayModel.turningSpeed);

                            npc.updateAngles(rotation.getAngle(), npc.pitch() + (-30 + (new Random().nextFloat() * 30)));

                        }

                    }

                }

            } else if (weapon instanceof AbstractBombRush && weapon.isActive()) {

                npc.setItemSlot(3);
                npc.setShooting(true);

                targetSwitchTicks++;
                if (targetSwitchTicks > 14) {

                    ArrayList<PaintableRegion> nearbyRegions = npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), 7d);
                    Collections.sort(nearbyRegions, new Comparator<PaintableRegion>() {
                        @Override
                        public int compare(PaintableRegion o1, PaintableRegion o2) {
                            return Double.compare(o1.coverage(npc.getTeam()), o2.coverage(npc.getTeam()));
                        }
                    });
                    ArrayList<Vector> potentialVectors = new ArrayList<>();
                    int i = 0;
                    for (PaintableRegion region : nearbyRegions) {

                        if (i < 4) {

                            potentialVectors.add(region.getCenter());

                        }

                        i++;

                    }
                    for (TargetManager.PotentialTarget target : npc.getTargetManager().getPossibleTargets()) {

                        potentialVectors.add(target.target.centeredHeightVector());

                    }
                    if (!potentialVectors.isEmpty()) {

                        Vector vector = null;
                        if (potentialVectors.size() == 1) {

                            vector = potentialVectors.get(0);

                        } else {

                            vector = potentialVectors.get(new Random().nextInt(potentialVectors.size() - 1));

                        }
                        Vector dir = vector.subtract(npc.getEyeLocation().toVector()).normalize();
                        if (VectorUtil.isValid(dir)) {

                            float[] dirVal = DirectionUtil.directionToYawPitch(dir);
                            npc.updateAngles(dirVal[0], dirVal[1]);

                        }

                    }

                }

            } else if (weapon instanceof Baller && weapon.isActive()) {

                npc.setItemSlot(3);
                NavigationPoint point = npc.getNavigationManager().nextNavigationPoint();
                if (!npc.getNavigationManager().isDone() && point != null) {

                    Baller baller = (Baller) weapon;

                    Vector delta = point.toVector().subtract(npc.getLocation().toVector());
                    Vector origDelta = delta.clone();
                    delta.setY(0);
                    delta = delta.normalize().multiply(1);
                    if (VectorUtil.isValid(delta)) {

                        baller.handleInput(delta.getX(), delta.getZ(), (origDelta.getY() > 0.2));

                    }

                }

                if(npc.getTargetManager().getTarget() != null) {

                    if(npc.getTargetManager().getTarget().getLastKnownLocation().toVector().distance(npc.getLocation().toVector()) <= 2d || !npc.getTargetManager().nearbyThreats(npc.getLocation().toVector(), 2).isEmpty()) {

                        npc.setShooting(true);

                    } else {

                        npc.setShooting(false);

                    }

                } else {

                    npc.setShooting(true);

                }

            }

        }

    }

    public static void main(String[] args) {

        ArrayList<Double> v = new ArrayList<>();
        v.add(1d);
        v.add(3d);

        Collections.sort(v);
        System.out.println(v.get(0));

    }

    public void onSpecialWeaponEnd() {

        if(npc.getEquipment().getSpecialWeapon() instanceof Jetpack) {

            npc.getTaskController().unpause();
            npc.getNavigationManager().enable();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(false);
            npc.setItemSlot(0);

        } else if(npc.getEquipment().getSpecialWeapon() instanceof AbstractBombRush) {

            npc.getTaskController().unpause();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(false);
            npc.setItemSlot(0);

        } else if(npc.getEquipment().getSpecialWeapon() instanceof StingRay) {

            npc.getTaskController().unpause();
            npc.getNavigationManager().enable();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(false);
            npc.setItemSlot(0);

        } else if(npc.getEquipment().getSpecialWeapon() instanceof Baller) {

            npc.getTaskController().unpause();
            npc.getNavigationManager().enable();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(false);
            npc.setItemSlot(0);

        }

    }

    public void onSpecialWeaponBegin() {

        if(npc.getEquipment().getSpecialWeapon() instanceof Jetpack) {

            npc.getTaskController().pause();
            npc.getWeaponManager().resetAim();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(true);
            npc.getNavigationManager().setTarget(new InkjetNavigationTarget(npc));
            npc.setItemSlot(3);

        } else if(npc.getEquipment().getSpecialWeapon() instanceof AbstractBombRush) {

            npc.getTaskController().pause();
            npc.getWeaponManager().resetAim();
            npc.getNavigationManager().resetTarget();
            npc.setShooting(true);
            npc.getNavigationManager().setTarget(new BombRushNavigationTarget(npc));

        } else if(npc.getEquipment().getSpecialWeapon() instanceof StingRay) {

            npc.getTaskController().pause();
            npc.getWeaponManager().resetAim();
            npc.setShooting(true);
            npc.getNavigationManager().resetTarget();

        } else if(npc.getEquipment().getSpecialWeapon() instanceof Baller) {

            npc.getTaskController().pause();
            npc.getWeaponManager().resetAim();
            npc.getNavigationManager().resetTarget();
            npc.getNavigationManager().setTarget(new BallerNavigationTarget(npc));
            npc.getWeaponManager().resetAim();

        }

    }

}
