package de.xenyria.splatoon.ai.task.tutorial;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import org.bukkit.util.Vector;

public class ReturnToSpawnTask extends AITask {

    private Vector spawn;
    public ReturnToSpawnTask(EntityNPC npc) {
        super(npc);
        spawn = npc.getSpawnPoint().toVector();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.APPROACH;
    }

    @Override
    public boolean doneCheck() {
        return getNPC().getTargetManager().hasPotentialTarget() || getNPC().getLocation().distance(getNPC().getSpawnPoint()) <= .5d;
    }

    @Override
    public void tick() {

    }

    private boolean flag;
    public class GoToSpawnTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return !flag || getNPC().getNavigationManager().isDone();
        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {
            return false;
        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {
            return new SquidAStar.MovementCapabilities();
        }

        @Override
        public void beginPathfinding() {

            flag = true;

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 0;
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
                    return node.toVector().distance(getNPC().getSpawnPoint().toVector());
                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    Node nearest = null;
                    double lowest = 0d;
                    for(Node node : nodes) {

                        if(nearest == null || node.toVector().distance(getNPC().getSpawnPoint().toVector()) < lowest) {

                            lowest = node.toVector().distance(getNPC().getSpawnPoint().toVector());
                            nearest = node;

                        }

                    }

                    return nearest;

                }
            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return spawn;
        }
    }

    @Override
    public void onInit() {

        getNPC().getNavigationManager().setTarget(new GoToSpawnTarget());

    }

    @Override
    public void onExit() {

        getNPC().getNavigationManager().resetTarget();

    }

}
