package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;

public class RollerVectorHurtPoint extends SplatoonProjectile implements DamageDealingProjectile {

    private Location location;

    public RollerVectorHurtPoint(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, Location location, float dmg) {

        super(shooter, weapon, match);
        this.dmg = dmg;
        this.location = location;

    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void tick() {

    }

    @Override
    public AxisAlignedBB aabb() {

        AxisAlignedBB bb = new AxisAlignedBB(getLocation().getX() - .25, getLocation().getY() - .25, getLocation().getZ() - .25,
                getLocation().getX() + .25, getLocation().getY() + .25, getLocation().getZ() + .25);

        return bb;

    }

    private float dmg;
    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return dmg;
    }
}
