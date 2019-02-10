package de.xenyria.splatoon.game.util;

import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

public class VectorUtil {

    public static boolean isValid(Vector vector) {

        return NumberConversions.isFinite(vector.getX()) &&
                NumberConversions.isFinite(vector.getY()) &&
                NumberConversions.isFinite(vector.getZ());

    }

    public static double horDistance(Vector a, Vector b) {

        Vector aA = a.clone();
        Vector bA = b.clone();
        aA.setY(0); bA.setY(0);
        return aA.distance(bA);

    }

}
