package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;

public class BallerCollision extends SplatoonProjectile implements DamageDealingProjectile {

    private Baller baller;
    public BallerCollision(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, Baller baller) {

        super(shooter, weapon, match);
        this.baller = baller;

    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return 20;
    }

    @Override
    public Location getLocation() {
        return baller.mimic.getBukkitEntity().getLocation();
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void tick() {

    }

    @Override
    public AxisAlignedBB aabb() {
        return baller.getHitbox().aabb();
    }
}
