package de.xenyria.splatoon.ai.task.special;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.NavigationPoint;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
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

    public void tick() {

        boolean useChance = RandomUtil.random((int) (npc.getProperties().getAggressiveness() / 10));

        SplatoonSpecialWeapon weapon = npc.getEquipment().getSpecialWeapon();
        if (npc.hasControl() && !npc.isSplatted() && npc.isSpecialReady() && npc.getEquipment().getSpecialWeapon() != null) {

            if (weapon != null) {

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

                }

            }

        }

        if (weapon instanceof Jetpack && weapon.isActive()) {

            if (!npc.getNavigationManager().isDone()) {

                NavigationPoint point = npc.getNavigationManager().nextNavigationPoint();
                if (point != null) {

                    Vector pos1 = point.toVector();
                    Vector pos2 = npc.getLocation().toVector();
                    pos1.setY(0);
                    pos2.setY(0);

                    double delta = pos1.distance(pos2);
                    Vector movement = new Vector(0,0,0);

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

                targetSwitchTicks = 0;
                Location target = npc.getTargetManager().getTarget().getLastKnownLocation();
                npc.getWeaponManager().aim(target.toVector());

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

        }

    }

    public void onSpecialWeaponEnd() {

        if(npc.getEquipment().getSpecialWeapon() instanceof Jetpack) {

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

        }

    }

}
