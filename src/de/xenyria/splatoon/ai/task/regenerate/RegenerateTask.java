package de.xenyria.splatoon.ai.task.regenerate;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import org.bukkit.util.Vector;

import java.util.Random;

public class RegenerateTask extends AITask {

    double rechargeTarget = 0d;
    public RegenerateTask(EntityNPC npc) {

        super(npc);
        rechargeTarget = 60d+(new Random().nextDouble()*20);

    }

    @Override
    public TaskType getTaskType() {
        return TaskType.REGENERATE;
    }

    @Override
    public boolean doneCheck() {

        return getNPC().getInk() >= rechargeTarget;

    }

    @Override
    public void tick() {

        getNPC().getNavigationManager().setSquidOnNavigationFinish(true);
        if(getNPC().getNavigationManager().isDone()) {

            if(!getNPC().isSquid()) {

                getNPC().enterSquidForm();

            }

            getNPC().setLastInkContactTicks(0);

        }

    }

    private boolean flag = false;
    public class RegenerateTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return getNPC().getNavigationManager().isStuck() || !getNPC().isSquid() || !flag || (getNPC().getNavigationManager().isDone() && !getNPC().isOnOwnInk());
        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {
            return node.getType() == TransitionType.SWIM && node.getParent() != null && node.getParent().getParent() != null && node.toVector().distance(positionBefore) >= 3d;
        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {

            SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
            capabilities.exitAsHuman = false;
            capabilities.requiredNodesToSwim = 1;
            return capabilities;

        }

        @Override
        public void beginPathfinding() {

            flag = true;
            positionBefore = getNPC().getLocation().toVector();

        }

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

                    if(node.getType() != TransitionType.SWIM) {

                        return 10d;

                    } else {

                        return 0d;

                    }

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    double highestNode = 0d;
                    Node bestNode = null;
                    for(Node node : nodes) {

                        double dist = node.toVector().distance(positionBefore);
                        if(dist > highestNode || bestNode == null) {

                            bestNode = node;
                            highestNode = dist;

                        }

                    }

                    return bestNode;

                }

            };
        }

        private Vector positionBefore = new Vector(0,0,0);
        @Override
        public Vector getEstimatedPosition() {
            return getNPC().getLocation().toVector();
        }
    }

    @Override
    public void onInit() {

        getNPC().getNavigationManager().setTarget(new RegenerateTarget());
        getNPC().getNavigationManager().setSquidOnNavigationFinish(true);

    }

    @Override
    public void onExit() {

        getNPC().getNavigationManager().resetTarget();
        getNPC().getNavigationManager().setSquidOnNavigationFinish(false);

    }

}
