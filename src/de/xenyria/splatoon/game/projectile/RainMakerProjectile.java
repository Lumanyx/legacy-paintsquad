package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageDealingProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class RainMakerProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    public RainMakerProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {
        super(shooter, weapon, match);
    }

    private Item item;
    private Location location;
    private boolean landed = false;
    private boolean explode = false;

    public void launch(Location location, Vector dir, double impulse, boolean explode) {

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(new ItemStack(getShooter().getTeam().getColor().getWool()));
        item.setVelocity(dir.clone().multiply(impulse));
        location = item.getLocation();
        this.explode = explode;

    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return 0;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void onRemove() {

    }

    private int ticksToExplosion = 30;
    private double radius = 5f;
    private int warnTicker = 0;

    @Override
    public void tick() {

        // Floor hit check
        if(!landed) {

            location = item.getLocation();
            SplatoonServer.broadcastColorizedBreakParticle(location.getWorld(),
                    location.getX(), location.getY(), location.getZ(), getShooter().getTeam().getColor());

            Vector velocity = item.getVelocity();
            velocity = velocity.clone().add(velocity.clone().normalize().multiply(0.5));
            Vector target = item.getLocation().toVector().add(velocity);
            Vector current = item.getLocation().toVector();

            Block block = BlockUtil.ground(location, 8);
            getMatch().colorSquare(block, getShooter().getTeam(), getShooter(), 2);

            for(HitableEntity entity : getMatch().getHitableEntities()) {

                RayProjectile projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), location, target.clone().subtract(current).normalize(), 30f);
                if(projectile.rayTraceWithoutObstruction(entity.aabb(), velocity.length() + .3, target.clone().subtract(current).normalize(), true) && entity.isHit(projectile)) {

                    entity.onProjectileHit(projectile);

                    landed = true;
                    item.remove();
                    NMSUtil.broadcastEntityRemovalToSquids(item);

                }

            }

            if(landed) {

                location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 0);

            } else {

                World world = getLocation().getWorld();
                RayTraceResult result = world.rayTraceBlocks(getLocation(), target.clone().subtract(current.clone()), velocity.length() + .3);
                if((result != null && result.getHitBlock() != null) || item.isOnGround()) {

                    landed = true;
                    item.remove();
                    NMSUtil.broadcastEntityRemovalToSquids(item);

                }

            }

        } else {

            if(explode) {

                ticksToExplosion--;
                if (ticksToExplosion < 1) {

                    ArrayList<GameObject> alreadyHit = new ArrayList<>();
                    for (HitableEntity entity : getMatch().getHitableEntities()) {

                        if(entity != getShooter()) {

                            Vector dir = entity.centeredHeightVector().clone().subtract(location.toVector()).normalize();
                            double dist = entity.getLocation().distance(location);
                            if (dist <= radius) {

                                final double maxDmg = 180;
                                double dmg = (1 - (dist / radius)) * maxDmg;

                                RayProjectile projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), location, dir, (float) dmg);
                                if (entity.isHit(projectile)) {

                                    if(entity instanceof GroupedObject) {

                                        GroupedObject object = (GroupedObject)entity;
                                        HitableEntity nearest = object.getNearestObject(entity, getLocation());
                                        GameObject object1 = object.getRoot();

                                        if(nearest != null) {

                                            entity = nearest;
                                            if(!alreadyHit.contains(object1)) {

                                                alreadyHit.add(object1);
                                                dist = nearest.getLocation().distance(location);
                                                dmg = (1 - (dist / radius)) * maxDmg;
                                                projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), location, dir, (float) dmg);

                                            } else {

                                                continue;

                                            }

                                        }

                                    }

                                    entity.onProjectileHit(projectile);

                                }


                            }

                        }

                    }

                    double radHalf = radius;
                    double minX = Math.min(item.getLocation().getX() - radHalf, item.getLocation().getX() + radHalf);
                    double minY = Math.min(item.getLocation().getY() - radHalf, item.getLocation().getY() + radHalf);
                    double minZ = Math.min(item.getLocation().getZ() - radHalf, item.getLocation().getZ() + radHalf);
                    double maxX = Math.max(item.getLocation().getX() - radHalf, item.getLocation().getX() + radHalf);
                    double maxY = Math.max(item.getLocation().getY() - radHalf, item.getLocation().getY() + radHalf);
                    double maxZ = Math.max(item.getLocation().getZ() - radHalf, item.getLocation().getZ() + radHalf);
                    location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);

                    for(int x = (int) minX; x <= maxX; x++) {

                        for(int y = (int) minY; y <= maxY; y++) {

                            for(int z = (int) minZ; z <= maxZ; z++) {

                                Vector vector = new Vector(x,y,z);
                                double dist = vector.distance(item.getLocation().toVector());
                                if(dist <= radius) {

                                    int ticks = (int) dist;
                                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                        Block block = getLocation().getWorld().getBlockAt((int)vector.getX(), (int)vector.getY(), (int)vector.getZ());
                                        if(block.getType() == Material.AIR) {

                                            Block grounded = BlockUtil.ground(block.getLocation(), 7);
                                            getMatch().paint(getShooter(), grounded.getLocation().toVector(), getShooter().getTeam());

                                        } else {

                                            getMatch().paint(getShooter(), block.getLocation().toVector(), getShooter().getTeam());

                                        }

                                    }, ticks);

                                }

                            }

                        }

                    }

                    remove();

                } else {

                    warnTicker++;
                    if(warnTicker > 3) {

                        location.getWorld().playSound(location, Sound.ENTITY_VEX_CHARGE, 1f, 2f);
                        warnTicker = 0;
                        for(float yaw = 0f; yaw < 360; yaw+=22.5f) {

                            for(float pitch = -90f; pitch < 90; pitch+=22.5) {

                                Location clocation = new Location(getLocation().getWorld(), 0,0,0);
                                clocation.setYaw(yaw);
                                clocation.setPitch(pitch);
                                Vector vec = location.toVector().add(clocation.getDirection().clone().multiply(radius));
                                if(ticksToExplosion < 5) {

                                    SplatoonServer.broadcastColorizedBreakParticle(getLocation().getWorld(), vec.getX(), vec.getY(), vec.getZ(), getShooter().getTeam().getColor());

                                } else {

                                    SplatoonServer.broadcastColorParticle(getLocation().getWorld(), vec.getX(), vec.getY(), vec.getZ(), getShooter().getTeam().getColor(), 1f);

                                }

                            }

                        }

                    }

                }

            } else {

                remove();

            }

        }

    }

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(0,0,0,0,0,0);
    }
}
