package de.xenyria.splatoon.ai.pathfinding.path;

import de.xenyria.splatoon.ai.navigation.NavigationManager;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NodePath {

    public ArrayList<NodePosition> getNodes() { return positions; }

    private double weight;
    public double getWeight() { return weight; }

    public static class NodePosition {

        public double blockHeight;
        private HashMap<String, Object> data;
        public HashMap<String, Object> getData() { return data; }

        public final int x;
        public final int z;
        public final double y;
        public Location toLocation(World world) {

            return toVector().toLocation(world);

        }
        public Vector toVector() {

            return new Vector(x + .5, y, z + .5);

        }

        public NodePosition(int x, double y, int z, TransitionType type, HashMap data, double blockHeight) {

            this.x = x;
            this.z = z;
            this.y = y;
            this.blockHeight = blockHeight;
            if(type == TransitionType.SWIM_DRY || type == TransitionType.SWIM_BLOCKED) {

                this.blockHeight = 0d;

            }

            this.data = data;
            this.type = type;

        }

        private TransitionType type;
        public TransitionType getType() { return type; }

    }
    private ArrayList<NodePosition> positions = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println(NavigationManager.center(-2));

    }

    public NodePath(Node currentNode) {

        Node lastNode = currentNode;
        weight = lastNode.totalHeight();
        while (lastNode != null) {

            NodePosition position = new NodePosition(lastNode.x, lastNode.y, lastNode.z, lastNode.getType(), lastNode.getAdditionalData(), lastNode.getData().getHeight());
            positions.add(position);
            lastNode = lastNode.getParent();

        }
        Collections.reverse(positions);

    }

}
