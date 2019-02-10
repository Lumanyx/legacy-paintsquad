package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.projectile.DamageDealingProjectile;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.structure.editor.point.Point;
import de.xenyria.util.math.LocalCoordinateSystem;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class BrellaTile extends GameObject implements HitableEntity, GroupedObject {

    private DetachedBrella brella;
    private Point point;
    public BrellaTile(Match match, DetachedBrella brella, Point point) {

        super(match);
        this.brella = brella;
        this.point = point;

    }

    public Vector location() {

        Location location = brella.getLocation();

        LocalCoordinateSystem lcs = new LocalCoordinateSystem();
        lcs.calculate(0, location.getYaw(), 0, .625);

        Vector vec = location.toVector().add((lcs.getForward().clone().multiply(point.getX()).add(lcs.getSideways().clone().multiply(point.getZ())).add(lcs.getUpwards().clone().multiply(point.getY()))));
        return vec;

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile.getShooter() == null || (projectile.getShooter().getTeam() != brella.owner.getTeam())) {

            if(projectile instanceof DamageDealingProjectile) {

                DamageDealingProjectile projectile1 = (DamageDealingProjectile)projectile;
                if(projectile1.dealsDamage()) {

                    brella.handleDamage(projectile.getLocation(), projectile1.getDamage());

                }

            }

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(brella.brella.attached && !brella.brella.isSelected()) {

            return false;

        }
        if(!brella.brella.getModel().isActive()) { return false; }

        if(projectile.getShooter() == null || (projectile.getShooter().getTeam() == brella.brella.getPlayer().getTeam())) { return false; }
        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }

        return aabb().c(projectile.aabb());
    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return location().toLocation(getMatch().getWorld()).distance(projectile.getLocation());
    }

    @Override
    public int getEntityID() {
        return -1;
    }

    @Override
    public Location getLocation() {
        return location().toLocation(getMatch().getWorld());
    }

    @Override
    public double height() {
        return 0.625;
    }

    @Override
    public AxisAlignedBB aabb() {
        Vector vector = location();
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
        return brella.tiles;
    }

    @Override
    public GameObject getRoot() {
        return brella;
    }
}
