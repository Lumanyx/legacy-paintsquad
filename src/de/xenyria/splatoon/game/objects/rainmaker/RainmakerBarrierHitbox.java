package de.xenyria.splatoon.game.objects.rainmaker;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.util.PlayerDamageCooldownMap;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class RainmakerBarrierHitbox extends GameObject implements HitableEntity, GroupedObject {

    private RainmakerBarrier barrier;
    private ArmorStand stand;
    private Vector vector;

    public RainmakerBarrierHitbox(Match match, RainmakerBarrier barrier, ArmorStand stand) {

        super(match);
        this.vector = stand.getLocation().toVector().add(new Vector(0, 1.6, 0));
        this.barrier = barrier;
        this.stand = stand;

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        barrier.handleProjectile(projectile);

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(!barrier.hasExploded()) {

            if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

                return true;

            }

            return aabb().c(projectile.aabb());

        } else {

            return false;

        }

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return getLocation().distance(projectile.getLocation());
    }

    @Override
    public int getEntityID() {
        return -1;
    }

    @Override
    public Location getLocation() {
        return vector.toLocation(getMatch().getWorld());
    }

    @Override
    public double height() {
        return .625;
    }

    @Override
    public AxisAlignedBB aabb() {

        return new AxisAlignedBB(vector.getX() - .325, vector.getY(), vector.getZ() - .325, vector.getX() + .325, vector.getY() + .625, vector.getZ() + .325);

    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    @Override
    public void onTick() {

    }

    @Override
    public void reset() {

    }

    @Override
    public ArrayList<HitableEntity> allObjects() {
        return barrier.getHitboxes();
    }

    @Override
    public GameObject getRoot() {
        return barrier;
    }
}