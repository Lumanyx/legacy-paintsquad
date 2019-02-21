package de.xenyria.splatoon.ai.navigation;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.target.NavigationTarget;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;
import de.xenyria.splatoon.ai.pathfinding.worker.PathfindingManager;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIWeaponRoller;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.InkRail;
import de.xenyria.splatoon.game.objects.RideRail;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Navigation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;

public class NavigationManager {

    public void dbgAddPoint(int x, int y, int z, TransitionType type) {

        remainingNavigationPoints.add(new NavigationPoint(x, y, z, type, new HashMap<>()));

    }

    private int ticksSincePathFound = 0;

    private ArrayList<NavigationPoint> remainingNavigationPoints = new ArrayList<>();
    public ArrayList<NavigationPoint> readNextPointChain(TransitionType type, int amount) {

        ArrayList<NavigationPoint> foundPoints = new ArrayList<>();
        for(int i = 0; i < remainingNavigationPoints.size(); i++) {

            NavigationPoint point = remainingNavigationPoints.get(i);
            if(point.getTransitionType() == type) {

                foundPoints.add(point);
                if(foundPoints.size() >= amount) {

                    return foundPoints;

                }

            } else {

                return foundPoints;

            }

        }
        return foundPoints;

    }

    private boolean newNavpointFlag = false;
    public NavigationPoint nextNavigationPoint() {

        NavigationPoint found = null;
        npc.targetYForSwim = 0d;

        while (!remainingNavigationPoints.isEmpty()) {

            found = remainingNavigationPoints.get(0);
            double horRange = .5d, vertRange=0.1d;
            if(npc.getEquipment().getSpecialWeapon() != null && npc.getEquipment().getSpecialWeapon() instanceof Baller) {

                if(npc.getEquipment().getSpecialWeapon().isActive()) {

                    horRange = 0.75d;
                    vertRange = 256d;

                }

            }

            if(!found.isReached(npc.getLocation(), horRange, vertRange) && !skipPoint) {

                return found;

            } else {

                if(skipPoint) {

                    skipPoint = false;
                    remainingNavigationPoints.remove(0);
                    return null;

                }
                ticksSinceLastNewNavigationPoint = 0;
                remainingNavigationPoints.remove(0);
                npc.disableWallSwimMode();
                if(!remainingNavigationPoints.isEmpty()) {

                    ticksSinceLastNodeProgress = 0;
                    distanceToNodeFinish = npc.getLocation().distance(remainingNavigationPoints.get(0).toVector().toLocation(npc.getWorld()));
                    newNavpointFlag = true;
                    return remainingNavigationPoints.get(0);

                }

            }

        }
        return null;

    }

    private SquidAStar pathfindRequest = null;

    private TransitionType[] COMPLEX_TRANSITIONS = new TransitionType[] {

            TransitionType.RIDE_RAIL, TransitionType.INK_RAIL,
            TransitionType.JUMP_TO, TransitionType.SWIM_WALL_VERTICAL, TransitionType.ENTER_FOUNTAIN,
            TransitionType.SWIM_BLOCKED, TransitionType.SWIM_DRY

    };
    private int ticksSinceLastNewNavigationPoint;
    public boolean isStuck() { return ticksSinceLastNodeProgress > 20; }
    public boolean isDoingComplexTask() {

        if(skipPoint) {

            return false;

        }

        boolean isDoingTask = false;
        if(!remainingNavigationPoints.isEmpty()) {

            NavigationPoint point = remainingNavigationPoints.get(0);
            for(TransitionType transitionType : COMPLEX_TRANSITIONS) {

                if(point.getTransitionType() == transitionType) {

                    isDoingTask = true;
                    break;

                }

            }

            if(isDoingTask) {

                if(ticksSinceLastNewNavigationPoint > 40) {

                    // Timeout
                    return false;

                } else {

                    return true;

                }

            }

        }

        return false;

    }

    public static double center(int val) {

        //if(val >= 0) {

            return val + .5;

        //}

    }

    public static NodePath DEBUG_PATH = null;

    private double distanceToNodeFinish = 0d;
    private int ticksSinceLastNodeProgress = 0;

