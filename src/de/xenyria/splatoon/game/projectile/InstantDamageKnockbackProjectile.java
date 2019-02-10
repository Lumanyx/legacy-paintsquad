package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.DetachedBrella;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class InstantDamageKnockbackProjectile extends SplatoonProjectile implements DamageDealingProjectile, KnockbackProjectile {

    private Vector vector;
    private double dmg;
    private Location location;
    public InstantDamageKnockbackProjectile(Location location, SplatoonPlayer player, SplatoonWeapon weapon, Vector knockback, double dmg, Match match) {

        super(player, weapon, match);
        this.vector = knockback;
        this.dmg = dmg;
        this.location = location;

    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return (float) dmg;
    }

    @Override
    public Vector getKnockback() {
        return vector;
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
        return new AxisAlignedBB(0,0,0,0,0,0);
    }
}
