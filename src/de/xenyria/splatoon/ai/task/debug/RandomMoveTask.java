package de.xenyria.splatoon.ai.task.debug;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import org.bukkit.util.Vector;

import java.util.Random;

public class RandomMoveTask extends AITask {

    private Vector startLocation;
    private Vector randomTarget;
    public RandomMoveTask(EntityNPC npc) {

        super(npc);
        startLocation = npc.getLocation().toVector().clone();

        double range = 6d;
        double mod1 = 4d + new Random().nextDouble();
        double mod2 = 4d + new Random().nextDouble();

        if(new Random().nextBoolean()) {

            mod1*=-1;

        }
        if(new Random().nextBoolean()) {

            mod2*=-1;

        }

        randomTarget = startLocation.clone().add(new Vector(mod1 * range, 0, mod2 * range));

    }

    @Override
    public TaskType getTaskType() {
        return TaskType.APPROACH;
    }

    public class RandomPositionTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return true;
        }

        @Override
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {
            return vector.distance(startLocation) > 3d;
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
            return 50;
        }

        @Override
        public NodeListener getNodeListener() {
            return null;
        }

        @Override
        public Vector getEstimatedPosition() {
            return randomTarget;
        }
    }

    @Override
    public boolean doneCheck() {
        return getNPC().getLocation().toVector().distance(startLocation) >= 1 || getNPC().getNavigationManager().getCurrentTargetFailureCount() > 3;
    }

    @Override
    public void tick() {

        if(!hasTarget()) {

            setTarget();

        }

    }

    public void setTarget() {

        getNPC().getNavigationManager().setTarget(new RandomPositionTarget());

    }
    public boolean hasTarget() {

        return !(getNPC().getNavigationManager().getTarget() instanceof RandomPositionTarget);

    }

    @Override
    public void onInit() {

        setTarget();
        if(!getNPC().getNavigationManager().doLookInDirection()) {

            getNPC().getNavigationManager().enableLookInDirection();

        }

    }

    @Override
    public void onExit() {

    }
}
