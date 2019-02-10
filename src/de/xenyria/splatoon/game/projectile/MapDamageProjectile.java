package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;

public class MapDamageProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    private double damage;

    private Color color;
    public Color getColor() { return color; }

    public MapDamageProjectile(Color color, DamageReason reason, Location location, Match match, double val) {

        super(null, null, match);
        this.location = location;
        this.color = color;
        this.reason = reason;
        this.damage = val;

    }

    private DamageReason reason;
    private Location location;

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

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return (float) damage;
    }
}
