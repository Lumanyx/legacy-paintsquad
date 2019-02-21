package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.Random;

public class BallerHitbox extends GameObject implements HitableEntity {

    private SplatoonPlayer player;
    private Baller baller;
    public BallerHitbox(Match match, SplatoonPlayer player, Baller baller) {

        super(match);
        this.player = player;
        this.baller = baller;

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private int lastBounceTicks = 0;

    @Override
    public void onTick() {

        if(lastBounceTicks > 0) {

            lastBounceTicks--;

        }

        if(lastBounceTicks < 1) {

            //RayProjectile projectile = new RayProjectile(baller.getPlayer(), baller, baller.getPlayer().getMatch(), baller.mimic.getBukkitEntity().getLocation(), baller.mimic.getBukkitEntity().getLocation().getDirection(), 20f);
            AxisAlignedBB bb = aabb();
            for (HitableEntity entity : baller.getPlayer().getMatch().getHitableEntities()) {

                if (entity != this && entity.aabb().c(bb)) {

                    BallerCollision collision = new BallerCollision(baller.getPlayer(), baller, baller.getPlayer().getMatch(), baller);
                    if (entity.isHit(collision)) {

                        entity.onProjectileHit(collision);
                        Vector entityLoc = entity.getLocation().toVector();
                        lastBounceTicks = 10;
                        Vector current = baller.mimic.getBukkitEntity().getLocation().toVector();
                        Vector dir = entityLoc.subtract(current).normalize().multiply(-1.7);
                        if(VectorUtil.isValid(dir)) {

                            baller.getVelocity().add(dir);

                        }

                        break;

                    }

                }

            }

        }

    }

    @Override
    public void reset() {

    }

    private double health = 350d;

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile instanceof DamageDealingProjectile) {

            DamageDealingProjectile projectile1 = (DamageDealingProjectile)projectile;
            if(projectile1.dealsDamage()) {

                health-=((DamageDealingProjectile) projectile).getDamage();
                getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_GLASS_BREAK, 0.35f, 0.8f);
                if(health < 0) {

                    baller.getPlayer().sendMessage( " §7Die §eSepisphäre §7wurde durch einen Gegner zerstört!");
                    baller.end();

                }

            }

        }
        if(projectile instanceof KnockbackProjectile) {

            KnockbackProjectile projectile1 = (KnockbackProjectile) projectile;
            baller.getVelocity().add(projectile1.getKnockback());

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile.getTeam() != null) {

            if(projectile.getTeam() == baller.getPlayer().getTeam()) { return false; }

        }

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }

        return aabb().c(projectile.aabb()) && health > 0;
    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().distance(getLocation());
    }

    @Override
    public int getEntityID() {
        return -1;
    }

    @Override
    public Location getLocation() {
        return baller.location();
    }

    @Override
    public double height() {
        return 2;
    }

    @Override
    public AxisAlignedBB aabb() {

        Location location = getLocation();
        return new AxisAlignedBB(location.getX() - .7, location.getY(), location.getZ() - .7, location.getX() + .7, location.getY() + 2, location.getZ() + .7);

    }

    @Override
    public boolean isDead() {
        return false;
    }
}
