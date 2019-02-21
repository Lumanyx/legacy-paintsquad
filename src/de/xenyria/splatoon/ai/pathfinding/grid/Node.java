package de.xenyria.splatoon.ai.pathfinding.grid;

import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.heap.IHeapItem;
import net.minecraft.server.v1_13_R2.IBlockData;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class Node implements IHeapItem {

    private Node parent;
    public Node getParent() { return parent; }
    public void setParent(Node node) { this.parent = node; }

    private HashMap<String, Object> additionalData;
    public boolean isAdditionalDataInitialized() { return additionalData != null; }
    public boolean containsData(String name) { return isAdditionalDataInitialized() && additionalData.containsKey(name); }
    public void putData(String key, Object obj) {

        if(!isAdditionalDataInitialized()) {

            additionalData = new HashMap<>();

        }
        additionalData.put(key, obj);

    }

    private boolean first;
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) {

        this.first = first;

    }

    public final int x,y,z;

    private NodeBlockData data;
    public NodeBlockData getData() { return data; }

    public double totalHeight() {

        if(type != TransitionType.SWIM && type != TransitionType.SWIM_DRY) {

            return y + data.getHeight();

        } else {

            return y;

        }

    }

    private TransitionType type = TransitionType.WALK;
    public TransitionType getType() { return type; }
    public void setType(TransitionType type) { this.type = type; }

    public static double distance(Node a, Node b) {

        return distance(a.x, a.y, a.z, b.x, b.y, b.z);

    }

    public static double distance(int x, int y, int z, int x1, int y1, int z1) {

        int dX = Math.abs(x1-x);
        int dY = Math.abs(y1-y);
        int dZ = Math.abs(z1-z);
        return dX+dY+dZ;

    }

    public void calculateCosts(SquidAStar astar) {

        hCost = distance(x,y,z,(int)astar.getTargetVector().getX(), (int)astar.getTargetVector().getY(), (int)astar.getTargetVector().getZ());
        if(astar.getStartNode() != null) {

            gCost = distance(x, y, z, astar.getStartNode().x, astar.getStartNode().y, astar.getStartNode().z);

        }

    }

    public double hCost,gCost;
    public double fCost() { return gCost+(hCost+type.getWeight()); }

    private boolean closed = false;
    public boolean isClosed() { return closed; }
    public void close() { closed = true; }

    public Node(int x, int y, int z) {

        this.x = x; this.y = y; this.z = z;

    }

    private int index;
    public int getIndex() { return index; }
    public void setIndex(int i) { this.index = i; }

    @Override
    public int hashCode() {

        return new HashCodeBuilder().append(x).append(y).append(z).toHashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof Node) {

            Node node = (Node)obj;
            return node.x == x && node.y == y && node.z == z;

        } else {

            return false;

        }

    }

    @Override
    public int compareTo(Object o) {
        int compare = Double.compare(fCost(), ((Node)o).fCost());
        if (compare == 0) {
            compare = Double.compare(hCost, ((Node)o).hCost);
        }
        return -compare;
    }

    public void createData(NodeGrid nodeGrid) {

        data = new NodeBlockData(nodeGrid.getWorld(), this, nodeGrid);

    }

    public Vector toVector() {

        if(alternativeVec != null) {

            return alternativeVec;

        } else {

            return new Vector(x + .5, totalHeight(), z + .5);

        }

    }

    private Vector alternativeVec;
    public void addHeight(double v) {

        alternativeVec = new Vector(x+.5,v,z+.5);

    }

    public Object getData(String key) {

        if(isAdditionalDataInitialized()) {

            return additionalData.get(key);

        }
        return null;

    }

    public HashMap getAdditionalData() { return additionalData; }
}