    public void revalidateSwimNodes() {

        for(NavigationPoint point : remainingNavigationPoints) {

            if(point.getTransitionType() == TransitionType.SWIM && !point.getData().containsKey("squidNeeded") && point.getData().containsKey("blockRef")) {

                BlockPosition position = (BlockPosition) point.getData().get("blockRef");
                if(!npc.getMatch().isOwnedByTeam(npc.getWorld().getBlockAt(
                        position.getX(), position.getY(), position.getZ()
                ), npc.getTeam())) {

                    point.updateTransitionType(TransitionType.WALK);

                }

            }

        }

    }

    private boolean squidOnNavigationFinish = false;
    public void setSquidOnNavigationFinish(boolean val) { squidOnNavigationFinish = val; }
    public boolean isSquidOnNavigationFinish() { return squidOnNavigationFinish; }

    public void tick() {

        if(!disabled) {

            String debugStr = "";
            String str2 = ""+System.currentTimeMillis()+" ";

            str2+=ticksSinceLastNewNavigationPoint + " " + ticksSincePathFound + " ";

            if(pathfindRequest != null) {

                str2+=pathfindRequest.getRequestResult();

            }

            if(!remainingNavigationPoints.isEmpty()) {

                NavigationPoint point = remainingNavigationPoints.get(0);
                debugStr+=point.x + " " + point.toVector().getY() + " " + point.z + " || ";

            }

            // Vergangene Ticks seit dem letzten neuen Navigationspunkt: Soll den Stuck-Status verhindern
            ticksSinceLastNewNavigationPoint++;
            ticksSincePathFound++;

            revalidateSwimNodes();
            if (remainingNavigationPoints.isEmpty()) {

                if (npc.hasControl()) {

                    if (!isSquidOnNavigationFinish()) {

                        if (npc.isSquid()) {

                            if (!npc.isSubmergedInInk()) {

                                if (npc.getLastFormChangeTicks() > 5) {

                                    npc.leaveSquidForm();

                                }

                            }

                        } else {

                            if (npc.onTeamTerritory()) {

                                if (npc.getLastFormChangeTicks() > 5) {

                                    npc.enterSquidForm();

                                }

                            }

                        }

                    }

                } else {

                    if (!npc.isSquid()) {

                        npc.enterSquidForm();

                    }

                }

            }

            boolean updateRequired = (target != null && (target.needsUpdate(npc.getLocation().toVector()) || isStuck()));
            if (!updateRequired) {

                if (pathfindRequest != null) {

                    SquidAStar.RequestResult requestResult = pathfindRequest.getRequestResult();
                    if (requestResult != SquidAStar.RequestResult.FOUND && requestResult != SquidAStar.RequestResult.PROCESSING) {

                        updateRequired = true;

                    }

                }

            }

            if (ticksSincePathFound > 7 && target != null && updateRequired) {

                ticksSincePathFound = 0;
                pathfindRequest = new SquidAStar(npc.getLocation().getWorld(), npc.getLocation().toVector(), target, npc.getMatch(), npc.getTeam(), target.maxNodeVisits());

                target.beginPathfinding();
                pathfindRequest.updateCapabilities(target.getMovementCapabilities());
                if (target.getNodeListener() != null) {

                    pathfindRequest.updateNodeListener(target.getNodeListener());

                }
                PathfindingManager.queueRequest(pathfindRequest);

            } else if ((pathfindRequest != null && (pathfindRequest.getRequestResult() == SquidAStar.RequestResult.NOT_FOUND || pathfindRequest.getRequestResult() == SquidAStar.RequestResult.ERROR))) {

                currentTargetFailureCount++;

            }

            if (pathfindRequest != null && target != null && pathfindRequest.getRequestResult() == SquidAStar.RequestResult.FOUND) {

                if (!isDoingComplexTask() || isStuck() || skipPoint) {

                    NodePath path = pathfindRequest.getNodePath();

                    remainingNavigationPoints.clear();
                    boolean ignore = true;

                    for (NodePath.NodePosition position : path.getNodes()) {

                        NavigationPoint point = new NavigationPoint(position.x, position.y + position.blockHeight, position.z, position.getType(), position.getData());
                        remainingNavigationPoints.add(point);

                    }

                    ticksSinceLastNodeProgress = 0;
                    if (!remainingNavigationPoints.isEmpty()) {

                        distanceToNodeFinish = npc.getLocation().distance(remainingNavigationPoints.get(0).toVector().toLocation(npc.getWorld()));

                    } else {

                        distanceToNodeFinish = 999d;

                    }
                    pathfindRequest = null;
                    currentTargetFailureCount = 0;
                    npc.disableWallSwimMode();

                }

            }

            if (!remainingNavigationPoints.isEmpty()) {


                NavigationPoint point = nextNavigationPoint();
                if (point != null) {

                    NavigationPoint point1 = remainingNavigationPoints.get(remainingNavigationPoints.size() - 1);

                    Vector target = point.toVector().setY(0);
                    Vector current = npc.getLocation().toVector().setY(0);
                    Vector direction = target.clone().subtract(current).normalize();

                    if (VectorUtil.isValid(direction)) {

                        Vector movementDirection = new Vector(direction.getX(), 0, direction.getZ());
                        Vector origMovement = movementDirection.clone().multiply(npc.getMovementSpeed());
                        double npcSpeed = npc.getMovementSpeed();
                        double distance = npc.getLocation().toVector().distance(point.toVector());
                        if (distance <= npcSpeed) {

                            npcSpeed = distance;

                        }

                        movementDirection = movementDirection.multiply(npcSpeed);
                        if (npc.isRidingARail() && (point.getTransitionType() != TransitionType.RIDE_RAIL && point.getTransitionType() != TransitionType.INK_RAIL)) {

                            npc.forceRailLeave();
                            return;

                        }

                        if (point.getTransitionType() == TransitionType.WALK ||
                                point.getTransitionType() == TransitionType.FALL ||
                                point.getTransitionType() == TransitionType.ENTER_FOUNTAIN ||
                                point.getTransitionType() == TransitionType.WALK_ENEMY ||
                                point.getTransitionType() == TransitionType.CLIMB ||
                                point.getTransitionType().isRollNode()) {

                            TransitionType type = point.getTransitionType();
                            if (type.isRollNode()) {

                                AIWeaponManager.AIPrimaryWeaponType type1 = npc.getWeaponManager().getAIPrimaryWeaponType();
                                if (type1 == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                                    AIWeaponRoller roller = (AIWeaponRoller) npc.getWeaponManager().getAIWeaponInterface();
                                    if (!roller.isRolling()) {

                                        Location loc1 = npc.getLocation().clone();
                                        loc1.setPitch(0f);

                                        Vector inFront = npc.getEyeLocation().clone().add(loc1.getDirection().multiply(npc.getWeaponManager().maxWeaponDistance())).toVector();
                                        npc.getWeaponManager().aim(inFront);
                                        npc.getWeaponManager().fire(roller.getTicksToRoll());

                                    } else {

                                        npc.getWeaponManager().fire(10);

                                    }

                                }

                            }

                            if (point.getTransitionType() == TransitionType.ENTER_FOUNTAIN) {

                                ticksSinceLastNewNavigationPoint = 0;
                                ticksSincePathFound = 0;

                            }

                            if (npc.isSquid()) {
                                npc.leaveSquidForm();
                            }

                            Vector before = npc.getLocation().toVector();
                            npc.move(movementDirection.getX(), 0, movementDirection.getZ());
                            Vector after = npc.getLocation().toVector();
                            if(before.distance(after) <= 0.0125d) {

                                Vector plannedPos = before.add(new Vector(movementDirection.getX(), 0, movementDirection.getZ()));
                                Block block = npc.getWorld().getBlockAt((int)plannedPos.getX(), (int)plannedPos.getY(), (int)plannedPos.getZ());
                                if(AABBUtil.isPassable(block.getType()) && AABBUtil.isPassable(block.getRelative(BlockFace.UP).getType())) {

                                    npc.enterSquidForm();

                                }

                            }

                            if (newNavpointFlag && lookInWalkingDirection) {

                                Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                                location.setDirection(origMovement);
                                npc.updateAngles(location.getYaw(), location.getPitch());

                            }

                        } else if (point.getTransitionType() == TransitionType.JUMP_TO) {

                            if (npc.isSquid()) {
                                npc.leaveSquidForm();
                            }

                            // An's Ende der Sprungkette gehen
                            Vector targetPosition = target.clone();
                            Vector currentPosition = current.clone();

                            targetPosition.setY(0);
                            currentPosition.setY(0);

                            Vector jumpDir = targetPosition.clone().subtract(currentPosition).normalize().multiply(npc.getMovementSpeed());

                            if (npc.canJump()) {

                                npc.jump(.17);
                                npc.move(jumpDir.getX(), 0, jumpDir.getZ());

                            }

                            if (!npc.isOnGround()) {

                                npc.move(jumpDir.getX(), 0, jumpDir.getZ());

                            }
                            if (newNavpointFlag && lookInWalkingDirection) {

                                Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                                location.setDirection(origMovement);
                                npc.updateAngles(location.getYaw(), location.getPitch());

                            }

                        } else if (point.getTransitionType() == TransitionType.SWIM || point.getTransitionType() == TransitionType.SWIM_DRY || point.getData().containsKey("nextSwim") || point.getData().containsKey("squidNeeded") || point.getTransitionType() == TransitionType.SWIM_BLOCKED) {

                            if (!npc.isSquid() && npc.isSquidFormAvailable()) {

                                if (point.getTransitionType() == TransitionType.SWIM) {

                                    boolean onInk = npc.isOnOwnInk();
                                    if (onInk) {

                                        npc.enterSquidForm();

                                    }

                                } else {

                                    npc.enterSquidForm();

                                }

                            }

                            npc.move(movementDirection.getX(), 0, movementDirection.getZ());
                            if (newNavpointFlag && lookInWalkingDirection) {

                                Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                                location.setDirection(origMovement);
                                npc.updateAngles(location.getYaw(), location.getPitch());

                            }

                        } else if (point.getTransitionType() == TransitionType.INK_RAIL || point.getTransitionType() == TransitionType.RIDE_RAIL) {

                            // Verhindern, dass eine Anfrage eine falsche Position verwendet
                            ticksSinceLastNewNavigationPoint = 0;
                            ticksSincePathFound = 0;

                            boolean hasRemovedNode = false;
                            if (!npc.isSquid() && npc.isSquidFormAvailable()) {
                                npc.enterSquidForm();
                            }
                            if (!npc.isRidingARail()) {

                                npc.move(movementDirection.getX(), 0, movementDirection.getZ());
                                if (point.getData().containsKey("objID")) {

                                    int objID = (int) point.getData().get("objID");
                                    GameObject object = npc.getMatch().getObject(objID);
                                    if (point.getTransitionType() == TransitionType.INK_RAIL) {

                                        npc.beginRidingInkRail((InkRail) object);

                                    } else if (point.getTransitionType() == TransitionType.RIDE_RAIL) {

                                        npc.beginRidingRideRail((RideRail) object);

                                    }
                                    hasRemovedNode = true;
                                    remainingNavigationPoints.remove(0);

                                }

                            }

                            boolean endFlag = !npc.isRidingARail();
                            if (!point.getData().containsKey("objID") || endFlag) {

                                // Sekundärer Knoten - Ende der Schiene
                                boolean atEnd = npc.isAtEndOfRail();
                                if (endFlag && !atEnd) {

                                    if (!hasRemovedNode) {

                                        remainingNavigationPoints.remove(0);

                                    }

                                    if (npc.isSquid()) {
                                        npc.leaveSquidForm();
                                    }

                                }

                                if (atEnd) {

                                    if (!hasRemovedNode) {

                                        remainingNavigationPoints.remove(0);

                                    }
                                    if (npc.isRidingOnInkRail()) {

                                        npc.ejectFromInkRail(false);

                                    } else {

                                        npc.ejectFromRideRail();

                                    }
                                    if (npc.isSquid()) {
                                        npc.leaveSquidForm();
                                    }

                                }

                            }

                        } else if (point.getTransitionType() == TransitionType.SWIM_WALL_VERTICAL) {

                            boolean invalid = false;
                            ticksSinceLastNewNavigationPoint = 0;
                            ticksSincePathFound = 0;
                            if (point.getData().containsKey("wallNodes")) {

                                ArrayList<BlockPosition> positions = (ArrayList<BlockPosition>) point.getData().get("wallNodes");
                                if (!positions.isEmpty()) {

                                    for (BlockPosition position : positions) {

                                        if (!npc.getMatch().isOwnedByTeam(npc.getWorld().getBlockAt(
                                                position.getX(), position.getY(), position.getZ()
                                        ), npc.getTeam())) {

                                            invalid = true;
                                            npc.disableWallSwimMode();
                                            break;

                                        }

                                    }

                                }

                            }
                            if (invalid) {

                                invalidateCurrentPoint();
                                return;

                            }

                            if (!npc.isSquid() && npc.isSquidFormAvailable()) {
                                npc.enterSquidForm();
                            }
                            npc.targetYForSwim = point.toVector().getY() + 1;
                            if (!npc.isSwimmingOnWall()) {

                                if (npc.canSwimWall()) {

                                    npc.enableWallSwimMode();

                                }

                            }
                            if (npc.isSquid() && npc.isSwimmingOnWall()) {

                                npc.move(origMovement.getX() * .3, 0.017, origMovement.getZ() * .3);
                                if (point.isReached(npc.getLocation(), .25d, .2d)) {

                                    npc.disableWallSwimMode();

                                }

                                Location location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                                location.setDirection(origMovement);
                                npc.updateAngles(location.getYaw(), location.getPitch());

                            }

                        }

                    }

                }

                if (!remainingNavigationPoints.isEmpty()) {

                    double distToTarget = npc.getLocation().distance(remainingNavigationPoints.get(0).toVector().toLocation(npc.getWorld()));
                    if (distToTarget < distanceToNodeFinish) {

                        ticksSinceLastNodeProgress = 0;
                        distanceToNodeFinish = distToTarget;

                    } else {

                        ticksSinceLastNodeProgress++;

                    }

                }

            } else {

                ticksSinceLastNewNavigationPoint = 0;

            }

            newNavpointFlag = false;
            //npc.diagnosticStand1.setCustomName(str2 + " ");

        }

    }

