package de.xenyria.splatoon.ai.task.paint;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.core.math.AngleUtil;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.NavigationPoint;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.AITask;
import de.xenyria.splatoon.ai.task.TaskType;
import de.xenyria.splatoon.ai.task.approach.ApproachEnemiesTask;
import de.xenyria.splatoon.ai.task.approach.ApproachPaintableRegionTask;
import de.xenyria.splatoon.ai.task.signal.SignalType;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponCharger;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.Navigation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PaintAreaTask extends AITask {

    public PaintAreaTask(EntityNPC npc) {
        super(npc);
    }

    @Override
    public TaskType getTaskType() {

        return TaskType.PAINT;

    }

    public class PaintStrip {

        public ArrayList<Block> list;
        private Vector direction;
        public Vector getDirection() { return direction; }

        private double length;
        public double getLength() { return length; }

        private Vector target;

        private int paintableBlocks,paintableWallBlocks,enemyBlocks;

        public PaintStrip(Vector start, Vector direction, double length) {

            this.direction = direction;
            this.length = length;
            target = start.clone().add(direction.clone().multiply(length));

        }

    }

    private PaintStrip target;

    private static float[] possibleDirections = new float[]{0f, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330};

    @Override
    public boolean doneCheck() {
        return !getNPC().getTargetManager().getPossibleTargets().isEmpty() || getNPC().getTargetManager().getTarget() != null;
    }

    private int lastPaintCheck = 0;
    private int errorCount = 0;
    private boolean navigationFlag = false;
    private boolean pathfindingProgress = false;
    private int targetChangeTicks = 0;
    private int timeoutTicker = 0;

    public static void main(String[] args) {

        ArrayList<Double> doubles = new ArrayList<>();
        doubles.add(1d);
        doubles.add(15d);
        Collections.sort(doubles, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return -Double.compare(o1, o2);
            }
        });
        System.out.println(doubles.get(0));

    }

    private int visitCountRemoveTicker = 0;
    public boolean newPathNecessary() {

        AIWeaponManager.AIPrimaryWeaponType type = getNPC().getWeaponManager().getAIPrimaryWeaponType();
        if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER || type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

            return target == null && !navigationFlag && getNPC().getNavigationManager().isDone();

        } else if(type == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

            return getNPC().getNavigationManager().isStuck() || getNPC().getInk() <= 33D || getNPC().getNavigationManager().hasReachedTarget(1);

        }
        return false;

    }

    @Override
    public void tick() {

        if(getNPC().getInk() <= 20D) {

            skip();
            return;

        }

        AIWeaponManager.AIPrimaryWeaponType type = getNPC().getWeaponManager().getAIPrimaryWeaponType();

        if(target != null) {

            timeoutTicker++;
            if(timeoutTicker > 10) {

                target = null;
                timeoutTicker = 0;

            }

        } else {

            timeoutTicker = 0;

        }

        if(targetChangeTicks > 0) {

            targetChangeTicks--;
            if(targetChangeTicks < 1) {

                navigationFlag = false;
                setTarget();

                errorCount = 0;
                lastPaintCheck = 0;
                return;

            }

        }

        if(errorCount > 2) {

            navigationFlag = false;
            setTarget();
            getNPC().getSignalManager().signal(SignalType.NO_PAINTABLE_SPOTS_AROUND, 200);

            errorCount = 0;
            lastPaintCheck = 0;

            skip();

            if(new Random().nextBoolean()) {

                getNPC().getTaskController().forceNewTask(new ApproachPaintableRegionTask(getNPC()));

            } else {

                getNPC().getTaskController().forceNewTask(new ApproachEnemiesTask(getNPC()));

            }

            return;

        }



        if(lastPaintCheck > 0) { lastPaintCheck--; }
        if((type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER || type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) && !pathfindingProgress && target == null && lastPaintCheck < 1) {

            lastPaintCheck = 9;
            ArrayList<PaintStrip> strips = new ArrayList<>();
            for(float angle : possibleDirections) {

                Location location = new Location(getNPC().getWorld(), 0,0,0);
                location.setYaw(angle);
                location.setPitch(0f);
                Vector direction = location.getDirection();

                Vector start = getNPC().getShootingLocation(getNPC().getWeaponManager().getCurrentHandBoolean()).toVector();
                Vector end = start.clone().add(direction.clone().multiply(getNPC().getWeaponManager().maxWeaponDistance()));

                if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                    ProjectileExaminer.Result result = getNPC().getWeaponManager().examineProjectile(start, end);
                    Vector shootEnd = null;
                    if(result.getTrajectory() != null) {

                        if(result.isTargetReached()) {

                            shootEnd = end.clone();

                        } else {

                            shootEnd = result.getHitLocation().toVector();

                        }

                        if(start.distance(shootEnd) >= 2d) {

                            ArrayList<Block> unpainted = getUnpaintedBlocks(result.getTrajectory());
                            PaintStrip strip = new PaintStrip(start, direction, start.distance(shootEnd));

                            for (Block block : unpainted) {

                                boolean wall = getNPC().getMatch().isWall(block);

                                if (getNPC().getMatch().isEnemyTurf(block, getNPC().getTeam())) {

                                    if (!wall) {

                                        strip.enemyBlocks++;

                                    }

                                } else {

                                    if (!wall) {

                                        strip.paintableBlocks++;

                                    } else {

                                        strip.paintableWallBlocks++;

                                    }

                                }

                            }
                            strip.list = unpainted;
                            if ((strip.enemyBlocks + strip.paintableBlocks) >= 4) {

                                strips.add(strip);

                            }

                        }

                    }

                } else if(type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                    ArrayList<Block> blocks = new ArrayList<>();
                    start = getNPC().getEyeLocation().toVector();
                    double distance = getNPC().getWeaponManager().maxWeaponDistance();
                    PaintStrip strip = new PaintStrip(start, direction, distance);
                    strip.list = blocks;

                    RayTraceResult result = getNPC().getMatch().getWorldInformationProvider().rayTraceBlocks(start, direction, distance, true);
                    if(result != null) {

                        distance = result.getHitPosition().distance(start);

                    }

                    if(distance >= 3d) {

                        Vector cursor = start.clone();

                        for (double d = 0; d <= distance; d += .25d) {

                            cursor.add(direction.clone().multiply(0.25d));
                            Block block = BlockUtil.ground(cursor.toLocation(getNPC().getWorld()), 10);

                            if (getNPC().getMatch().isPaintable(getNPC().getTeam(), block.getX(), block.getY(), block.getZ())) {

                                if (!blocks.contains(block)) {

                                    blocks.add(block);
                                    if (getNPC().getMatch().isEnemyTurf(block, getNPC().getTeam())) {

                                        strip.enemyBlocks++;

                                    } else {

                                        if (getNPC().getMatch().isWall(block)) {

                                            strip.paintableWallBlocks++;

                                        } else {

                                            strip.paintableBlocks++;

                                        }

                                    }

                                }

                            }

                        }
                        strip.target = start.clone().add(direction.clone().multiply(direction));
                        if ((strip.enemyBlocks + strip.paintableBlocks) >= (getNPC().getWeaponManager().maxWeaponDistance() * .825d)) {

                            strips.add(strip);

                        }

                    }

                }

            }

            if(strips.size() > 1) {

                Collections.sort(strips, new Comparator<PaintStrip>() {

                    @Override
                    public int compare(PaintStrip o1, PaintStrip o2) {

                        boolean charger = getNPC().getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.CHARGER;

                        double weight1 = (o1.enemyBlocks*5)+(o1.paintableBlocks*7)+((!charger) ? (o1.paintableWallBlocks) : 0);
                        double weight2 = (o2.enemyBlocks*5)+(o2.paintableBlocks*7)+((!charger) ? (o2.paintableWallBlocks) : 0);

                        return -Double.compare(weight1, weight2);

                    }

                });

            }

            if(strips.isEmpty()) {

                errorCount++;

            } else {

                /*if(strips.size() >= 3) {

                    target = strips.get(new Random().nextInt(strips.size()-1));

                } else {

                    target = strips.get(0);

                }*/
                target = strips.get(0);
                float[] f = DirectionUtil.directionToYawPitch(target.direction);

            }

        }

        if(target != null) {

            for(Block block : target.list) {

                Location location = block.getLocation();
                location = location.add(.5, 1, .5);

            }

            if(getNPC().getNavigationManager().isStuck()) { setTarget(); return; }

            if(!getNPC().isSquid()) {

                if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                    //if (getNPC().getNavigationManager().doLookInDirection()) {

                        //getNPC().getNavigationManager().disableLookInDirection();

                    //}

                    getNPC().getWeaponManager().aim(target.target);
                    getNPC().getWeaponManager().fire(30);
                    if (targetChangeTicks == 0) {

                        targetChangeTicks = 6 + (new Random().nextInt(5));

                    }

                } else if(type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                    if(getNPC().getWeaponManager().getShootingTicks() == 0) {

                        AIWeaponCharger charger = (AIWeaponCharger) getNPC().getWeaponManager().getAIWeaponInterface();
                        getNPC().getWeaponManager().aim(target.target);

                        getNPC().getWeaponManager().fire((int) (charger.estimatedChargeTimeForTargetDistance(getNPC().getEyeLocation().toVector().distance(target.target)) / 50));

                        targetChangeTicks = getNPC().getWeaponManager().getShootingTicks() + (new Random().nextInt(4));
                        currentAimStrip = target;

                    }

                }

            } else {

                getNPC().leaveSquidForm();

            }

        } else {

            if(!getNPC().isSquid()) {

                if (type == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                    if (!getNPC().getNavigationManager().doLookInDirection()) {

                        getNPC().getNavigationManager().enableLookInDirection();

                    }

                }

            }

        }

        if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER || type == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

            if (!getNPC().isShooting() && !getNPC().getNavigationManager().doLookInDirection()) {

                getNPC().getWeaponManager().resetAim();
                getNPC().getNavigationManager().enableLookInDirection();

            }

            if(type == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                if(getNPC().isShooting()) {

                    getNPC().getNavigationManager().disableLookInDirection();

                }

            }

        } else {

            /*Vector lastDelta = getNPC().getLastDelta();
            Vector current = getNPC().getLocation().toVector();
            Vector target = current.clone().add(lastDelta);
            Vector direction = target.clone().subtract(current).normalize().multiply(-1);

            if(VectorUtil.isValid(direction)) {

                Location location = new Location(getNPC().getWorld(), 0,0,0);
                location.setDirection(direction);
                getNPC().updateAngles(location.getYaw(), 0);

            }*/
            if(currentAimStrip != null) {

                if(getNPC().getNavigationManager().doLookInDirection()) {

                    getNPC().getNavigationManager().disableLookInDirection();

                }

                getNPC().getWeaponManager().aim(currentAimStrip.target);
                float[] floats = DirectionUtil.directionToYawPitch(currentAimStrip.direction);
                getNPC().updateAngles(floats[0], floats[1]);

            }

        }

    }

    private PaintStrip currentAimStrip = null;

    public class ReachPaintableTerritoryTarget implements PathfindingTarget {

        @Override
        public boolean needsUpdate(Vector vector) {
            return newPathNecessary();
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
            capabilities.walkOnEnemyTurf = false;
            if(getNPC().getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                capabilities.requiredNodesToSwim = 1;

            } else if(getNPC().getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                capabilities.canRoll = true;

            } else if(getNPC().getWeaponManager().getAIPrimaryWeaponType() == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                capabilities.requiredNodesToSwim = 3;
                capabilities.squidFormUsable = true;

            }
            return capabilities;

        }

        @Override
        public void beginPathfinding() {

            pathfindingProgress = true;
            positionBefore = getNPC().getLocation().toVector();

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 145;
        }

        public ThreeDimensionalArray<Double> cachedCoverage = new ThreeDimensionalArray<>();
        public ThreeDimensionalArray<Integer> nearbyTeamMembers = new ThreeDimensionalArray<>();

        public int getNearbyTeamMembers(int x, int y, int z) {

            PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(x,y,z);
            Vector center = coordinate.center();
            if(nearbyTeamMembers.exists(coordinate.getX(), coordinate.getY(), coordinate.getZ())) {

                return nearbyTeamMembers.get(coordinate.getX(), coordinate.getY(), coordinate.getZ());

            } else {

                int members = getNPC().getNearbyTeamMembers(center, 6d).size();
                nearbyTeamMembers.set(members, coordinate.getX(), coordinate.getY(), coordinate.getZ());
                return members;

            }

        }

        public double getCoverage(int x, int y, int z) {

            /*if(cachedCoverage.exists(x,y,z)) {

                return cachedCoverage.get(x,y,z);

            } else {*/

                PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(x,y,z);
                PaintableRegion region = getNPC().getMatch().getAIController().getPaintableRegion(coordinate);
                if(region != null) {

                    double coverage = region.coverage(getNPC().getTeam());
                    cachedCoverage.set(coverage, x, y, z);
                    return coverage;

                } else {

                    return -1d;

                }

           // }

        }
        private Vector positionBefore = null;

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

                    double coverage = getCoverage(node.x, node.y, node.z);
                    //int members = getNearbyTeamMembers(node.x, node.y, node.z);
                    PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(node.x, node.y, node.z);
                    double additionalWeight = 0d;
                    if(getNPC().getRegionTracker().hasCoordinate(coordinate)) { additionalWeight = getNPC().getRegionTracker().getVisitCounts(coordinate) * 80; }

                    double distance = positionBefore.distance(node.toVector());
                    //additionalWeight+=members*35;
                    if(distance <= 5d) {

                        additionalWeight+=((5d-distance) * 4);

                    }

                    additionalWeight-=(node.toVector().distance(getNPC().getSpawnPoint().toVector())*3);

                    return (coverage) + lastDistWeight + additionalWeight;

                }

                public boolean hasSafeParents(Node node) {

                    if(node.getParent() != null) {

                        TransitionType[] unsafe = new TransitionType[]{
                                TransitionType.SWIM_WALL_VERTICAL,
                                TransitionType.JUMP_TO,
                                TransitionType.RIDE_RAIL,
                                TransitionType.INK_RAIL,
                                TransitionType.FALL,
                                TransitionType.SWIM_BLOCKED,
                                TransitionType.SWIM_DRY
                        };
                        for(TransitionType transitionType : unsafe) {

                            if(node.getParent().getType() == transitionType) {

                                return false;

                            }

                        }

                    }
                    return true;

                }

                @Override
                public Node getBestNodeFromRemaining(Node[] nodes) {

                    ArrayList<PaintableRegion.Coordinate> nearbyCoordinates = new ArrayList<>();
                    Node lowest = null;
                    double lowestCoverage = 0d;
                    for(Node node : nodes) {

                        if(hasSafeParents(node)) {

                            double coverageVal = getCoverage(node.x, node.y, node.z);
                            double coverage = coverageVal
                                    + (getNPC().getRegionTracker().getVisitCounts(PaintableRegion.Coordinate.fromWorldCoordinates(node.x, node.y, node.z)) * 7);

                            if (coverageVal != -1D && coverageVal <= getRegionCoverageThreshold() && (lowest == null || coverage < lowestCoverage)) {

                                lowest = node;
                                lowestCoverage = coverage;

                            }

                        }

                    }

                    if(lowest != null) {

                        for(Node node : nodes) {

                            PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(
                                    node.x, node.y, node.z
                            );
                            if (!nearbyCoordinates.contains(coordinate) && coordinate.center().distance(lowest.toVector()) <= 6.5d) {

                                nearbyCoordinates.add(coordinate);

                            }

                        }

                        PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(
                                lowest.x, lowest.y, lowest.z
                        );

                        for(PaintableRegion.Coordinate coordinate1 : nearbyCoordinates) {

                            int amount = 4;
                            if(!coordinate1.equals(coordinate)) { amount = 1; }

                            for(int i = 0; i < amount; i++) {

                                getNPC().getRegionTracker().addVisitCount(coordinate1);

                            }

                            /*
                            if (!visitCounts.containsKey(coordinate1)) {

                                visitCounts.put(coordinate1, amount);

                            } else {

                                visitCounts.put(coordinate1, visitCounts.get(coordinate1) + amount);

                            }*/

                        }

                    } else {

                        //getNPC().getSignalManager().signal(SignalType.NO_PAINTABLE_SPOTS_AROUND, 120);

                    }

                    navigationFlag = true;
                    pathfindingProgress = false;
                    return lowest;

                }

            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return getNPC().getLocation().toVector();
        }

    }

    public double getRegionCoverageThreshold() {

        return 70d;

    }

    public void setTarget() {

        target = null;
        getNPC().getNavigationManager().setTarget(new ReachPaintableTerritoryTarget());

    }

    // Bezieht Paintbelow mit ein
    public ArrayList<Block> getUnpaintedBlocks(Trajectory trajectory) {

        ArrayList<Block> blocks = new ArrayList<>();
        for(Vector3f vector : trajectory.getVectors()) {

            Location location = new Location(getNPC().getWorld(), vector.x, vector.y, vector.z);
            Block block = BlockUtil.ground(location, 5);

            Match match = getNPC().getMatch();
            if(match.isPaintable(getNPC().getTeam(), block.getX(), block.getY(), block.getZ())) {

                blocks.add(block);

            }

        }
        return blocks;

    }

    @Override
    public void onInit() {

        setTarget();
        if(getNPC().getNavigationManager().doLookInDirection()) {

            getNPC().getNavigationManager().disableLookInDirection();

        }

    }

    @Override
    public void onExit() {

        getNPC().getWeaponManager().resetAim();
        if(!getNPC().getNavigationManager().doLookInDirection()) {

            getNPC().getNavigationManager().enableLookInDirection();

        }

    }
}
