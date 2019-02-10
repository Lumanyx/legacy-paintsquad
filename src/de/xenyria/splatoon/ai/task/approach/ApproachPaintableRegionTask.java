package de.xenyria.splatoon.ai.task.approach;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class ApproachPaintableRegionTask extends AITask {

    public ApproachPaintableRegionTask(EntityNPC npc) {

        super(npc);
        region = getNPC().getMatch().getAIController().getNextRegion(npc.getTeam(), npc.getLocation().toVector());
        if(region == null) {

            ArrayList<Team> enemies = getNPC().getMatch().getEnemyTeams(npc.getTeam());
            orientationLocation = getNPC().getMatch().getNextSpawnPoint(enemies.get(0)).toVector();

        } else {

            orientationLocation = region.getCenter();
            skip = true;

        }

    }

    private boolean skip = false;
    private Vector orientationLocation;
    private PaintableRegion region;

    @Override
    public TaskType getTaskType() {
        return TaskType.APPROACH;
    }

    public void setTarget() {

        getNPC().getNavigationManager().setTarget(new ReachRegionTarget());

    }

    public class ReachRegionTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return !flag;
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


        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 250;
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

                    double lastDistWeight = 0d;
                    for(Location location : getNPC().getTimeLine().last(5)) {

                        double maxDist = 12d;
                        double dist = location.toVector().distance(node.toVector());
                        if(dist <= 12d) {

                            lastDistWeight+=(maxDist-dist);

                        }

                    }
                    lastDistWeight*=100d;
                    return lastDistWeight;

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    double lowest = 0d;
                    Node lowestNode = null;

                    for(Node node : nodes) {

                        if(node != null) {

                            double dist = node.toVector().distance(orientationLocation);
                            if(lowestNode == null || dist < lowest) {

                                lowestNode = node;
                                lowest = dist;

                            }

                        }

                    }

                    flag = true;
                    return lowestNode;

                }
            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return orientationLocation;
        }
    }

    private boolean flag = false;
    private boolean flag1 = false;
    @Override
    public boolean doneCheck() {
        return flag1 && (getNPC().getNavigationManager().isStuck() || getNPC().getNavigationManager().isDone());
    }

    @Override
    public void tick() {

        if(skip) {

            getNPC().getTaskController().forceNewTask(new ApproachEnemiesTask(getNPC()));
            return;

        }

        if(flag) {

            flag1 = true;

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
