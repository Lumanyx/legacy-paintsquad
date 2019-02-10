package de.xenyria.splatoon.ai.navigation;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class NavigationPoint {

    public int x;
    public int z;
    public double y;

    public double blockHeight;
    private HashMap<String, Object> data;
    public HashMap<String, Object> getData() { return data; }

    public NavigationPoint(double x, double y, double z, TransitionType type, HashMap<String, Object> data) {

        this.x = (int) x;
        this.y = y;
        this.z = (int) z;
        this.transitionType = type;
        this.data = data;

    }

    public Vector toVector() {

        return new Vector(x + .5, y, z + .5);

    }

    private TransitionType transitionType = TransitionType.WALK;
    public TransitionType getTransitionType() { return transitionType; }

    public boolean isReached(Location location) {

        Vector vector = new Vector(x, 0, z);

        double cenX = x + .5;
        double cenZ = z + .5;

        double minX = Math.min(cenX - .5, cenX + .5);
        double minZ = Math.min(cenZ - .5, cenZ + .5);

        double minY = y-.076;
        double maxY = y + .076d;
        if(getTransitionType() == TransitionType.RIDE_RAIL || getTransitionType() == TransitionType.INK_RAIL) {

            //minY-=0.2d;
            //maxY+=0.1;

        } else if(getTransitionType() == TransitionType.SWIM_WALL_VERTICAL) {

            maxY+=0.2d;


        } else if(getTransitionType() == TransitionType.ENTER_FOUNTAIN) {

            minY-=2d;
            maxY+=4d;

        } else if(getTransitionType() == TransitionType.SWIM_DRY) {

            maxY = 256D;
            minY = 0d;

        }

        double maxX = Math.max(cenX - .5, cenX + .5);
        double maxZ = Math.max(cenZ - .5, cenZ + .5);
        double x = location.getX(), y = location.getY(), z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;

    }

    public void updateTransitionType(TransitionType type) {

        this.transitionType = type;

    }

}
