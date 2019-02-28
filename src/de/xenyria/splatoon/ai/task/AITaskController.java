package de.xenyria.splatoon.ai.task;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.NavigationPoint;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.combat.AttackTask;
import de.xenyria.splatoon.ai.task.debug.FleeTask;
import de.xenyria.splatoon.ai.task.paint.PaintAreaTask;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.ai.task.paint.RollAreaTask;
import de.xenyria.splatoon.ai.task.regenerate.RegenerateTask;
import de.xenyria.splatoon.ai.task.secondary.SecondaryWeaponManager;
import de.xenyria.splatoon.ai.task.signal.SignalType;
import de.xenyria.splatoon.ai.task.special.SpecialWeaponManager;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

public class AITaskController {

    private EntityNPC npc;
    public AITaskController(EntityNPC npc) {

        this.npc = npc;
        secondaryWeaponManager = new SecondaryWeaponManager(npc);
        specialWeaponManager = new SpecialWeaponManager(npc);

    }

    private AITask task;

    public void forceNewTask(AITask task) {

        if(this.task != null) {

            this.task.onExit();
            this.task = null;

        }

        this.task = task;
        task.onInit();

    }

    public static final double INK_TO_ATTACK = 80d;

    public void pause() {

        if(task != null) {

            task.onExit();
            task = null;

        }
        paused = true;

    }
    public void unpause() {

        paused = false;

    }

    private boolean paused = false;
    public boolean isPaused() { return paused; }

    public class WeightedLocation implements Comparable<WeightedLocation> {

        private Object location;
        private double weight;

        public WeightedLocation(Object location, double weight) {

            this.location = location;
            this.weight = weight;

        }

        @Override
        public int compareTo(WeightedLocation o) {
            return Double.compare(weight, o.weight);
        }
    }


    private SecondaryWeaponManager secondaryWeaponManager;
    public SecondaryWeaponManager getSecondaryWeaponManager() { return secondaryWeaponManager; }

    private SpecialWeaponManager specialWeaponManager;
    public SpecialWeaponManager getSpecialWeaponManager() { return specialWeaponManager; }

    private int lastSuperJumpCheckTicks, lastSecondaryUse, lastSecondaryCheck;
    private int splatTicks = 0;

