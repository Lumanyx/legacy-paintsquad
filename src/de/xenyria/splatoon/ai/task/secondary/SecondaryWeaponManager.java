package de.xenyria.splatoon.ai.task.secondary;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class SecondaryWeaponManager {

    private EntityNPC npc;
    public SecondaryWeaponManager(EntityNPC npc) {

        this.npc = npc;

    }

    private int lastSecondaryCheck = 0;
    public void tick() {

        lastSecondaryCheck++;
        if(!npc.isSplatted() && npc.hasControl() && !npc.isSpecialActive() && npc.getEquipment().getSecondaryWeapon() != null) {

            if (lastSecondaryCheck > 20) {

                lastSecondaryCheck = 0;
                AIWeaponManager.AISecondaryWeaponType type = npc.getWeaponManager().getAISecondaryWeaponType();
                boolean secondaryUseChance = RandomUtil.random(7);
                AIThrowableBomb bomb = (AIThrowableBomb) npc.getEquipment().getSecondaryWeapon();

                // Genug Tinte?
                double inkUsage = npc.getEquipment().getSecondaryWeapon().getNextInkUsage();
                if (npc.hasEnoughInk((float) inkUsage)) {

                    if (type == AIWeaponManager.AISecondaryWeaponType.DAMAGEBOMB) {

                        // Bombe in die Nähe eines Gegners werfen
                        if (npc.getTargetManager().getTarget() != null) {

                            TargetManager.Target target = npc.getTargetManager().getTarget();
                            if (target.isVisible() && !target.isDead()) {

                                Location targetLocation = target.getLastKnownLocation();
                                if (targetLocation.distance(npc.getLocation()) >= 1.5) {

                                    ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(npc.getEyeLocation(), targetLocation, bomb.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                                    if (result.isTargetReached() && result.getHitLocation() != null && result.getTrajectory() != null) {

                                        if (secondaryUseChance) {

                                            bomb.throwBomb(result.getHitLocation(), result.getTrajectory());
                                            npc.removeInk(npc.getEquipment().getSecondaryWeapon().getNextInkUsage());
                                            //Bukkit.broadcastMessage("Bomb thrown to kill.");

                                        }

                                    }

                                }

                            }

                        } else {

                            // Bombe auf eine einfärbbare Fläche werfen
                            ArrayList<PaintableRegion> nearby = npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), 7.5d);
                            ArrayList<PaintableRegion> potentialRegions = new ArrayList<>();
                            for (PaintableRegion region : nearby) {

                                if (region.coverage(npc.getTeam()) <= 50d && region.getFloorCoordinates().size() >= 5) {

                                    potentialRegions.add(region);

                                }

                            }
                            Collections.sort(potentialRegions, new Comparator<PaintableRegion>() {
                                @Override
                                public int compare(PaintableRegion o1, PaintableRegion o2) {
                                    return -Double.compare(o1.coverage(npc.getTeam()), o2.coverage(npc.getTeam()));
                                }
                            });
                            if (!potentialRegions.isEmpty()) {

                                PaintableRegion region = potentialRegions.get(0);
                                int tries = 3;
                                while (tries > 0) {

                                    Node node = region.getFloorCoordinates().get(new Random().nextInt(region.getFloorCoordinates().size() - 1));
                                    Vector target = node.toVector();

                                    ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(npc.getEyeLocation(), target.toLocation(npc.getWorld()), bomb.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                                    if (result.isTargetReached() && result.getHitLocation() != null && result.getTrajectory() != null) {

                                        if (secondaryUseChance) {

                                            bomb.throwBomb(result.getHitLocation(), result.getTrajectory());
                                            npc.removeInk(npc.getEquipment().getSecondaryWeapon().getNextInkUsage());
                                            //Bukkit.broadcastMessage("Bomb thrown to paint.");
                                            tries = 0;
                                            break;

                                        }

                                    }

                                    tries--;

                                }

                            }

                        }

                    } else if(type == AIWeaponManager.AISecondaryWeaponType.PAINTBOMB) {

                        // Bombe auf eine einfärbbare Fläche werfen
                        ArrayList<PaintableRegion> nearby = npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), 7.5d);
                        ArrayList<PaintableRegion> potentialRegions = new ArrayList<>();
                        for (PaintableRegion region : nearby) {

                            if (region.coverage(npc.getTeam()) <= 10d && region.getFloorCoordinates().size() >= 5) {

                                potentialRegions.add(region);

                            }

                        }
                        Collections.sort(potentialRegions, new Comparator<PaintableRegion>() {
                            @Override
                            public int compare(PaintableRegion o1, PaintableRegion o2) {
                                return -Double.compare(o1.coverage(npc.getTeam()), o2.coverage(npc.getTeam()));
                            }
                        });
                        if (!potentialRegions.isEmpty()) {

                            PaintableRegion region = potentialRegions.get(0);
                            int tries = 3;
                            while (tries > 0) {

                                Node node = region.getFloorCoordinates().get(new Random().nextInt(region.getFloorCoordinates().size() - 1));
                                Vector target = node.toVector();

                                ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(npc.getEyeLocation(), target.toLocation(npc.getWorld()), bomb.getImpulse(), npc.getMatch(), npc.getTeam(), npc);
                                if (result.isTargetReached() && result.getHitLocation() != null && result.getTrajectory() != null) {

                                    if (secondaryUseChance) {

                                        bomb.throwBomb(result.getHitLocation(), result.getTrajectory());
                                        npc.removeInk(npc.getEquipment().getSecondaryWeapon().getNextInkUsage());
                                        //Bukkit.broadcastMessage("Bomb thrown to paint.");
                                        tries = 0;
                                        break;

                                    }

                                }

                                tries--;

                            }

                        }

                    }

                }

            }

        }

    }

}
