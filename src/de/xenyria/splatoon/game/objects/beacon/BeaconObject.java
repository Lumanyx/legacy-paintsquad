package de.xenyria.splatoon.game.objects.beacon;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.objects.RemovableGameObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageDealingProjectile;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.*;
import org.bukkit.block.Block;

public class BeaconObject extends GameObject implements HitableEntity, RemovableGameObject {

    private SplatoonPlayer owner;
    public SplatoonPlayer getOwner() { return owner; }

    private Block block;
    public BeaconObject(Match match, SplatoonPlayer owner, Block block) {

        super(match);
        this.team = owner.getTeam();
        this.owner = owner;
        this.block = block;
        int placeCounter = 0;
        BeaconObject lastBeacon = null;
        block.setType(Material.BEACON);

        for(GameObject object : match.getGameObjects()) {

            if(object instanceof BeaconObject && ((BeaconObject)object).owner == owner) {

                lastBeacon = (BeaconObject) object;
                placeCounter++;

            }

        }
        if(placeCounter >= 2) {

            lastBeacon.remove();

        }

    }

    public void remove() {

        block.setType(Material.AIR);
        Location location = block.getLocation();
        location = location.add(.5, .5, .5);
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 0);
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 0);
        location.getWorld().playSound(location, Sound.BLOCK_STONE_BREAK, 0.7f, 2f);
        getOwner().getMatch().queueObjectRemoval(this);

        getMatch().removeBeacon(this);

    }

    private double health = 50d;

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile.getShooter() != null && projectile.getShooter().getTeam() != owner.getTeam()) {

            if(projectile instanceof DamageDealingProjectile) {

                DamageDealingProjectile projectile1 = (DamageDealingProjectile)projectile;
                if(projectile1.dealsDamage()) {

                    health-=projectile1.getDamage();
                    if(health <= 0d) {

                        remove();

                    }

                }

            }

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }

        return aabb().c(projectile.aabb()) && health > 0;
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
        return block.getLocation();
    }

    @Override
    public double height() {
        return 0;
    }

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(
                block.getX() - 0.2, block.getY(), block.getZ() - 0.2,
                block.getX() + 1.2, block.getY() + 1.2, block.getZ() + 1.2
        );
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.BEACON;
    }

    private int soundTicker = 0;
    @Override
    public void onTick() {

        soundTicker++;
        if(soundTicker > 30) {

            soundTicker = 0;
            getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.125f, 0.9f);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.125f, 0.8f);

            }, 3l);

        }

    }

    @Override
    public void reset() {
        remove();
    }

    @Override
    public void onRemove() {

        block.setType(Material.AIR);

    }

    private Team team;
    public Team getTeam() { return team; }

}