    public void tick() {

        String dbg = "";
        lastSuperJumpCheckTicks++;
        lastSecondaryUse++;
        lastSecondaryCheck++;

        secondaryWeaponManager.tick();
        specialWeaponManager.tick();

        if(task == null || task.isDone()) {

            if (task != null) {
                task.onExit();
            }
            task = null;

            if (!paused) {

                if (npc.getMatch().getMatchType() == MatchType.TUTORIAL) {

                    if (task == null) {

                        if (npc.isOnOwnInk()) {

                            npc.getNavigationManager().setSquidOnNavigationFinish(true);

                            if(!npc.isSquid()) {

                                npc.enterSquidForm();

                            }

                        } else {

                            npc.getNavigationManager().setSquidOnNavigationFinish(false);
                            if(npc.isSquid()) {

                                npc.leaveSquidForm();

                            }

                        }

                        npc.getNavigationManager().setSquidOnNavigationFinish(false);
                        if (npc.getTargetManager().hasPotentialTarget()) {

                            boolean exec = true;
                            for (SplatoonPlayer player : npc.getMatch().getPlayers(npc.getTeam())) {

                                if (!player.isHuman()) {

                                    EntityNPC npc = (EntityNPC) player;
                                    AITaskController cntrl = npc.getTaskController();
                                    if (cntrl.task != null && npc.getTargetManager().hasTarget()) {

                                        exec = false;
                                        break;

                                    }

                                }

                            }

                            if (exec) {

                                TargetManager.PotentialTarget target = npc.getTargetManager().getPossibleTargets().get(0);
                                npc.getTargetManager().target(target.target);
                                task = new AttackTask(npc, npc.getTargetManager().getTarget(), target.path);
                                task.onInit();

                            }

                        }

                    }

                } else if (npc.getMatch().getMatchType() == MatchType.TURF_WAR) {

                    if (!paused && !npc.inSuperJump()) {

                        if (lastSuperJumpCheckTicks > 20) {

                            int ticksSinceRespawn = npc.getTicksSinceRespawn();

                            boolean doSuperJump = RandomUtil.random(3 + ((ticksSinceRespawn<100&&ticksSinceRespawn>10) ? 4 : 0));

                            lastSuperJumpCheckTicks = 0;
                            if (doSuperJump) {

                                if (npc.getSignalManager().isActive(SignalType.NO_ENEMIES_AROUND)) {

                                    if (npc.getHealth() >= 50D) {

                                        lastSuperJumpCheckTicks = 0;
                                        // Einen Spieler unterstützen
                                        ArrayList<SplatoonPlayer> team = new ArrayList<>();
                                        for (SplatoonPlayer player : npc.getMatch().getPlayers(npc.getTeam())) {

                                            if (player != npc && !player.isSplatted()) {

                                                team.add(player);

                                            }

                                        }

                                        int maxEnemies = npc.getMatch().getMaxEnemies(npc);
                                        if (!team.isEmpty()) {

                                            ArrayList<WeightedLocation> locations = new ArrayList<>();
                                            for (SplatoonPlayer player : team) {

                                                JumpPoint point = npc.getMatch().getJumpPointFor(player);
                                                if (point != null && player.getLocation().distance(npc.getLocation()) > 24) {

                                                    int nearbyEnemies = npc.getTargetManager().nearbyThreats(player.getLocation().toVector(), 12d).size();
                                                    if (nearbyEnemies > 0) {

                                                        int weight = maxEnemies - nearbyEnemies;
                                                        weight *= 4;
                                                        WeightedLocation location = new WeightedLocation(point, weight);
                                                        locations.add(location);

                                                    }

                                                }

                                            }
                                            Collections.sort(locations);
                                            if (!locations.isEmpty()) {

                                                WeightedLocation location = locations.get(0);
                                                JumpPoint point = (JumpPoint) location.location;

                                                if (npc.superJump(point.getLocation(), 26)) {

                                                    point.onJumpBegin();

                                                }
                                                npc.getSignalManager().dismiss(SignalType.NO_ENEMIES_AROUND);
                                                return;

                                            }

                                        }

                                    }

                                } else if (npc.getSignalManager().isActive(SignalType.NO_PAINTABLE_SPOTS_AROUND)) {

                                    if (npc.getInk() >= 30D) {

                                        lastSuperJumpCheckTicks = 0;
                                        // Einen Spieler unterstützen
                                        ArrayList<SplatoonPlayer> team = new ArrayList<>();
                                        for (SplatoonPlayer player : npc.getMatch().getPlayers(npc.getTeam())) {

                                            if (player != npc && !player.isSplatted()) {

                                                team.add(player);

                                            }

                                        }

                                        if (!team.isEmpty()) {

                                            ArrayList<WeightedLocation> locations = new ArrayList<>();
                                            for (SplatoonPlayer player : team) {

                                                JumpPoint point = npc.getMatch().getJumpPointFor(player);
                                                if (point != null && point.getLocation().distance(npc.getLocation()) >= 24) {

                                                    int weight = npc.getTargetManager().nearbyThreats(player.getLocation().toVector(), 12d).size();
                                                    weight *= 10;
                                                    weight += (npc.getMatch().getAIController().coverageInArea(player.getLocation(), player.getTeam(), 12d));
                                                    WeightedLocation location = new WeightedLocation(point, weight);
                                                    locations.add(location);

                                                }

                                            }
                                            Collections.sort(locations);
                                            if (!locations.isEmpty()) {

                                                WeightedLocation location = locations.get(0);
                                                JumpPoint point = (JumpPoint) location.location;

                                                if (npc.superJump(point.getLocation(), 26)) {

                                                    point.onJumpBegin();

                                                }
                                                npc.getSignalManager().dismiss(SignalType.NO_PAINTABLE_SPOTS_AROUND);
                                                return;

                                            }

                                        }

                                    }

                                }

                            }

                        }

                        if (!npc.getTargetManager().getPossibleTargets().isEmpty()) {

                            if (npc.getInk() >= INK_TO_ATTACK) {

                                TargetManager.PotentialTarget player = npc.getTargetManager().getMostImportantThreat();
                                npc.getTargetManager().target(player.target);
                                task = new AttackTask(npc, npc.getTargetManager().getTarget(), player.path);
                                task.onInit();

                            } else {

                                task = new RegenerateTask(npc);
                                task.onInit();

                            }

                        } else {

                            if (npc.getInk() >= INK_TO_ATTACK) {

                                //if (RandomUtil.random(((int) npc.getProperties().getAggressiveness() / 2))) {

                                if (npc.getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.SHOOTER ||
                                npc.getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                                    task = new PaintAreaTask(npc);
                                    task.onInit();

                                } else if (npc.getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                                    task = new RollAreaTask(npc);
                                    task.onInit();

                                }

                                //} else {

                                //task = new ApproachEnemiesTask(npc);
                                //task.onInit();

                                //}

                            } else {

                                task = new RegenerateTask(npc);
                                task.onInit();

                            }

                            //task = new FleeTask(npc);
                            //task.onInit();

                        }

                    } else {

                        if (task != null) {

                            task.onExit();
                            task = null;

                        }

                    }

                }

            }

        } else {

            if (!paused) {

                task.tick();

                if(!npc.inSuperJump() && npc.hasControl()) {

                    if(!npc.getTargetManager().hasTarget() && npc.getWeaponManager().getShootingTicks() < 2) {

                        // Vorliegende Navigationspunkte einfärben
                        ArrayList<NavigationPoint> nodes = npc.getNavigationManager().readNextPointChain(TransitionType.WALK_ENEMY, 3);
                        Collections.reverse(nodes);
                        if ((
                                npc.getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.SHOOTER ||
                                        npc.getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.CHARGER) && !nodes.isEmpty()) {

                            for (NavigationPoint point : nodes) {

                                Block block = npc.getWorld().getBlockAt(
                                        point.x, (int) (point.y - 1), point.z
                                );
                                if (npc.getMatch().isOwnedByTeam(block, npc.getTeam())) {

                                    point.updateTransitionType(TransitionType.WALK);

                                }

                                if (npc.getWeaponManager().canHitBlock(npc.getShootingLocation(npc.getWeaponManager().getCurrentHandBoolean()), block.getLocation().toVector().add(new Vector(.5, 1.25, .5)), block)) {

                                    npc.getWeaponManager().aim(block);
                                    npc.getWeaponManager().fire(20);
                                    break;

                                }


                            }

                        }

                    }

                }

            }

            if (npc.inSuperJump()) {

                task.onExit();
                task = null;

            }

        }

        if(EntityNPC.DEBUG_MODE) {

            String dbgStr = "";
            if (task == null) {

                dbgStr = "§cNo Task";

            } else {

                String className = task.getClass().getName();
                String name = className.split("\\.")[className.split("\\.").length - 1];
                dbgStr = "§4" + name;

            }
            npc.a3.setCustomName(dbgStr);

        }

        if(task != null) {

            /*String className = task.getClass().getName();
            className = className.split("\\.")[className.split("\\.").length - 1];

            dbg+="§c"+ className + " (" + task.getID() + " | " + npc.getNavigationManager().getTarget() + " | " + npc.getLocation().getBlockX() + ", " + npc.getLocation().getBlockY() + ", " + npc.getLocation().getBlockZ() + " | " + npc.getInk();
            npc.diagnosticStand2.setCustomName(dbg);*/

        }

    }

    public void reset() {

        specialWeaponManager.reset();
        paused = false;
        if(task != null) {

            task.onExit();
            task = null;

        }

    }

}
