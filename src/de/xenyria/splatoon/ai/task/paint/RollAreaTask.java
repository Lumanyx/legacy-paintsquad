package de.xenyria.splatoon.ai.task.paint;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.ai.task.approach.ApproachEnemiesTask;
import de.xenyria.splatoon.ai.task.approach.ApproachPaintableRegionTask;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Random;

public class RollAreaTask extends AITask {

    public RollAreaTask(EntityNPC npc) {
        super(npc);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.PAINT;
    }

    @Override
    public boolean doneCheck() {
        return getNPC().getTargetManager().hasPotentialTarget() || getNPC().getInk() <= 20d;
    }

    private boolean reach = false;
    public class RollAreaTarget implements PathfindingTarget {

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

            SquidAStar.MovementCapabilities movementCapabilities = new SquidAStar.MovementCapabilities();
            movementCapabilities.requiredNodesToSwim = 0;
            movementCapabilities.canRoll = true;
            return movementCapabilities;

        }


        @Override
        public void beginPathfinding() {

            flag = true;
            System.out.println("Begin");

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

                    Block below = getNPC().getWorld().getBlockAt(node.x, node.y-1, node.z);
                    double nearby = getNPC().getTargetManager().nearbyThreats(node.toVector(), 8d).size() * 8;
                    if(!getNPC().getMatch().isPaintable(getNPC().getTeam(), below)) {

                        return 20d+nearby;

                    }

                    return 0d+nearby;

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    int highestScore = 0;
                    Node highestNode = null;
                    for(Node node : nodes) {

                        if (node != null) {

                            Node parent = node;
                            int score = 0;
                            while (parent != null) {

                                if (parent.getType() == TransitionType.ROLL_ENEMY_TURF) {

                                    score += 10;

                                } else if (parent.getType() == TransitionType.ROLL_UNPAINTED) {

                                    score += 7;

                                }
                                parent = parent.getParent();

                            }
                            if (highestNode == null || (score > highestScore)) {

                                highestNode = node;
                                highestScore = score;

                            }

                        }

                    }

                    System.out.println("highest: " + highestScore);
                    if(highestScore >= 30) {

                        return highestNode;

                    } else {

                        reach = true;
                        return null;

                    }

                }
            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return getNPC().getLocation().toVector();
        }
    }

    public void setTarget() {

        getNPC().getNavigationManager().setTarget(new RollAreaTarget());

    }

    private boolean flag,flag1;

    @Override
    public void tick() {

        if(flag) {

            flag1 = true;

        }

        if(reach) {

            getNPC().getTaskController().forceNewTask(new ApproachPaintableRegionTask(getNPC()));

        }

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
