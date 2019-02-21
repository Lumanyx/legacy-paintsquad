package de.xenyria.splatoon.game.projectile.inkstorm;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.inkstorm.InkCloud;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntitySnowball;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftSnowball;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

public class InkStormProjectile extends SplatoonProjectile {

    public InkStormProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

    public void spawn(Location location) {

        snowball = (Snowball) location.getWorld().spawnEntity(location, EntityType.SNOWBALL);
        Vector direction = location.getDirection().clone().multiply(.15);
        snowball.setVelocity(direction);

        EntitySnowball snowball1 = ((CraftSnowball)snowball).getHandle();
        snowball1.shooter = getShooter().getNMSPlayer();

        Location location1 = location.clone();
        location1.setPitch(0f);
        throwDirection = location1.getDirection();

    }

    @Override
    public Location getLocation() {
        return snowball.getLocation();
    }

    private Snowball snowball = null;

    @Override
    public void onRemove() {

        if(!snowball.isDead()) {

            snowball.remove();

        }

    }

    private Vector throwDirection;

    @Override
    public void tick() {

        if(snowball.getLocation().getY() < 0) { remove(); return; }

        if(snowball.isDead()) {

            InkCloud cloud = new InkCloud(getMatch(), getShooter(), snowball.getLocation(), throwDirection);

            remove();

        }

    }

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(0,0,0,0,0,0);
    }
}
