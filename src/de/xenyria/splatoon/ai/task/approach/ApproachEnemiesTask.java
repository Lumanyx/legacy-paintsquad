package de.xenyria.splatoon.ai.task.approach;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.ai.task.signal.SignalType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ApproachEnemiesTask extends AITask {

    public ApproachEnemiesTask(EntityNPC npc) {
        super(npc);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.APPROACH;
    }

    @Override
    public boolean doneCheck() {

        return flag1 && (getNPC().getNavigationManager().isDone() || getNPC().getNavigationManager().isStuck() || getNPC().getTargetManager().hasPotentialTarget() || getNPC().getTargetManager().hasTarget());
    }

    private boolean flag1;
    private Vector orientation;

    @Override
    public void tick() {

        if(flag) {

            flag1 = true;

        }

    }

    private boolean flag;
    private boolean calculateFlag = false;
    public class ApproachEnemyTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return !calculateFlag;
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
            capabilities.requiredNodesToSwim = 0;
            capabilities.walkOnEnemyTurf = true;
            return capabilities;

        }

        private Vector begin;

        @Override
        public void beginPathfinding() {

            begin = getNPC().getLocation().toVector();
            calculateFlag = true;

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 175;
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

                private ThreeDimensionalArray<Integer> nearbyThreats = new ThreeDimensionalArray<>();

                public int getNearbyThreats(Node node, Vector vector) {

                    if(nearbyThreats.exists(node.x, node.y, node.z)) {

                        return nearbyThreats.get(node.x,node.y,node.z);

                    } else {

                        int enemies = getNPC().getTargetManager().nearbyThreats(vector, 30d).size();
                        nearbyThreats.set(enemies, node.x, node.y, node.z);
                        return enemies;

                    }

                }

                private ThreeDimensionalArray<Double> lastDistWeights = new ThreeDimensionalArray<>();

                @Override
                public double getAdditionalWeight(Node node) {

                    if(!lastDistWeights.exists(node.x, node.y, node.z)) {

                        double lastDistWeight = 0d;
                        for (Location location : getNPC().getTimeLine().last(5)) {

                            double maxDist = 12d;
                            double dist = location.toVector().distance(node.toVector());
                            if (dist <= 12d) {

                                lastDistWeight += (maxDist - dist);

                            }

                        }
                        lastDistWeight *= 15d;

                        lastDistWeights.set(lastDistWeight, node.x, node.y, node.z);

                    }
                    double lastDistWeight = lastDistWeights.get(node.x, node.y, node.z);

                    return ((lastDistWeight) + ((maxEnemies) - getNearbyThreats(node, node.toVector())) * 5);

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    int mostTargets = 0;
                    double score = 0;
                    Node bestNode = null;
                    for(Node node : nodes) {

                        if(node != null) {

                            int targets = getNearbyThreats(node, node.toVector());
                            double currentScore = (getNearbyThreats(node, node.toVector()) * 20) + node.toVector().distance(begin) + (node.toVector().distance(getNPC().getSpawnPoint().toVector())*3);
                            if((bestNode == null || currentScore > score)) {

                                score = currentScore;
                                bestNode = node;

                            }

                            if(targets > mostTargets) {

                                mostTargets = targets;

                            }

                        }

                    }

                    if(mostTargets == 0) {

                        getNPC().getSignalManager().signal(SignalType.NO_ENEMIES_AROUND, 160);

                    }
                    flag = true;
                    return bestNode;

                }
            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return orientation;
        }

    }

    private int maxEnemies;
    @Override
    public void onInit() {

        getNPC().getNavigationManager().setTarget(new ApproachEnemyTarget());
        ArrayList<SplatoonPlayer> enemies = new ArrayList<>();
        for(SplatoonPlayer player : getNPC().getMatch().getAllPlayers()) {

            if(!player.isSpectator() && player.getTeam() != getNPC().getTeam()) {

                maxEnemies++;
                enemies.add(player);

            }

        }
        Collections.sort(enemies, new Comparator<SplatoonPlayer>() {
            @Override
            public int compare(SplatoonPlayer o1, SplatoonPlayer o2) {
                return Double.compare(o1.getLocation().distance(getNPC().getLocation()), o2.getLocation().distanceSquared(getNPC().getLocation()));
            }
        });
        if(!enemies.isEmpty()) {

            orientation = enemies.get(0).getLocation().toVector();

        } else {

            orientation = getNPC().getLocation().toVector();

        }

    }

    @Override
    public void onExit() {

        getNPC().getNavigationManager().resetTarget();

    }

}