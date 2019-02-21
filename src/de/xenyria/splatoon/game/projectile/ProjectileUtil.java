package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import org.bukkit.Location;

public class ProjectileUtil {

    public static double manhattanDistance(double x1, double y1, double z1, double x2, double y2, double z2) {

        return Math.abs(x1-x2)+Math.abs(y1-y2)+Math.abs(z1-z2);

    }

    public static double manhattanDistance(SplatoonProjectile projectile, HitableEntity entity) {

        Location location1 = projectile.getLocation();
        Location location2 = entity.getLocation();
        return manhattanDistance(location1.getX(), location1.getY(), location1.getZ(), location2.getX(), location2.getY(), location2.getZ());

    }

}
