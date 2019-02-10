package de.xenyria.splatoon.ai.pathfinding;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.grid.NodeGrid;
import de.xenyria.splatoon.ai.pathfinding.heap.Heap;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.ai.MatchAIManager;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.BlockPosition;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class SquidAStar {

    private World world;
    public World getWorld() { return world; }

    private Node startNode;
    public Node getStartNode() { return startNode; }

    private Vector targetVector;
    public Vector getTargetVector() { return targetVector; }

    private PathfindingTarget target;
    private int maxNodeVisits;

    private Heap openList = null;
    private NodeGrid grid;

    private Team team;
    public Team getTeam() { return team; }

    private Match match;
    public Match getMatch() { return match; }

    private PathfindingTarget.NodeListener nodeListener;
    public PathfindingTarget.NodeListener getNodeListener() { return nodeListener; }

    public void updateNodeListener(PathfindingTarget.NodeListener listener) {

        this.nodeListener = listener;
        if(nodeListener.useAlternativeTargetCheck()) {

            noTargetCheck = true;

        }

    }

    public void updateCapabilities(MovementCapabilities movementCapabilities) {

        this.capabilities = movementCapabilities;

    }

    public static class MovementCapabilities {

        public boolean squidFormUsable = true;
        public boolean climbEveryWall = false;
        public boolean exitAsHuman = true;
        public boolean canRoll = false;
        public boolean walkOnEnemyTurf = true;
        public int requiredNodesToSwim = 1;

    }

    public static interface GameObjectSnapshot {

        int getID();

    }

    public ArrayList<Node> lastNodes(Node baseNode, int depth) {

        int size = 1;
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(baseNode);

        Node cursor = baseNode.getParent();
        while (cursor != null && size < depth) {

            nodes.add(cursor);
            size++;

        }
        return nodes;

    }

    public static class GusherSnapshot implements GameObjectSnapshot {

        public Team team;
        public int targetHeight;
        private Vector position;

        public GusherSnapshot(int id, Vector position, int gainableHeight, Team team) {

            this.id = id;
            this.position = position;
            this.team = team;
            this.targetHeight = gainableHeight;

        }

        private int id;
        public int getID() { return id; }

    }

    private boolean noTargetCheck = false;
    public void disableTargetCheck() { noTargetCheck = true; }

    public static class RailSnapshot implements GameObjectSnapshot {

        private ArrayList<Vector> vectors = new ArrayList<>();
        private ArrayList<AxisAlignedBB> boundingBoxes = new ArrayList<>();
        private AxisAlignedBB boundary;

        private ObjectType type;
        public RailSnapshot(int id, ObjectType type, Team team, Vector... vectors1) {

            this.id = id;
            this.type = type;
            this.owningTeam = team;
            for(Vector vector : vectors1) { vectors.add(vector); }
            calculateBounds();

        }

        private int id;
        public int getID() { return id; }

        private int minX,minY,minZ,maxX,maxY,maxZ;
        public Team owningTeam;

        public void calculateBounds() {

            for(Vector vector : vectors) {

                if(vector.getX() < minX) { minX = (int) vector.getX(); }
                if(vector.getY() < minY) { minY = (int) vector.getY(); }
                if(vector.getZ() < minZ) { minZ = (int) vector.getZ(); }
                if(vector.getX() > maxX) { maxX = (int) vector.getX(); }
                if(vector.getY() > maxY) { maxY = (int) vector.getY(); }
                if(vector.getZ() > maxZ) { maxZ = (int) vector.getZ(); }

                boundingBoxes.add(new AxisAlignedBB(vector.getX() - .5, vector.getY() - .5, vector.getZ() - .5, vector.getX() + .5, vector.getY() + .5, vector.getZ() + .5));

            }
            boundary = new AxisAlignedBB(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);

        }

    }

    private ArrayList<RailSnapshot> railSnapshots = new ArrayList<>();
    private ArrayList<GusherSnapshot> gusherSnapshots = new ArrayList<>();

    private MovementCapabilities capabilities = null;

    private Node goalNode;
    public Node getGoalNode() { return goalNode; }

    private Vector begin;

    public SquidAStar(World world, Vector begin, PathfindingTarget target, Match match, @Nullable Team team, int maxNodeVisits) {

        this.begin = begin;
        this.world = world;
        this.team = team;
        this.match = match;
        this.target = target;
        this.maxNodeVisits = maxNodeVisits;
        this.targetVector = target.getEstimatedPosition();
        this.grid = new NodeGrid(this);
        copySnapshotsFrom(match.getAIController().getSnapshot());

        if(capabilities == null) {

            capabilities = new MovementCapabilities();

        }

        openList = new Heap(maxNodeVisits * 3);

        if(target.useGoalNode()) {

            goalNode = grid.getNode((int)target.getEstimatedPosition().getBlockX(), (int)target.getEstimatedPosition().getBlockY(), (int)target.getEstimatedPosition().getBlockZ());
            NodeGrid.Result result = grid.isValidPosition(goalNode, dimensions);
            if(result != NodeGrid.Result.OK && result != NodeGrid.Result.OK_SQUID) {

                requestResult = RequestResult.NOT_FOUND;

            }

        }

        if(target.getNodeListener() != null) {

            updateNodeListener(target.getNodeListener());

        }

        startNode = grid.getNode((int)begin.getBlockX(), (int)begin.getBlockY(), (int)begin.getBlockZ());
        if(capabilities.squidFormUsable && team != null && match.isOwnedByTeam(world.getBlockAt(startNode.x, startNode.y - 1, startNode.z), team)) {

            if(startNode.getData().getHeight() == 0d && capabilities.requiredNodesToSwim == 0) {

                startNode.setType(TransitionType.SWIM);

            } else {

                startNode.setType(TransitionType.WALK);

            }

        }
        NodeGrid.Result result = grid.isValidPosition(startNode, dimensions);
        System.out.println("Result for StartNode: " + result);
        if(result == NodeGrid.Result.NO_SPACE) {

            if(grid.isValidInSquidForm(startNode.x, startNode.y, startNode.z, dimensions)) {

                System.out.println("Squid Valid");
                Node below = grid.getNode(startNode.x, startNode.y - 1, startNode.z);
                if(below.getData().getHeight() == 1d) {

                    System.out.println("Below full");
                    if(nodeListener != null && !nodeListener.isPassable(startNode, startNode.x, startNode.y, startNode.z)) {

                        requestResult = RequestResult.NOT_FOUND;
                        return;

                    }

                    Block below1 = world.getBlockAt(startNode.x, startNode.y - 1, startNode.z);
                    Block above = world.getBlockAt(startNode.x, startNode.y + 1, startNode.z);
                    if(AABBUtil.isPassable(above.getType())) {

                        if (match.isOwnedByTeam(below1, team)) {

                            startNode.setType(TransitionType.SWIM_BLOCKED);
                            startNode.getAdditionalData().put("squidNeeded", true);
                            result = NodeGrid.Result.OK_SQUID;

                        } else {

                            if(!match.isEnemyTurf(below1, team)) {

                                startNode.getAdditionalData().put("nextSwim", true);
                                startNode.getAdditionalData().put("squidNeeded", true);
                                startNode.setType(TransitionType.SWIM_DRY);
                                result = NodeGrid.Result.OK_SQUID;

                            }

                        }

                    }

                }

            }

        }

        if(result == NodeGrid.Result.NO_FLOOR) {

            // Gushers
            boolean found = false;
            for(GusherSnapshot snapshot : gusherSnapshots) {

                if(team == null || snapshot.team == team) {

                    int tX = (int) snapshot.position.getX();
                    int tY = (int) snapshot.position.getY();
                    int tZ = (int) snapshot.position.getZ();

                    double dist = Node.distance(startNode.x, startNode.y, startNode.z, tX, tY, tZ);
                    if(dist <= 1) {

                        Node targetNode = grid.getNode(
                                (int)snapshot.position.getX(),
                                snapshot.targetHeight - 1,
                                (int)snapshot.position.getZ());

                        targetNode.setType(TransitionType.ENTER_FOUNTAIN);
                        startNode = targetNode;
                        found = true;

                    }

                }

            }

            if(!found) {

                Node alternativeNode = grid.fallCheck(startNode.x, startNode.y, startNode.z, dimensions);
                if (alternativeNode != null) {

                    alternativeNode.setType(startNode.getType());
                    startNode = alternativeNode;

                } else {

                    //requestResult = RequestResult.NOT_FOUND;

                }

            }

        }
        startNode.setFirst(true);
        openList.add(startNode);

    }

    public void copySnapshotsFrom(MatchAIManager.GameObjectSnapshot snapshot) {

        gusherSnapshots.addAll(snapshot.getGusherSnapshots());
        railSnapshots.addAll(snapshot.getRailSnapshots());

    }

    public static enum RequestResult {

        PROCESSING,
        FOUND,
        ERROR,
        NOT_FOUND;

    }

    private RequestResult requestResult = RequestResult.PROCESSING;
    public RequestResult getRequestResult() { return requestResult; }

    private NodePath nodePath;
    public NodePath getNodePath() { return nodePath; }

    private int processedNodes = 0;
    public boolean isValidExitNodeType(TransitionType type) {

        return type != TransitionType.INK_RAIL && type != TransitionType.RIDE_RAIL && type != TransitionType.FALL && type != TransitionType.JUMP_TO && type != TransitionType.ENTER_FOUNTAIN && type != TransitionType.SWIM_BLOCKED && type != TransitionType.SWIM_DRY;

    }
    private ArrayList<Node> processedNodeList = new ArrayList<>();

    public void beginProcessing() {

        if(capabilities == null) {

            capabilities = new MovementCapabilities();

        }
        if(requestResult != RequestResult.NOT_FOUND) {

            boolean found = false;
            while (processedNodes < maxNodeVisits && !found && !openList.isEmpty()) {

                try {

                    Node node = (Node) openList.removeFirst();
                    processedNodes++;
                    node.close();
                    processedNodeList.add(node);
                    boolean reached = false;
                    reached = isValidExitNodeType(node.getType()) && target.isReached(this, node, node.toVector());

                    if(reached && capabilities.exitAsHuman) {

                        TransitionType type = node.getType();
                        if(type == TransitionType.SWIM) {

                            if(node.getAdditionalData().containsKey("squidNeeded")) {

                                reached = false;

                            } else {

                                node.setType(TransitionType.WALK);

                            }

                        }

                    }

                    if (reached) {

                        nodePath = new NodePath(node);
                        requestResult = RequestResult.FOUND;
                        found = true;

                    } else {

                        for (Node successor : getNeighbours(node)) {

                            if (!successor.isClosed()) {

                                boolean hasSuccessor = openList.contains(successor);
                                double additionalWeight = 0d;
                                if(nodeListener != null) { additionalWeight = nodeListener.getAdditionalWeight(successor); }

                                double tentativeG = node.gCost + Node.distance(node, successor) + (node.getType().getWeight() + additionalWeight);
                                if (!hasSuccessor || tentativeG < successor.gCost) {

                                    successor.gCost = tentativeG;
                                    successor.setParent(node);

                                    if (!hasSuccessor) {

                                        openList.add(successor);

                                    } else {

                                        openList.updateItem(successor);

                                    }

                                    //Viewport.GLOBAL_VIEWPORT.spawnParticle(RPGPlayer.getPlayers().get(0), Particle.END_ROD, new Location(Bukkit.getWorld("debug"), successor.getX()+.5, successor.getY()+.2, successor.getZ()+.5), 0);

                                }

                            }

                        }

                    }

                } catch (Exception e) {

                    e.printStackTrace();
                    requestResult = RequestResult.ERROR;

                }

            }

            if(!found && requestResult == RequestResult.PROCESSING) {

                requestResult = RequestResult.NOT_FOUND;

            }

        }

        if(requestResult == RequestResult.NOT_FOUND && noTargetCheck) {

            Node node = nodeListener.getBestNodeFromRemaining(processedNodeList.toArray(new Node[]{}));
            if(node != null) {

                nodePath = new NodePath(node);
                requestResult = RequestResult.FOUND;

            }

        }

    }


    private NodeGrid.BoundingBoxDimensions dimensions = new NodeGrid.BoundingBoxDimensions();
    public ArrayList<Node> getNeighbours(Node node) {

        ArrayList<Node> nodes = new ArrayList<>();
        boolean ridingNode = node.getType() == TransitionType.INK_RAIL || node.getType() == TransitionType.RIDE_RAIL;
        for(int x = -1; x <= 1; x++) {

            for(int y = -1; y <= 1; y++) {

                for (int z = -1; z <= 1; z++) {

                    int nX = node.x + x;
                    int nY = node.y + y;
                    int nZ = node.z + z;

                    double nCenX = nX + .5, nCenZ = nZ + .5;

                    if(x == 0 && y == 0 && z == 0) { continue; }
                    boolean diagonal = (Math.abs(x) * Math.abs(z)) != 0;
                    if(diagonal && y != 0) { continue; }
                    //if(node.getParent() != null) {

                        if(node.getType() == TransitionType.ENTER_FOUNTAIN) {

                            if(y != 0 || diagonal || (x == 0 && z == 0)) { continue; }

                        }

                    //}

                    if(!ridingNode) {

                        NodeGrid.Result nodeResult = grid.isValidPosition(nX, nY, nZ, dimensions);
                        boolean acceptable = nodeResult == NodeGrid.Result.OK;

                        if (acceptable) {

                            Node currentNode = grid.getNode(nX, nY, nZ);
                            if(nodeListener != null) {

                                if(!nodeListener.isPassable(currentNode, nX,nY,nZ)) {

                                    continue;

                                }

                            }
                            if(currentNode.isClosed()) { continue; }

                            double delta1 = node.totalHeight() - currentNode.totalHeight();
                            if(diagonal && delta1 != 0d) { continue; }

                            Block block = world.getBlockAt(nX, nY - 1, nZ);

                            boolean ownTeam = false;
                            boolean otherTeam = false;
                            if(team != null) {

                                ownTeam = match.isOwnedByTeam(block, team);
                                if (!ownTeam) {
                                    otherTeam = match.isEnemyTurf(block, team);
                                }

                            }

                            boolean withSquid = ownTeam;

                            AxisAlignedBB aabb = new AxisAlignedBB((nCenX) - (dimensions.width / 2), currentNode.totalHeight(), (nCenZ) - (dimensions.width / 2),
                                    (nCenX) + (dimensions.width / 2),
                                    currentNode.totalHeight() + dimensions.height,
                                    (nZ) + (dimensions.width / 2));

                            // Gusher
                            boolean found = false;
                            for(GusherSnapshot snapshot : gusherSnapshots) {

                                if(team == null || snapshot.team == team) {

                                    int tX = (int) snapshot.position.getX();
                                    int tY = (int) snapshot.position.getY();
                                    int tZ = (int) snapshot.position.getZ();

                                    double dist = Node.distance(nX, nY, nZ, tX, tY, tZ);
                                    if(dist <= 1) {

                                        Node targetNode = grid.getNode(
                                                (int)snapshot.position.getX(),
                                                snapshot.targetHeight - 1,
                                                (int)snapshot.position.getZ());
                                            if(nodeListener != null) {

                                                if(!nodeListener.isPassable(targetNode, nX,nY,nZ)) {

                                                    continue;

                                                }

                                            }
                                            targetNode.setType(TransitionType.ENTER_FOUNTAIN);
                                            nodes.add(targetNode);
                                            found = true;


                                    }

                                }

                            }
                            if(found) { continue; }

                            if(((node.getType() != TransitionType.RIDE_RAIL) && (node.getType() != TransitionType.INK_RAIL))) {

                                // Ride/InkRails
                                for (RailSnapshot snapshot : railSnapshots) {

                                    if (team == null || snapshot.owningTeam == team) {

                                        if (snapshot.boundary.c(aabb)) {

                                            // Trifft ein Vektor zu?
                                            for (int i = 0; i < snapshot.vectors.size(); i++) {

                                                Vector vector = snapshot.vectors.get(i);
                                                double dist = vector.distance(currentNode.toVector());
                                                if (dist < 3) {

                                                    AxisAlignedBB vectorBB = snapshot.boundingBoxes.get(i);
                                                    //world.spawnParticle(Particle.VILLAGER_ANGRY, vectorBB.minX, vectorBB.minY, vectorBB.minZ, 0);
                                                    //world.spawnParticle(Particle.VILLAGER_ANGRY, vectorBB.maxX, vectorBB.maxY, vectorBB.maxZ, 0);
                                                    if (aabb.c(vectorBB) || vectorBB.c(aabb)) {

                                                        if (snapshot.type == ObjectType.RIDE_RAIL) {

                                                            currentNode.setType(TransitionType.RIDE_RAIL);

                                                        } else {

                                                            currentNode.setType(TransitionType.INK_RAIL);

                                                        }

                                                        currentNode.getAdditionalData().put("railIndex", railSnapshots.indexOf(snapshot));
                                                        currentNode.getAdditionalData().put("objID", snapshot.getID());
                                                        nodes.add(currentNode);
                                                        found = true;
                                                        break;

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                            if(found) { continue; }

                            if (diagonal) {

                                if (grid.isValidPosition(node.x + x, node.y, node.z, dimensions) != NodeGrid.Result.OK) { continue; }
                                if (grid.isValidPosition(node.x, node.y, node.z + z, dimensions) != NodeGrid.Result.OK) { continue; }

                            }
                            if (y != 0) {

                                if (y > 0) {

                                    double cenX = node.x;
                                    double cenZ = node.z;
                                    if (!grid.hasSpace(new AxisAlignedBB(cenX - (dimensions.width / 2),
                                            nY, cenZ - (dimensions.width / 2), cenX + (dimensions.width / 2), nY + dimensions.height, cenZ + (dimensions.width / 2)), false)) {
                                        continue;
                                    }

                                } else {

                                    double cenX = nCenX;
                                    double cenZ = nCenZ;
                                    if (!grid.hasSpace(new AxisAlignedBB(cenX - (dimensions.width / 2),
                                            nY, cenZ - (dimensions.width / 2), cenX + (dimensions.width / 2), nY + dimensions.height, cenZ + (dimensions.width / 2)), false)) {
                                        continue;
                                    }


                                }

                            }

                            TransitionType oldType = currentNode.getType();

                            boolean swimable = ownTeam;
                            if(diagonal) {

                                Block diagBlock1 = world.getBlockAt(node.x + x, nY, nZ);
                                Block diagBlock2 = world.getBlockAt(nX, nY, node.z + z);

                                if(match.isOwnedByTeam(diagBlock1, team) && match.isOwnedByTeam(diagBlock2, team)) {

                                    swimable = ownTeam;

                                } else {

                                    boolean enemy1 = match.isEnemyTurf(diagBlock1, team), enemy2 = match.isEnemyTurf(diagBlock2, team);
                                    if(enemy1 || enemy2) {

                                        otherTeam = true;

                                    }

                                    swimable = false;

                                }

                            }

                            if (otherTeam) {

                                if(capabilities.canRoll) {

                                    currentNode.setType(TransitionType.ROLL_ENEMY_TURF);

                                } else {

                                    if(capabilities.walkOnEnemyTurf) {

                                        currentNode.setType(TransitionType.WALK_ENEMY);

                                    } else {

                                        continue;

                                    }

                                }

                            } else {

                                if (swimable) {

                                    if (capabilities.squidFormUsable && currentNode.getData().getHeight() == 0d) {

                                        ArrayList<Node> nodesBefore = lastNodes(node, capabilities.requiredNodesToSwim);
                                        int streak = 0;
                                        for(Node node1 : nodesBefore) {

                                            if(node1.getAdditionalData().containsKey("squidUsable")) {

                                                streak++;

                                            } else {

                                                streak = 0;
                                                break;

                                            }

                                        }

                                        currentNode.getAdditionalData().put("squidUsable", true);
                                        currentNode.getAdditionalData().put("blockRef",
                                                new BlockPosition(block.getX(), block.getY(), block.getZ()));

                                        if(streak >= capabilities.requiredNodesToSwim) {

                                            currentNode.setType(TransitionType.SWIM);

                                        } else {

                                            currentNode.setType(TransitionType.WALK);

                                        }

                                    } else {

                                        if(y == 1) {

                                            currentNode.setType(TransitionType.CLIMB);

                                        } else {

                                            currentNode.setType(TransitionType.WALK);

                                        }

                                    }

                                } else {

                                    boolean paintable = getMatch().isPaintable(block);
                                    if(!capabilities.canRoll && !paintable) {

                                        if (y == 1) {

                                            currentNode.setType(TransitionType.CLIMB);

                                        } else {

                                            currentNode.setType(TransitionType.WALK);

                                        }

                                    } else {

                                        if(capabilities.canRoll && paintable) {

                                            currentNode.setType(TransitionType.ROLL_UNPAINTED);

                                        } else {

                                            currentNode.setType(TransitionType.WALK);

                                        }

                                    }

                                }

                            }

                            if(node.getAdditionalData().containsKey("nextSwim")) {

                                currentNode.setType(TransitionType.SWIM);
                                currentNode.getAdditionalData().put("squidNeeded", true);

                            }


                            if(oldType == TransitionType.SWIM_WALL_VERTICAL) {

                                currentNode.setType(oldType);

                            } else {

                                double prevHeight = node.totalHeight();
                                double curHeight = currentNode.totalHeight();

                                if(curHeight > prevHeight) {

                                    double delta = curHeight-prevHeight;
                                    if(delta > 1d) { continue; }

                                }

                            }

                            nodes.add(currentNode);

                        } else if (nodeResult == NodeGrid.Result.NO_FLOOR) {

                            if (y >= 0) {

                                boolean trySwim = (node.getType() != TransitionType.ENTER_FOUNTAIN) && y > 0 && x == 0 && z == 0;
                                boolean tryJump = y == 0 || ((node.getType() == TransitionType.ENTER_FOUNTAIN));

                                if(trySwim) {

                                    // Wall Swim
                                    Node currentNode = grid.swimCheck(nX, nY, nZ, dimensions, team, this, capabilities.climbEveryWall);
                                    if (currentNode != null && !currentNode.isClosed()) {

                                        currentNode.setType(TransitionType.SWIM_WALL_VERTICAL);
                                        nodes.add(currentNode);
                                        tryJump = false;

                                    }

                                }

                                if (tryJump) {

                                    if (diagonal) { continue; }

                                    int jumpLength = 2;
                                    //if(node.getType() == TransitionType.ENTER_FOUNTAIN) {

                                        //jumpLength = 3;

                                    //}

                                    int targetJumpX = node.x + (x * jumpLength);
                                    int targetJumpY = node.y;
                                    int targetJumpZ = node.z + (z * jumpLength);

                                    NodeGrid.Result result = grid.isValidPosition(targetJumpX, targetJumpY, targetJumpZ, dimensions);

                                    if (result == NodeGrid.Result.OK) {

                                        boolean obstructed = false;
                                        for (int jL = 0; jL < jumpLength; jL++) {

                                            NodeGrid.Result result1 = grid.isValidPosition(node.x + (x * jL),
                                                    targetJumpY + 1, node.z + (z * jL), dimensions);
                                            if (result1 == NodeGrid.Result.NO_SPACE) {

                                                obstructed = true;
                                                break;

                                            }

                                        }

                                        if(!obstructed) {

                                            Node node1 = grid.getNode(targetJumpX, targetJumpY, targetJumpZ);
                                            if(!node1.isClosed()) {

                                                if(nodeListener != null) {

                                                    if(!nodeListener.isPassable(node1, nX,nY,nZ)) {

                                                        continue;

                                                    }

                                                }
                                                node1.setType(TransitionType.JUMP_TO);
                                                nodes.add(node1);

                                            }

                                        }

                                    } else if (result == NodeGrid.Result.NO_FLOOR) {

                                        Node target = grid.fallCheck(targetJumpX, targetJumpY, targetJumpZ, dimensions);
                                        if (target != null && !target.isClosed() && (target.x != node.x || target.y != node.y || target.z != node.z)) {

                                            target.setType(TransitionType.JUMP_TO);
                                            nodes.add(target);

                                        }

                                    } else if (result == NodeGrid.Result.NO_SPACE) {

                                        NodeGrid.Result above = grid.isValidPosition(targetJumpX, targetJumpY + 1, targetJumpZ, dimensions);
                                        if (above == NodeGrid.Result.OK) {

                                            boolean obstructed = false;
                                            if(!obstructed && (targetJumpX != node.x || targetJumpY+1 != node.y || targetJumpZ != node.z)) {

                                                Node node1 = grid.getNode(targetJumpX, targetJumpY + 1, targetJumpZ);
                                                if(!node1.isClosed()) {

                                                    if(nodeListener != null) {

                                                        if(!nodeListener.isPassable(node1, nX,nY,nZ)) {

                                                            continue;

                                                        }

                                                    }

                                                    node1.setType(TransitionType.JUMP_TO);
                                                    nodes.add(node1);

                                                }

                                            }

                                        }

                                    }

                                }

                            } else {

                                Node fallNode = grid.fallCheck(nX, nY, nZ, dimensions);
                                if(fallNode != null && !fallNode.isClosed()) {

                                    fallNode.setType(TransitionType.FALL);
                                    nodes.add(fallNode);

                                }

                            }

                        } else if(nodeResult == NodeGrid.Result.NO_SPACE) {

                            if(true) {

                                //System.out.println("Check @ " + nX + " " + nY + " " + nZ);
                                boolean squidPossible = grid.isValidInSquidForm(nX, nY, nZ, dimensions);
                                if(squidPossible) {

                                    //System.out.println("Squid pos!");
                                    Node below = grid.getNode(nX,nY-1,nZ);
                                    if(below.getData().getSquidHeight() == 1d) {

                                        //System.out.println("Squid below!");
                                        if(!AABBUtil.isPassable(below.getData().material())) {

                                            //System.out.println("floor not passable ok");
                                            Node node1 = grid.getNode(nX, nY, nZ);
                                            if(node1.isClosed() || node1.getData().getSquidHeight() != 0d) { continue; }
                                            if(nodeListener != null && !nodeListener.isPassable(node1, nX,nY,nZ)) {

                                                continue;

                                            }

                                            Block below1 = world.getBlockAt(nX, nY-1, nZ);
                                            Block above = world.getBlockAt(nX,nY+1,nZ);
                                            if(above.getType() != Material.AIR && AABBUtil.isPassable(above.getType())) {

                                                if (match.isOwnedByTeam(below1, team)) {

                                                    node1.setType(TransitionType.SWIM_BLOCKED);
                                                    node1.getAdditionalData().put("squidNeeded", true);
                                                    nodes.add(node1);

                                                } else {

                                                    if(!match.isEnemyTurf(below1, team)) {

                                                        node1.getAdditionalData().put("nextSwim", true);
                                                        node1.getAdditionalData().put("squidNeeded", true);
                                                        node1.setType(TransitionType.SWIM_DRY);
                                                        nodes.add(node1);

                                                    }

                                                }

                                            }


                                        }

                                    }

                                    /*Block below = world.getBlockAt(nX, nY-1, nZ);
                                    Node node1 = grid.getNode(nX, nY, nZ);
                                    if(node1.isClosed()) { continue; }
                                    if(nodeListener != null) {

                                        if(!nodeListener.isPassable(node1, nX,nY,nZ)) {

                                            continue;

                                        }

                                    }
                                    /*if(match.isOwnedByTeam(below, team)) {

                                        node1.setType(TransitionType.SWIM_BLOCKED);
                                        node1.getAdditionalData().put("squidNeeded", true);

                                    } else {

                                        node1.getAdditionalData().put("nextSwim", true);
                                        node1.getAdditionalData().put("squidNeeded", true);
                                        node1.setType(TransitionType.SWIM_DRY);

                                    }*/

                                }

                            }

                        }

                    } else {

                        if(node.getAdditionalData().containsKey("railIndex")) {

                            RailSnapshot snapshot = railSnapshots.get((Integer) node.getAdditionalData().get("railIndex"));
                            Vector targetVector = snapshot.vectors.get(snapshot.vectors.size() - 1);
                            int targetX = (int) targetVector.getX();
                            int targetY = (int) targetVector.getY();
                            int targetZ = (int) targetVector.getZ();

                            if(node.x == targetX && node.y == targetY && node.z == targetZ) {

                                // Letzter Knoten auf der Schiene
                                Node node1 = grid.fallCheck(targetX, targetY, targetZ, dimensions);
                                if(node1 != null && !node1.isClosed()) {

                                    node1.setType(TransitionType.FALL);
                                    nodes.add(node1);

                                }
                                //world.spawnParticle(Particle.END_ROD, node1.toVector().toLocation(world), 0);
                                return nodes;

                            } else {

                                Node node1 = grid.getNode(targetX, targetY, targetZ);
                                if(nodeListener != null) {

                                    if(!nodeListener.isPassable(node1, nX,nY,nZ)) {

                                        continue;

                                    }

                                }
                                if(!node1.isClosed()) {

                                    //world.spawnParticle(Particle.SPELL_INSTANT, node1.toVector().toLocation(world), 0);
                                    node1.setType(node.getType());
                                    node1.getAdditionalData().put("railIndex", node.getAdditionalData().get("railIndex"));
                                    nodes.add(node1);
                                    return nodes;

                                }

                            }

                        }

                    }

                }

            }

        }

        return nodes;

    }

}
