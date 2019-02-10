package de.xenyria.splatoon.ai.task.special;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.NPC;
import org.bukkit.util.Vector;

public class InkjetNavigationTarget implements PathfindingTarget {

    public InkjetNavigationTarget(EntityNPC npc) {

        this.npc = npc;

    }

    private EntityNPC npc;
    @Override
    public boolean needsUpdate(Vector vector) {
        return npc.getNavigationManager().isDone();
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
        capabilities.canRoll = false;
        capabilities.requiredNodesToSwim = 9999;
        capabilities.climbEveryWall = false;
        capabilities.walkOnEnemyTurf = true;
        return capabilities;

    }

    @Override
    public void beginPathfinding() {

        if(npc.getTaskController().getSpecialWeaponManager().getInkJetTarget() != null) {

            orientation = npc.getTaskController().getSpecialWeaponManager().getInkJetTarget().toVector();

        } else {

            if (npc.getTargetManager().getTarget() != null) {

                orientation = npc.getTargetManager().getTarget().getLastKnownLocation().toVector();

            } else {

                orientation = npc.getLocation().toVector();

            }

        }

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

                Block block = npc.getWorld().getBlockAt(nX,nY,nZ);
                for(int x = 0; x <= 5; x++) {

                    block = block.getRelative(BlockFace.UP);
                    if(!block.isEmpty()) {

                        return false;

                    }

                }

                return true;

            }

            @Override
            public boolean useAlternativeTargetCheck() {
                return true;
            }

            @Override
            public double getAdditionalWeight(Node node) {

                double score = 0d;
                for(Location location : npc.getTimeLine().last(3)) {

                    double distance = location.toVector().distance(node.toVector());
                    if(distance <= 10d) {

                        score+=(10d-distance);

                    }

                }

                return score;

            }

            @Override
            public Node getBestNodeFromRemaining(Node[] nodes) {

                double val = 0d;
                Node highest = null;

                for(Node node : nodes) {

                    if(node.getType() == TransitionType.WALK || node.getType() == TransitionType.FALL) {

                        if (highest == null || (val > node.toVector().distance(npc.getLocation().toVector()))) {

                            highest = node;
                            val = node.toVector().distance(npc.getLocation().toVector());

                        }

                    }

                }

                return highest;

            }
        };
    }

    private Vector orientation = null;
    @Override
    public Vector getEstimatedPosition() {

        if(orientation != null) {

            return orientation;

        } else {

            return npc.getLocation().toVector();

        }

    }
}
