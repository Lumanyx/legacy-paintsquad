package de.xenyria.splatoon.ai.pathfinding;

import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.heap.IHeapItem;
import org.bukkit.util.Vector;

public interface PathfindingTarget {

    boolean needsUpdate(Vector vector);
    boolean isReached(SquidAStar pathfinder, Node node, Vector vector);
    boolean useGoalNode();
    SquidAStar.MovementCapabilities getMovementCapabilities();
    void beginPathfinding();
    void endPathfinding();
    int maxNodeVisits();
    NodeListener getNodeListener();

    public static interface NodeListener {

        boolean isPassable(Node node, int nX, int nY, int nZ);
        boolean useAlternativeTargetCheck();
        double getAdditionalWeight(Node node);
        Node getBestNodeFromRemaining(Node[] nodes);

    }

    Vector getEstimatedPosition();

}
