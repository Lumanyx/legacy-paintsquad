package de.xenyria.splatoon.ai.task.debug;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;

import static de.xenyria.splatoon.ai.task.AITaskController.INK_TO_ATTACK;

public class FleeTask extends AITask {

    public FleeTask(EntityNPC npc) {

        super(npc);

    }

    @Override
    public TaskType getTaskType() {

        return TaskType.FLEE;

    }

    @Override
    public boolean doneCheck() {

        return flag1 && (getNPC().getNavigationManager().isDone() || getNPC().getNavigationManager().isStuck());

    }

    private boolean flag, flag1;

    public static final double NO_ENEMY_RADIUS = 7d;

    public class AvoidPlayerTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return !flag;
        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

            return getNPC().getTargetManager().nearbyThreats(getNPC().getLocation().toVector(), NO_ENEMY_RADIUS).isEmpty();

        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {

            SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
            capabilities.requiredNodesToSwim = 0;
            capabilities.exitAsHuman = false;
            return capabilities;


        }

        @Override
        public void beginPathfinding() {

            flag = true;
            startVector = getNPC().getLocation().toVector();

        }

        private Vector startVector;

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 120;
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

                    return getNPC().getTargetManager().nearbyThreats(node.toVector(), NO_ENEMY_RADIUS).size() * 5;

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    // Höher -> Besser
                    double highestScore = 0d;
                    Node highestNode = null;

                    for(Node node : nodes) {

                        double nodeScore = node.toVector().distance(startVector) * 2;
                        int nearbyThreats = getNPC().getTargetManager().nearbyThreats(node.toVector(), NO_ENEMY_RADIUS).size();
                        nodeScore+=((NO_ENEMY_RADIUS-nearbyThreats) * 30d);

                        if(node.getType() == TransitionType.SWIM && (highestNode == null || nodeScore > highestScore)) {

                            highestNode = node;
                            highestScore = nodeScore;

                        }

                    }
                    if(highestNode != null) {

                        getNPC().getWorld().spawnParticle(Particle.BARRIER, highestNode.x, highestNode.y, highestNode.z, 0);

                    }
                    return highestNode;

                }

            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return getNPC().getLocation().toVector();
        }

    }

    public class WeightedJumpPoint {

        private JumpPoint point;
        private double weight;

    }

    @Override
    public void tick() {


        if(getNPC().getNavigationManager().getCurrentTargetFailureCount() >= 3 || (getNPC().getInk() <= 35D && getNPC().getHealth() <= 50D)) {

            // Supersprung
            double superJumpChance = getNPC().getProperties().getAggressiveness();
            if(RandomUtil.random((int)superJumpChance)) {

                ArrayList<WeightedJumpPoint> availablePoints = new ArrayList<>();
                for(JumpPoint point : getNPC().getMatch().getJumpPoints(getNPC())) {

                    double dist = point.getLocation().distance(getNPC().getLocation());
                    double nearbyEnemies = getNPC().getTargetManager().nearbyThreats(point.getLocation().toVector(), 10).size() * 4;
                    WeightedJumpPoint jumpPoint = new WeightedJumpPoint();
                    jumpPoint.point = point;
                    jumpPoint.weight = (dist+nearbyEnemies);

                    if(point instanceof JumpPoint.Beacon) {

                        jumpPoint.weight*=1.5d;

                    }

                    availablePoints.add(jumpPoint);

                }

                if(!availablePoints.isEmpty()) {

                    getNPC().superJump(availablePoints.get(0).point.getLocation(), 27);
                    return;

                }

            }

        }


        if(flag) {

            flag1 = true;

        }

    }

    public void setTarget() {

        getNPC().getNavigationManager().setTarget(new AvoidPlayerTarget());

    }

    @Override
    public void onInit() {

        setTarget();

    }

    @Override
    public void onExit() {

        getNPC().getNavigationManager().resetTarget();

    }

}
