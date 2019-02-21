package de.xenyria.splatoon.ai.paint;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;
import de.xenyria.splatoon.ai.pathfinding.worker.PathfindingManager;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

public class PaintManager {

    private EntityNPC npc;
    public PaintManager(EntityNPC npc) {

        this.npc = npc;

    }

    private ArrayList<PaintableRegion> possibleRegions = new ArrayList<>();
    private HashMap<PaintableRegion, SquidAStar> regionQueries = new HashMap<>();

    public static double MAX_DISTANCE = 20d;
    public static double MIN_DISTANCE = 5d;
    public static final int MAX_PROCESSABLE_REGIONS = 3;

    private int lastRegionCheck = 0;

    public static final int MAX_BLOCK_QUERY_COUNT = 12;

    public ArrayList<PaintableRegion> getPossibleRegions() { return possibleRegions; }

    public void punish(PaintableRegion region, int i) {

        punishTicks.put(region, i);
        possibleRegions.remove(region);

    }

    public void reset() {

        possibleRegions.clear();
        regionQueries.clear();
        punishTicks.clear();

    }

    public class QueryRegionTarget implements PathfindingTarget {

        private PaintableRegion region;
        private EntityNPC npc;
        public QueryRegionTarget(PaintableRegion region, EntityNPC npc) {

            this.region = region;
            this.npc = npc;
            paintable = region.getPaintableBlocks(npc.getTeam().getColor());

        }

        private Block[] paintable;

        @Override
        public boolean needsUpdate(Vector vector) {
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
            return 40;
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

                    int possible = 0;
                    for(Node node : nodes) {

                        if(node.toVector().distance(region.getCenter()) <= npc.getWeaponManager().maxWeaponDistance()) {

                            for (Block block : paintable) {

                                if (block.getLocation().distance(npc.getLocation()) <= npc.getWeaponManager().maxWeaponDistance()) {

                                    if (npc.getWeaponManager().canHitBlock(
                                            npc.getShootingLocation(node.toVector(), npc.getWeaponManager().getCurrentHandBoolean()), block.getLocation().toVector().add(new Vector(.5, 1.25, .5)), block)) {

                                        possible++;
                                        if (possible >= 4) {

                                            return node;

                                        }

                                    }

                                }

                            }

                        }

                    }
                    return null;

                }

            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return region.getCenter();
        }
    }

    private HashMap<PaintableRegion, Integer> punishTicks = new HashMap<>();

    public boolean isProcessingSpots() {

        for(SquidAStar aStar : regionQueries.values()) {

            if(aStar.getRequestResult() == SquidAStar.RequestResult.PROCESSING) {

                return true;

            }

        }
        return false;

    }
    public boolean hasFinishedProcessingSpots() {

        for(SquidAStar aStar : regionQueries.values()) {

            if(aStar.getRequestResult() != SquidAStar.RequestResult.FOUND && aStar.getRequestResult() != SquidAStar.RequestResult.NOT_FOUND) {

                return false;

            }

        }
        return true;


    }

    public void tick() {

        /*if(!possibleRegions.isEmpty()) {

            PaintableRegion region = possibleRegions.get(0);
            npc.getLocation().getWorld().spawnParticle(Particle.VILLAGER_HAPPY, region.getCenter().toLocation(npc.getWorld()), 0);

        }

        Iterator<Map.Entry<PaintableRegion, Integer>> iterator = punishTicks.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<PaintableRegion, Integer> entry = iterator.next();
            int newVal = entry.getValue() - 1;
            if(newVal < 1) {

                iterator.remove();

            } else {

                entry.setValue(newVal);

            }

        }

        boolean finished = hasFinishedProcessingSpots();
        lastRegionCheck++;

        if(lastRegionCheck > 28 || finished) {

            if(regionQueries.isEmpty() && !isProcessingSpots()) {

                lastRegionCheck = 0;

                ArrayList<PaintableRegion> regions = npc.getMatch().getAIController().nearbyRegions(npc.getLocation().toVector(), MAX_DISTANCE);
                ArrayList<PaintableRegion> validRegions = new ArrayList<>();
                if (!regions.isEmpty()) {

                    for (PaintableRegion region : regions) {

                        if (!punishTicks.containsKey(region) &&

                                region.getCenter().distance(npc.getLocation().toVector()) >= MIN_DISTANCE &&
                                region.coverage(npc.getTeam()) < 90f) {

                            validRegions.add(region);

                        }

                    }

                } else {

                    // Sollte bei ordentlich generierten Regions nicht passieren.
                    possibleRegions.clear();

                }

                if(!validRegions.isEmpty()) {

                    Vector npcLocation = npc.getLocation().toVector();
                    Collections.sort(validRegions, new Comparator<PaintableRegion>() {
                        @Override
                        public int compare(PaintableRegion o1, PaintableRegion o2) { return Double.compare(o1.getCenter().distance(npcLocation), o2.getCenter().distance(npcLocation)); }
                    });
                    ArrayList<PaintableRegion> toProcess = new ArrayList<>();
                    if((validRegions.size() - 1) >= MAX_PROCESSABLE_REGIONS) {

                        for (int i = 0; i < MAX_PROCESSABLE_REGIONS; i++) {

                            toProcess.add(validRegions.get(i));

                        }

                    } else {

                        toProcess.addAll(validRegions);

                    }

                    for(PaintableRegion region : validRegions) {

                        SquidAStar query = new SquidAStar(npc.getWorld(), npc.getLocation().toVector(),
                                new QueryRegionTarget(region, npc), npc.getMatch(), npc.getTeam(), 120);
                        PathfindingManager.queueRequest(query);
                        regionQueries.put(region, query);

                    }

                }

            } else if(finished) {

                possibleRegions.clear();

                int successfulQueries = 0;
                // Erfolgreiche Resultate auflisten
                for(Map.Entry<PaintableRegion, SquidAStar> entry : regionQueries.entrySet()) {

                    SquidAStar aStar = entry.getValue();
                    if(aStar.getRequestResult() == SquidAStar.RequestResult.FOUND) {

                        possibleRegions.add(entry.getKey());
                        successfulQueries++;

                    }

                }


                Collections.sort(possibleRegions, new Comparator<PaintableRegion>() {
                    @Override
                    public int compare(PaintableRegion o1, PaintableRegion o2) {

                        NodePath path1 = regionQueries.get(o1).getNodePath();
                        double additionalWeight1 = npc.getNearbyTeamMembers(o1.getCenter(), 7).size() * 15;
                        double additionalWeight2 = npc.getNearbyTeamMembers(o2.getCenter(), 7).size() * 15;

                        double mod1 = 1d-(o1.coverage(npc.getTeam()) / 100d);
                        double mod2 = 1d-(o2.coverage(npc.getTeam()) / 100d);

                        NodePath path2 = regionQueries.get(o2).getNodePath();

                        return Double.compare(path1.getWeight()+(additionalWeight1) * mod1, path2.getWeight()+(additionalWeight2) * mod2);

                    }
                });

                regionQueries.clear();

            }

        }*/

    }

}
