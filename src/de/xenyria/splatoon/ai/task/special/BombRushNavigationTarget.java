package de.xenyria.splatoon.ai.task.special;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import org.bukkit.util.Vector;

public class BombRushNavigationTarget implements PathfindingTarget {

    private EntityNPC npc;
    public BombRushNavigationTarget(EntityNPC npc) {

        this.npc = npc;

    }

    int updateAskCounter = 0;
    @Override
    public boolean needsUpdate(Vector vector) {

        updateAskCounter++;
        if(updateAskCounter>4) { return true; }
        return false;

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

        SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
        capabilities.squidFormUsable = false;
        capabilities.climbEveryWall = false;
        capabilities.exitAsHuman = true;
        return capabilities;

    }

    @Override
    public void beginPathfinding() {

    }

    @Override
    public void endPathfinding() {

    }

    @Override
    public int maxNodeVisits() {
        return 80;
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
                double highestScore = 0d;

                for(Node node : nodes) {

                    double score = node.toVector().distance(npc.getLocation().toVector());
                    for(PaintableRegion region : npc.getMatch().getAIController().nearbyRegions(node.toVector(), 6d)) {

                        if(region.foundBlocks() > 5 && region.getFloorCoordinates().size() > 5) {

                            score+=((100d-region.coverage(npc.getTeam()))*2);

                        }

                    }
                    score+=npc.getTargetManager().nearbyThreats(npc.getLocation().toVector(), 5d).size()*20;

                    if(bestNode == null || score > highestScore) {

                        bestNode = node;
                        highestScore = score;

                    }

                }

                return bestNode;

            }
        };
    }

    @Override
    public Vector getEstimatedPosition() {
        return npc.getLocation().toVector();
    }
}
