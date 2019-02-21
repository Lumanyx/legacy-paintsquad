package de.xenyria.splatoon.ai.task.special;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class BallerNavigationTarget implements PathfindingTarget {

    private EntityNPC npc;
    public BallerNavigationTarget(EntityNPC npc) {

        this.npc = npc;
        this.maxEnemies = npc.getMatch().getMaxEnemies(npc);

    }

    private int maxEnemies;


    int askCounter = 0;

    @Override
    public boolean needsUpdate(Vector vector) {

        return npc.getNavigationManager().isDone() || npc.getNavigationManager().isStuck();

    }

    @Override
    public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

        return npc.getTargetManager().nearbyThreats(vector, 1d).size() >= 1;

    }

    @Override
    public boolean useGoalNode() {
        return false;
    }

    @Override
    public SquidAStar.MovementCapabilities getMovementCapabilities() {

        SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
        capabilities.squidFormUsable = false;
        capabilities.walkOnEnemyTurf = true;
        capabilities.useRails = false;
        capabilities.useFountains = false;
        capabilities.canJump = false;
        capabilities.hitboxWidth = 0.85d;
        capabilities.hitboxHeight = 2d;
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
        return 60;
    }

    @Override
    public NodeListener getNodeListener() {
        return new NodeListener() {
            @Override
            public boolean isPassable(Node node, int nX, int nY, int nZ) {
                return node.getType() != TransitionType.SWIM_WALL_VERTICAL && node.getType() != TransitionType.JUMP_TO;
            }

            @Override
            public boolean useAlternativeTargetCheck() {
                return true;
            }

            private HashMap<Node, Double> lastDistWeights = new HashMap<>();

            @Override
            public double getAdditionalWeight(Node node) {

                double weight = 0d;
                TransitionType transitionType = node.getType();
                if(transitionType == TransitionType.WALK || transitionType == TransitionType.FALL) { weight+=40d; }
                int nearbyEnemies = maxEnemies-npc.getTargetManager().nearbyThreats(node.toVector(), 7d).size();

                double lastDistWeight = 0d;
                for(Location location : npc.getTimeLine().last(5)) {

                    double maxDist = 12d;
                    double dist = location.toVector().distance(node.toVector());
                    if(dist <= 12d) {

                        lastDistWeight+=(maxDist-dist);

                    }

                }
                lastDistWeights.put(node, lastDistWeight);
                weight+=lastDistWeight*4;
                return weight+(nearbyEnemies*30);

            }

            @Override
            public Node getBestNodeFromRemaining(Node[] nodes) {

                Node bestNode = null;
                double highestScore = 0d;

                for(Node node : nodes) {

                    int enemies = npc.getTargetManager().nearbyThreats(node.toVector(), 7d).size();
                    double score = (enemies*3)+node.toVector().distance(npc.getLocation().toVector()) + (lastDistWeights.getOrDefault(node, 0d));
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

        if(npc.getTargetManager().getTarget() != null) {

            return npc.getTargetManager().getTarget().getLastKnownLocation().toVector();

        }

        return npc.getLocation().toVector();
    }
}
