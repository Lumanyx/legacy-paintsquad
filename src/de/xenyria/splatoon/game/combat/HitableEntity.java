package de.xenyria.splatoon.game.combat;

import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface HitableEntity {

    void onProjectileHit(SplatoonProjectile projectile);
    boolean isHit(SplatoonProjectile projectile);
    double distance(SplatoonProjectile projectile);
    int getEntityID();
    Location getLocation();
    double height();
    default Vector centeredHeightVector() {

        Location location = getLocation();
        location = location.clone().add(0, height() / 2, 0);
        return location.toVector();

    }
    AxisAlignedBB aabb();
    boolean isDead();
}
