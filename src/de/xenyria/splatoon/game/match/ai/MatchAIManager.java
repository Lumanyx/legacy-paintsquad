package de.xenyria.splatoon.game.match.ai;

import de.xenyria.core.array.ThreeDimensionalArray;
import de.xenyria.io.reader.ByteArrayReader;
import de.xenyria.io.reader.ByteArrayWriter;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.map.Map;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Gusher;
import de.xenyria.splatoon.game.objects.InkRail;
import de.xenyria.splatoon.game.objects.RideRail;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.BlockPosition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MatchAIManager {

    public Match getMatch() { return match; }

    private ThreeDimensionalArray<PaintableRegion> paintableRegionMirror = new ThreeDimensionalArray<>();
    public ArrayList<PaintableRegion> getPaintableRegions() { return paintableRegions; }

    public PaintableRegion getPaintableRegion(PaintableRegion.Coordinate coordinate) {

        return paintableRegionMirror.get(coordinate.getX(), coordinate.getY(), coordinate.getZ());

    }

    public double coverageInArea(Location location, Team team, double val) {

        double val1 = 0d;
        for(PaintableRegion region : getPaintableRegions()) {

            if(region.getCenter().distance(location.toVector()) <= val) {

                val1+=region.coverage(team);

            }

        }
        return val1;

    }

    public class GameObjectSnapshot {

        private ArrayList<SquidAStar.GusherSnapshot> gusherSnapshots = new ArrayList<>();
        public ArrayList<SquidAStar.GusherSnapshot> getGusherSnapshots() { return gusherSnapshots; }

        private ArrayList<SquidAStar.RailSnapshot> railSnapshots = new ArrayList<>();
        public ArrayList<SquidAStar.RailSnapshot> getRailSnapshots() { return railSnapshots; }

    }

    public double coverageAt(Vector position, Team team) {

        PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        PaintableRegion region = paintableRegionMirror.get(coordinate.getX(), coordinate.getY(), coordinate.getZ());
        if(region != null) {

            return region.coverage(team);

        } else {

            return -1d;

        }

    }

    public double notCoverageWeight(Vector position, Team team) {

        double percentage = coverageAt(position, team);
        if(percentage != -1) {

            return 100d - percentage;

        } else {

            return 0d;

        }


    }

    private GameObjectSnapshot snapshot = new GameObjectSnapshot();
    public GameObjectSnapshot getSnapshot() { return snapshot; }

    private int objectRefreshTicker = 0;
    public void tick() {

        objectRefreshTicker++;
        if(objectRefreshTicker > 20) {

            objectRefreshTicker = 0;
            ArrayList<SquidAStar.GusherSnapshot> newGusherSnapshots = new ArrayList<>();
            ArrayList<SquidAStar.RailSnapshot> newRailSnapshots = new ArrayList<>();

            for(GameObject object : match.getGameObjects()) {

                if(object instanceof Gusher) {

                    Gusher gusher = (Gusher)object;
                    Team owningTeam = gusher.getOwningTeam();
                    if(gusher.getRemainingTicks() < 1) {

                        owningTeam = null;

                    }

                    SquidAStar.GusherSnapshot snapshot = new SquidAStar.GusherSnapshot(object.getID(), gusher.getLocation().toVector(), (int)Math.floor(gusher.computeHeight()), owningTeam);
                    newGusherSnapshots.add(snapshot);

                } else if(object instanceof InkRail) {

                    InkRail rail = (InkRail) object;
                    if(rail.getOwningTeam() != null) {

                        SquidAStar.RailSnapshot snapshot = new SquidAStar.RailSnapshot(object.getID(), object.getObjectType(), rail.getOwningTeam(),
                                rail.getTrack().toArray(new Vector[]{}));
                        newRailSnapshots.add(snapshot);

                    }

                } else if(object instanceof RideRail) {

                    RideRail rail = (RideRail) object;
                    if(rail.getOwningTeam() != null) {

                        SquidAStar.RailSnapshot snapshot = new SquidAStar.RailSnapshot(object.getID(), object.getObjectType(), rail.getOwningTeam(),
                                rail.getVectors().toArray(new Vector[]{}));
                        newRailSnapshots.add(snapshot);

                    }

                }

            }
            snapshot.gusherSnapshots = newGusherSnapshots;
            snapshot.railSnapshots = newRailSnapshots;

        }

    }

    public class FindAllNodesTarget implements PathfindingTarget {

        private ArrayList<Node> nodeList;
        private Vector teamSpawn;
        public FindAllNodesTarget(ArrayList<Node> foundNodes, Vector teamSpawn) {

            this.nodeList = foundNodes;
            this.teamSpawn = teamSpawn;

        }

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

            SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
            capabilities.climbEveryWall = true;
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
            return 99999;
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

                    for(Node node : nodes) {

                        if(!nodeList.contains(node)) {

                            nodeList.add(node);

                        }

                    }
                    return null;

                }

            };
        }

        @Override
        public Vector getEstimatedPosition() {
            return teamSpawn;
        }
    }

    public ArrayList<Node> goBackUntil(Node node, TransitionType type, int depth) {

        ArrayList<Node> nodes = new ArrayList<>();
        int i = 0;
        Node lastNode = node;
        while (i < depth) {

            nodes.add(node);
            if(node.getType() == type) {

                return nodes;

            }

            i++;
            lastNode = node.getParent();

        }
        return nodes;

    }

    private Match match;

    private Node[] foundNodeArray;
    public Node[] getFoundNodes() { return foundNodeArray; }

    public static final Vector[] PAINT_SPOT_DETECTION_OFFSETS = new Vector[]{
            new Vector(0,0,0),
            new Vector(0, -1, 0),
            new Vector(0, 1, 0),
            new Vector(1,0,0),
            new Vector(0,0,1),
            new Vector(-1,0,0),
            new Vector(0,0,-1),
    };

    public static void main(String[] args) {

        ArrayList<Double> doubles = new ArrayList<>();
        doubles.add(1d);
        doubles.add(3d);

        Collections.sort(doubles, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        });
        System.out.println(doubles.get(0));

    }

    public PaintableRegion getNextRegion(Team team, Vector vector) {

        ArrayList<PaintableRegion> nearbyRegions = new ArrayList<>();
        for(PaintableRegion region : paintableRegions) {

            if(region.getCenter().distance(vector) <= 30d && region.coverage(team) < 70D) {

                nearbyRegions.add(region);

            }

        }

        Collections.sort(nearbyRegions, new Comparator<PaintableRegion>() {
            @Override
            public int compare(PaintableRegion o1, PaintableRegion o2) {
                return Double.compare(o1.coverage(team),o2.coverage(team));
            }
        });
        if(!nearbyRegions.isEmpty()) {

            int max = 3;
            if((nearbyRegions.size() - 1) >= max) {

                return nearbyRegions.get(new Random().nextInt(3));

            } else {

                return nearbyRegions.get(0);

            }

        }
        return null;

    }

    private Vector mapCenter = null;

    public void determineMapCenter() {

        Vector mapCenter = new Vector();
        for(Map.TeamSpawn spawn : match.getMap().getSpawns()) {

            mapCenter = mapCenter.add(spawn.getPosition().toVector());

        }
        this.mapCenter = mapCenter.divide(new Vector(match.getMap().getSpawns().size(), match.getMap().getSpawns().size(), match.getMap().getSpawns().size()));

    }

    public ArrayList<Node> gatherNodesBySpawns() {

        if(mapCenter == null) { determineMapCenter(); }

        ArrayList<Node> foundNodes = new ArrayList<>();
        FindAllNodesTarget target = new FindAllNodesTarget(foundNodes, mapCenter);
        int id = 0;
        for(Map.TeamSpawn team : match.getMap().getSpawns()) {

            Team team1 = match.getRegisteredTeams().get(id);
            SquidAStar aStar = new SquidAStar(getMatch().getWorld(), team.getPosition().toVector(), target, match, team1, 99999);
            aStar.updateCapabilities(target.getMovementCapabilities());
            aStar.beginProcessing();
            id++;

        }

        return foundNodes;

    }
    public Node[] gatherNodesByLocation(Location location, boolean climbEveryWall) {

        ArrayList<Node> foundNodes = new ArrayList<>();
        FindAllNodesTarget target = new FindAllNodesTarget(foundNodes, mapCenter);
        int id = 0;

        SquidAStar.MovementCapabilities capabilities = new SquidAStar.MovementCapabilities();
        capabilities.climbEveryWall = climbEveryWall;

        Team team1 = match.getRegisteredTeams().get(id);
        SquidAStar aStar = new SquidAStar(getMatch().getWorld(), location.toVector(), target, match, team1, 99999);
        aStar.updateCapabilities(target.getMovementCapabilities());
        aStar.beginProcessing();

        return foundNodes.toArray(new Node[]{});

    }

    public void initSpots(ArrayList<Node> foundNodes) {

        Vector mapCenter = new Vector();
        for(Map.TeamSpawn spawn : match.getMap().getSpawns()) {

            mapCenter = mapCenter.add(spawn.getPosition().toVector());

        }
        mapCenter = mapCenter.divide(new Vector(match.getMap().getSpawns().size(), match.getMap().getSpawns().size(), match.getMap().getSpawns().size()));

        int id = 0;
        FindAllNodesTarget target = new FindAllNodesTarget(foundNodes, mapCenter);
        for(Map.TeamSpawn team : match.getMap().getSpawns()) {

            Team team1 = match.getRegisteredTeams().get(id);
            SquidAStar aStar = new SquidAStar(getMatch().getWorld(), team.getPosition().toVector(), target, match, team1, 99999);
            aStar.updateCapabilities(target.getMovementCapabilities());
            aStar.beginProcessing();
            id++;

        }

        foundNodeArray = foundNodes.toArray(new Node[]{});
        XenyriaSplatoon.getXenyriaLogger().log("Insgesamt §e" + foundNodeArray.length + " mögliche Positionen §rgefunden.");

        // Paint-Spot-Generierung
        ArrayList<PaintableRegion.Coordinate> coordinates = new ArrayList<>();
        for(Node node : foundNodes) {

            for(Vector vector : PAINT_SPOT_DETECTION_OFFSETS) {

                PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(node.x + vector.getBlockX(), node.y + vector.getBlockY(), node.z + vector.getBlockZ());
                if (!coordinates.contains(coordinate)) {

                    coordinates.add(coordinate);

                }

            }

        }

        for(PaintableRegion.Coordinate coordinate : coordinates) {

            Vector start = new Vector(coordinate.getX() * 5, coordinate.getY() * 5, coordinate.getZ() * 5);
            Vector end = new Vector((coordinate.getX() * 5) + 5, (coordinate.getY() * 5) + 5, (coordinate.getZ() * 5) + 5);

            int minX = (int) Math.min(start.getX(), end.getX());
            int minY = (int) Math.min(start.getY(), end.getY());
            int minZ = (int) Math.min(start.getZ(), end.getZ());
            int maxX = (int) Math.max(start.getX(), end.getX());
            int maxY = (int) Math.max(start.getY(), end.getY());
            int maxZ = (int) Math.max(start.getZ(), end.getZ());

            ArrayList<Node> nodes = new ArrayList<>();
            for(Node node : foundNodeArray) {

                if(node.x >= minX && node.x <= maxX && node.y >= minY && node.y <= maxY && node.z >= minZ && node.z <= maxZ) {

                    nodes.add(node);

                }

            }
            PaintableRegion region = new PaintableRegion(getMatch().getWorld(), this, start, end, nodes);
            if(region.foundBlocks() > 0) {

                //System.out.println(coordinate.getX() + " " + coordinate.getY() + " " + coordinate.getZ() + " > " + region.foundBlocks());
                paintableRegions.add(region);

            }

        }
        XenyriaSplatoon.getXenyriaLogger().log("Insgesamt §e" + paintableRegions.size() + " KI-Färboriertungsbereiche §rerstellt.");
        for(PaintableRegion region : paintableRegions) {

            paintableRegionMirror.set(region, region.getCoordinate().getX(), region.getCoordinate().getY(), region.getCoordinate().getZ());

        }

    }

    public void fromBytes(byte[] data) {

        determineMapCenter();
        ByteArrayReader reader = new ByteArrayReader(data);
        Vector offset = match.getOffset();
        int offsetX = (int) offset.getX();
        int offsetY = (int) offset.getY();
        int offsetZ = (int) offset.getZ();
        reader.readInt();
        reader.readInt();
        reader.readInt();

        int indx = reader.readInt();
        for(int i = 0; i < indx; i++) {

            double centerX = offsetX+reader.readInt()+.5;
            double centerY = offsetY+reader.readInt()+.5;
            double centerZ = offsetZ+reader.readInt()+.5;
            PaintableRegion region = new PaintableRegion(getMatch().getWorld(), this, centerX, centerY, centerZ);
            int blockCount = reader.readInt();
            for(int x = 0; x < blockCount; x++) {

                int blockX = offsetX+reader.readInt();
                int blockY = offsetY+reader.readInt();
                int blockZ = offsetZ+reader.readInt();
                region.getPaintableBlocks().add(match.getWorld().getBlockAt(
                        blockX, blockY, blockZ
                ));

            }
            int nodeCount = reader.readInt();
            for(int x = 0; x < nodeCount; x++) {

                int nodeX = offsetX+reader.readInt();
                int nodeY = offsetY+reader.readInt();
                int nodeZ = offsetZ+reader.readInt();
                Node node = new Node(nodeX, nodeY, nodeZ);
                node.addHeight(offsetY+reader.readDouble());
                node.setType(TransitionType.WALK);
                region.getFloorCoordinates().add(node);

            }
            PaintableRegion.Coordinate coordinate = PaintableRegion.Coordinate.fromWorldCoordinates(
                    (int)centerX, (int)centerY, (int)centerZ
            );
            paintableRegionMirror.set(region, coordinate.getX(), coordinate.getY(), coordinate.getZ());
            this.paintableRegions.add(region);

        }

    }

    public byte[] regionsToBytes() {

        Vector offset = match.getOffset();
        int offsetX = (int) offset.getX();
        int offsetY = (int) offset.getY();
        int offsetZ = (int) offset.getZ();

        ByteArrayWriter writer = new ByteArrayWriter();
        writer.writeInt(offsetX);
        writer.writeInt(offsetY);
        writer.writeInt(offsetZ);
        writer.writeInt(this.getPaintableRegions().size());
        for(PaintableRegion region : getPaintableRegions()) {

            Vector relPos = region.getCenter();
            writer.writeInt((int)relPos.getX()-offsetX);
            writer.writeInt((int)relPos.getY()-offsetY);
            writer.writeInt((int)relPos.getZ()-offsetZ);
            writer.writeInt(region.getPaintableBlocks().size());
            for(Block block : region.getPaintableBlocks()) {

                writer.writeInt(block.getX()-offsetX);
                writer.writeInt(block.getY()-offsetY);
                writer.writeInt(block.getZ()-offsetZ);

            }
            writer.writeInt(region.getFloorCoordinates().size());
            System.out.println("Save " + region.getFloorCoordinates().size() + " coords");
            for(Node node : region.getFloorCoordinates()) {

                writer.writeInt(node.x-offsetX);
                writer.writeInt(node.y-offsetY);
                writer.writeInt(node.z-offsetZ);
                writer.writeDouble(node.toVector().getY()-offsetY);

            }

        }
        return writer.bytes();

    }

    public MatchAIManager(Match match) {

        this.match = match;

    }

    private ArrayList<PaintableRegion> paintableRegions = new ArrayList<>();

    public ArrayList<PaintableRegion> nearbyRegions(Vector position, double radius) {

        ArrayList<PaintableRegion> regions = new ArrayList<>();
        for(PaintableRegion region : paintableRegions) {

            if(region.getCenter().distance(position) <= radius) {

                regions.add(region);

            }

        }
        return regions;

    }

}