    private boolean skipPoint = false;
    public void invalidateCurrentPoint() {

        skipPoint = true;

    }

    private boolean lookInWalkingDirection = true;

    private EntityNPC npc;
    public NavigationManager(EntityNPC npc) {

        this.npc = npc;

    }

    private int currentTargetFailureCount = 0;
    public int getCurrentTargetFailureCount() { return currentTargetFailureCount; }

    private PathfindingTarget target;
    public void setTarget(PathfindingTarget pathfindingTarget) {

        currentTargetFailureCount = 0;
        this.target = pathfindingTarget;

    }

    public PathfindingTarget getTarget() { return target; }
    public void resetTarget() {

        squidOnNavigationFinish = false;
        ticksSincePathFound = 0;
        ticksSinceLastNodeProgress = 0;
        ticksSinceLastNewNavigationPoint = 0;
        currentTargetFailureCount = 0;
        target = null;
        remainingNavigationPoints.clear();
        pathfindRequest = null;
        disabled = false;

    }

    public void disableLookInDirection() {

        lookInWalkingDirection = false;

    }

    public boolean doLookInDirection() {

        return lookInWalkingDirection;

    }

    public void enableLookInDirection() {

        lookInWalkingDirection = true;

    }

    public boolean hasReachedTarget(int i) {

        return remainingNavigationPoints.size() <= i;

    }

    public void useExistingPathfinder(SquidAStar path) {

        this.pathfindRequest = path;

    }

    public boolean isDone() {

        return remainingNavigationPoints.isEmpty();

    }

    private boolean disabled = false;
    public void disable() {

        disabled = true;
        disableLookInDirection();

    }

    public void enable() {

        disabled = false;
        enableLookInDirection();

    }

    public void removeFirstNavigationPoint() {

        if(!remainingNavigationPoints.isEmpty()) {

            remainingNavigationPoints.remove(0);

        }

    }

}
