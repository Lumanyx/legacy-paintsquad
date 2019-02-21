package de.xenyria.splatoon.ai.task.combat;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.ai.task.debug.FleeTask;
import de.xenyria.splatoon.ai.task.regenerate.RegenerateTask;
import de.xenyria.splatoon.ai.task.tutorial.ReturnToSpawnTask;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class AttackTask extends AITask {

    private TargetManager.Target target;
    public AttackTask(EntityNPC npc, TargetManager.Target target, SquidAStar path) {

        super(npc);
        this.target = target;
        npc.getNavigationManager().useExistingPathfinder(path);

    }

    private boolean flee;

    @Override
    public TaskType getTaskType() {
        return TaskType.ATTACK;
    }

    @Override
    public boolean doneCheck() {

        return flee || target.isDead() || getNPC().getTargetManager().getTarget() != target;

    }

    public static final double FLEE_CONSIDER_MIN_HEALTH = 30d;
    public static final double FLEE_REQUIRED_AGGRESSIVENESS = 60d;
    public static final int FLEE_REQUIRED_MIN_PERCENTAGE = 40;

    private int lastJumpTicker = 0;

    @Override
    public void tick() {

        if(target != null) {

            dodgeOrientation = target.getLastKnownLocation().toVector();

        } else {

            dodgeOrientation = getNPC().getLocation().toVector();

        }

        AIWeaponManager.AIPrimaryWeaponType type = getNPC().getWeaponManager().getAIPrimaryWeaponType();
        ticksSinceLastPath++;
        if(!getNPC().isShooting()) {

            if(!getNPC().getNavigationManager().doLookInDirection()) {

                if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER || type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                    getNPC().getNavigationManager().enableLookInDirection();

                }

            }

        }

        if(!getNPC().getNavigationManager().doLookInDirection() && type == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

            getNPC().getNavigationManager().enableLookInDirection();

        }

        lastJumpTicker++;

        // Desto höher die Aggressivität ist desto wahrscheinlicher ist es, dass der NPC nicht flieht
        boolean couldFlee = getNPC().getHealth() < FLEE_CONSIDER_MIN_HEALTH;
        if(couldFlee && target.getEnemy() instanceof EntityNPC) {

            if(getNPC().getProperties().getAggressiveness() < FLEE_REQUIRED_AGGRESSIVENESS) {

                double aggressiveness = FLEE_REQUIRED_AGGRESSIVENESS - getNPC().getProperties().getAggressiveness();
                double ratio = aggressiveness / FLEE_REQUIRED_AGGRESSIVENESS;
                if((int)(ratio * 100) >= FLEE_REQUIRED_MIN_PERCENTAGE) {

                    // TODO Change Task
                    getNPC().getTaskController().forceNewTask(new RegenerateTask(getNPC()));
                    return;

                }

            }

        }

        // Basics
        if(!target.isVisible()) {

            invisibleTicks++;
            if(invisibleTicks > target.getTicksToDismiss()) {

                skip();
                getNPC().getTargetManager().punishTarget(target.getEnemy());
                return;

            }

        } else {

            invisibleTicks = 0;

        }

        lastTargetHitCheck++;
        if(lastTargetHitCheck > 3) {

            lastTargetHitCheck = 0;
            if(target.isVisible()) {

                targetHitable = getNPC().getWeaponManager().canHitEntity(getNPC().getShootingLocation(
                        getNPC().getWeaponManager().getCurrentHandBoolean()
                ), target.getLastKnownLocation(), target.getEnemy());

            } else {

                targetHitable = false;

            }

        }

        if(getNPC().getTargetManager().getTarget() != null) {

            TargetManager.Target target = getNPC().getTargetManager().getTarget();

        }

        Action action = getCurrentAction();
        if(action == Action.UNKNOWN) {

            setOptimalTarget();

        } else {

            if(action == Action.DODGE) {

                if(lastJumpTicker > 30 && !getNPC().isSquid()) {

                    lastJumpTicker = 0;
                    if(RandomUtil.random((int) getNPC().getProperties().getAggressiveness())) {

                        if(getNPC().canJump()) { getNPC().jump(.2d); }

                    }

                }

                // Wird ausgewichen aber das Ziel ist nicht mehr erreichbar so wird wieder in den Navigationsmodus gewechselt
                if(!targetHitable || !target.isVisible() || target.getLastKnownLocation().distance(getNPC().getLocation()) >= 3d) {

                    setEnemyNavigationTarget();

                }

            } else if(action == Action.NAVIGATE) {

                if(targetHitable && target.isVisible()) {

                    setDodgeNavigationTarget();

                }

            }

        }

        if(getNPC().getNavigationManager().getCurrentTargetFailureCount() >= 4) {

            skip();
            if(getNPC().getMatch().getMatchType() == MatchType.TUTORIAL) {

                getNPC().getTaskController().forceNewTask(new ReturnToSpawnTask(getNPC()));

            }

            return;

        }

        // Schießen
        if(getNPC().getWeaponManager().enoughInkToShootPrimaryWeapon()) {

            boolean isRoller = type == AIWeaponManager.AIPrimaryWeaponType.ROLLER;
            if((targetHitable || isRoller) && !getNPC().isSquid()) {

                // TODO Waffentypen
                AIWeaponManager.AIPrimaryWeaponType weaponType = getNPC().getWeaponManager().getAIPrimaryWeaponType();

                if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                    getNPC().getWeaponManager().aim(target.getEnemy());
                    getNPC().getWeaponManager().fire(10);

                } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                    getNPC().getWeaponManager().aim(target.getEnemy());
                    getNPC().getWeaponManager().fire(30);

                } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                    getNPC().getWeaponManager().aim(target.getEnemy());
                    if(getNPC().getWeaponManager().getShootingTicks() == 0) {

                        getNPC().getWeaponManager().fire(
                                getNPC().getWeaponManager().requiredTicksForDistance(target.getEnemy().getLocation().distance(getNPC().getLocation())+1.5d)
                        );

                    }

                }

            } else {

                if(getNPC().isSquid()) {

                    getNPC().getNavigationManager().setSquidOnNavigationFinish(false);

                }

            }

        } else {

            getNPC().getTaskController().forceNewTask(new RegenerateTask(getNPC()));
            return;

        }

        if(getCurrentAction() != Action.UNKNOWN) {

            if(getNPC().getNavigationManager().getCurrentTargetFailureCount() > 7) {

                skip();
                getNPC().getTargetManager().resetTarget();

            }

        }

    }

    private boolean targetHitable = false;
    private int lastTargetHitCheck = 0;

    private int invisibleTicks;

    public boolean hitable(Vector vector) {

        return getNPC().getWeaponManager().canHitEntity(getNPC().getShootingLocation(vector, getNPC().getWeaponManager().getCurrentHandBoolean()), target.getLastKnownLocation(), target.getEnemy());

    }

    public enum Action {

        NAVIGATE,
        DODGE,
        UNKNOWN;

    }

    public Action getCurrentAction() {

        if(getNPC().getNavigationManager().getTarget() instanceof EnemyDodgeTarget) {

            return Action.DODGE;

        } else if(getNPC().getNavigationManager().getTarget() instanceof EnemyNavigationTarget) {

            return Action.NAVIGATE;

        } else {

            return Action.UNKNOWN;

        }

    }

    private int ticksSinceLastPath = 0;
    public boolean newPath() {

        return ticksSinceLastPath > 5;

    }
    private Vector dodgeOrientation = null;

    public class EnemyDodgeTarget implements PathfindingTarget {

        private EntityNPC npc;
        private AttackTask task;
        private TargetManager.Target target;
        private Vector beforeRequest;
        private double targetDistBeforeRequest;
        private AIWeaponManager.AIPrimaryWeaponType weaponType;

        public EnemyDodgeTarget(AttackTask task, EntityNPC npc, TargetManager.Target target) {

            this.task = task;
            this.npc = npc;
            this.target = target;
            this.weaponType = npc.getWeaponManager().getAIPrimaryWeaponType();

        }

        @Override
        public boolean needsUpdate(Vector vector) {
            return true;
        }

        public boolean hitable(Vector vector) {

            return npc.getWeaponManager().canHitEntity(npc.getShootingLocation(vector, npc.getWeaponManager().getCurrentHandBoolean()), target.getLastKnownLocation(), target.getEnemy());

        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                double horDist = VectorUtil.horDistance(vector, target.getLastKnownLocation().toVector());
                double maxDist = (npc.getWeaponManager().maxWeaponDistance())*.33;

                return vector.distance(beforeRequest) >= 2.5 && horDist >= 2.5 && horDist <= maxDist && hitable(vector);

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                return vector.distance(beforeRequest) >= 1 && vector.distance(target.getLastKnownLocation().toVector()) <= 0.5d;

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                return hitable(vector) && vector.distance(beforeRequest) >= 2.5 && vector.distance(target.getLastKnownLocation().toVector()) <= (npc.getWeaponManager().maxWeaponDistance()*.66);

            }

            return false;

        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {

            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
                if (npc.getInk() <= 33d && npc.getInk() > 2d) {

                    capabilities.squidFormUsable = true;

                } else {

                    capabilities.squidFormUsable = false;

                }
                return capabilities;

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
                if (npc.getInk() <= 33d && npc.getInk() > 2d) {

                    capabilities.squidFormUsable = true;
                    capabilities.exitAsHuman = true;

                } else {

                    capabilities.squidFormUsable = false;
                    capabilities.canRoll = true;

                }
                return capabilities;

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
                capabilities.squidFormUsable = false;
                capabilities.exitAsHuman = true;
                return capabilities;

            }

            return new SquidAStar.MovementCapabilities();

        }

        @Override
        public void beginPathfinding() {

            beforeRequest = npc.getLocation().toVector();
            targetDistBeforeRequest = npc.getLocation().distance(target.getLastKnownLocation());

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 75;
        }

        @Override
        public NodeListener getNodeListener() {
            return new NodeListener() {
                @Override
                public boolean isPassable(Node node, int nX, int nY, int nZ) {
                    return true;
                }

                @Override
                public boolean useAlternativeTargetCheck() {
                    return true;
                }

                @Override
                public double getAdditionalWeight(Node node) {
                    return 0;
                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    Node bestNode = null;
                    double nearestNode = 0d;

                    for(Node node : nodes) {

                        double distance = node.toVector().distance(dodgeOrientation);
                        if(bestNode == null || distance < nearestNode) {

                            bestNode = node;
                            nearestNode = distance;

                        }
                        if(hitable(node.toVector())) {

                            return node;

                        }

                    }

                    return bestNode;

                }
            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return dodgeOrientation;
        }

    }


    public static class EnemyNavigationTarget implements PathfindingTarget {

        private TargetManager.Target target;
        private EntityNPC npc;
        private AIWeaponManager.AIPrimaryWeaponType weaponType;

        public EnemyNavigationTarget(EntityNPC npc, TargetManager.Target target) {

            this.target = target;
            this.npc = npc;
            this.weaponType = npc.getWeaponManager().getAIPrimaryWeaponType();

        }

        public boolean hitable(Vector vector) {

            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                Location shootLoc = npc.getShootingLocation(vector, npc.getWeaponManager().getCurrentHandBoolean());
                World world = npc.getLocation().getWorld();
                return npc.getWeaponManager().canHitEntity(npc.getShootingLocation(vector, npc.getWeaponManager().getCurrentHandBoolean()), target.getLastKnownLocation(), target.getEnemy());

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                return vector.distance(target.getLastKnownLocation().toVector()) <= 1d;

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                return npc.getWeaponManager().canHitEntity(vector.clone().add(new Vector(0, 1.62, 0)).toLocation(npc.getWorld()), target.getLastKnownLocation(), target.getEnemy());


            }
            return true;

        }

        @Override
        public boolean needsUpdate(Vector vector) {

            //return target.isVisible() && !hitable(vector);
            return true;

        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

            double distToLast = node.toVector().distance(before);

            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                return distToLast>=1.5&&VectorUtil.horDistance(vector, target.getLastKnownLocation().toVector()) <= 3.5 && hitable(vector);

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                return VectorUtil.horDistance(vector, target.getLastKnownLocation().toVector()) <= 0.25d;

            }
            return false;

        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {

            // Default
            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
                if (npc.getInk() <= 33d && npc.getInk() > 2d) {

                    capabilities.squidFormUsable = true;

                } else {

                    capabilities.squidFormUsable = false;

                }
                return capabilities;

            } else if(weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
                capabilities.canRoll = true;
                capabilities.requiredNodesToSwim = 3;
                return capabilities;

            }

            return new SquidAStar.MovementCapabilities();


        }

        Vector before = null;
        @Override
        public void beginPathfinding() {

            before = npc.getLocation().toVector();

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 130;
        }

        @Override
        public Vector getEstimatedPosition() {
            return target.getLastKnownLocation().toVector();
        }

        @Override
        public NodeListener getNodeListener() {
            return new NodeListener() {
                @Override
                public boolean isPassable(Node node, int nX, int nY, int nZ) {
                    return true;
                }

                @Override
                public boolean useAlternativeTargetCheck() {
                    return true;
                }

                @Override
                public double getAdditionalWeight(Node node) {
                    return 0;
                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    if(target.getLastKnownLocation() != null) {

                        Vector vector = target.getLastKnownLocation().toVector();
                        Node bestNode = null;
                        double nearestNode = 0d;

                        for (Node node : nodes) {

                            double distance = node.toVector().distance(vector);
                            if (bestNode == null || distance < nearestNode) {

                                bestNode = node;
                                nearestNode = distance;

                            }
                            if (hitable(node.toVector())) {

                                return node;

                            }

                        }

                        return bestNode;

                    }
                    return null;

                }
            };
        }

    }

    public void setOptimalTarget() {

        if(!targetHitable || target.getLastKnownLocation().distance(getNPC().getLocation()) >= 4d) {

            setEnemyNavigationTarget();

        } else {

            setDodgeNavigationTarget();

        }

    }

    public void setDodgeNavigationTarget() {

        getNPC().getNavigationManager().setTarget(new EnemyDodgeTarget(this, getNPC(), target));

    }

    public void setEnemyNavigationTarget() {

        getNPC().getNavigationManager().setTarget(new EnemyNavigationTarget(getNPC(), target));

    }

    @Override
    public void onInit() {

        getNPC().getNavigationManager().setSquidOnNavigationFinish(false);
        setOptimalTarget();

    }

    @Override
    public void onExit() {

        if(!getNPC().getNavigationManager().doLookInDirection()) {

            getNPC().getNavigationManager().enableLookInDirection();

        }

        getNPC().getNavigationManager().resetTarget();
        getNPC().getTargetManager().resetTarget();

    }

}
