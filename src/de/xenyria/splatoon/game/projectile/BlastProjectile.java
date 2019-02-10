package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.RandomUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class BlastProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    private float range, explosionRadius, impactDamage;
    private Location location;
    private Item item;
    private Location spawnLoc;
    private Vector dir;

    public BlastProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, float explosionRadius, float range, float impactDamage) {

        super(shooter, weapon, match);
        this.location = shooter.getShootingLocation(true).clone().add(0, -.35, 0);
        dir = shooter.getEyeLocation().getDirection().clone();
        this.explosionRadius = explosionRadius;
        this.range = range;
        this.impactDamage = impactDamage;

    }

    private double speed;
    public void spawn(double impulse) {

        this.speed = impulse;
        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        spawnLoc = item.getLocation().clone();
        item.setItemStack(new ItemStack(getShooter().getTeam().getColor().getWool()));
        item.setGravity(false);
        item.setVelocity(dir.clone().multiply(speed));

    }

    @Override
    public boolean dealsDamage() {

        return true;

    }

    private float dmg;
    @Override
    public float getDamage() {
        return dmg;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void onRemove() {

        item.remove();
        NMSUtil.broadcastEntityRemovalToSquids(item);

    }

    private ArrayList<HitableEntity> directlyHit = new ArrayList<>();

    @Override
    public void tick() {

        location = item.getLocation();
        Block block = location.getBlock();
        block = BlockUtil.ground(block.getLocation(), (int) explosionRadius);
        if(RandomUtil.random(50)) {

            getMatch().colorSquare(block, getShooter().getTeam(), getShooter(), 1);

        }

        Vector targetLocation = location.toVector().add(location.getDirection().clone().multiply(speed));
        World world = location.getWorld();
        RayTraceResult result = world.rayTraceBlocks(location, dir, (speed * 3));
        boolean detonate = false;

        if(targetLocation.distance(spawnLoc.toVector()) > range) {

            detonate = true;

        }

        if(!detonate) {

            if (result != null && result.getHitBlock() != null) {

                detonate = true;

            } else {

                // Entities
                for (HitableEntity entity : getMatch().getHitableEntities()) {

                    AxisAlignedBB bb = entity.aabb();
                    BoundingBox boundingBox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                    RayTraceResult entityHitResult = boundingBox.rayTrace(location.toVector(), dir, speed);
                    if (getShooter() != entity && entityHitResult != null && entityHitResult.getHitPosition() != null) {

                        detonate = true;
                        directlyHit.add(entity);

                    }

                }

            }

        }

        if(detonate) {

            detonate();

        }

        item.setVelocity(dir.clone().multiply(speed));

    }

    public void detonate() {

        getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 2f);

        dmg = impactDamage;
        for(HitableEntity entity : directlyHit) {

            RayProjectile projectile = new RayProjectile(getShooter(), getShooter().getEquipment().getPrimaryWeapon(),
                    getMatch(), location, dir, impactDamage);
            if(projectile.getShooter() != entity && entity.isHit(projectile)) {

                entity.onProjectileHit(projectile);
                getShooter().hitMark(location);

            }

        }

        ArrayList<GameObject> hitObjects = new ArrayList<>();
        for(HitableEntity entity : getMatch().getHitableEntities()) {

            double dist = entity.getLocation().distance(item.getLocation());
            if(dist <= explosionRadius) {

                double factor = 1 - (dist / explosionRadius);
                double dmg1 = (impactDamage / 2) * factor;
                this.dmg = (float) dmg1;

                Vector dir = entity.centeredHeightVector().clone().subtract(location.toVector()).normalize().multiply(1);

                RayProjectile projectile = new RayProjectile(getShooter(), getShooter().getEquipment().getPrimaryWeapon(),
                        getMatch(), location, dir, dmg);

                if(projectile.rayTraceWithoutObstruction(entity.aabb(), explosionRadius, dir) && entity.isHit(projectile)) {

                    if(entity instanceof GroupedObject) {

                        GroupedObject object = (GroupedObject) entity;
                        GameObject object1 = object.getRoot();
                        if(!hitObjects.contains(object1)) {

                            hitObjects.add(object1);
                            entity = object.getNearestObject(entity, item.getLocation());
                            dist = entity.getLocation().distance(item.getLocation());
                            factor = 1 - (dist / explosionRadius);
                            dmg1 = (impactDamage / 2) * factor;
                            this.dmg = (float) dmg1;
                            projectile = new RayProjectile(getShooter(), getShooter().getEquipment().getPrimaryWeapon(),
                                    getMatch(), location, dir, dmg);

                        } else {

                            continue;

                        }

                    }

                    entity.onProjectileHit(projectile);

                }

            }

        }

        double radHalf = explosionRadius;
        double minX = Math.min(item.getLocation().getX() - radHalf, item.getLocation().getX() + radHalf);
        double minY = Math.min(item.getLocation().getY() - radHalf, item.getLocation().getY() + radHalf);
        double minZ = Math.min(item.getLocation().getZ() - radHalf, item.getLocation().getZ() + radHalf);
        double maxX = Math.max(item.getLocation().getX() - radHalf, item.getLocation().getX() + radHalf);
        double maxY = Math.max(item.getLocation().getY() - radHalf, item.getLocation().getY() + radHalf);
        double maxZ = Math.max(item.getLocation().getZ() - radHalf, item.getLocation().getZ() + radHalf);

        for(float yaw = 0f; yaw < 360; yaw+=22.5f) {

            for(float pitch = -90f; pitch < 90; pitch+=22.5) {

                Location clocation = new Location(getLocation().getWorld(), 0,0,0);
                clocation.setYaw(yaw);
                clocation.setPitch(pitch);
                Vector vec = location.toVector().add(clocation.getDirection().clone().multiply(explosionRadius));
                SplatoonServer.broadcastColorParticle(getLocation().getWorld(), vec.getX(), vec.getY(), vec.getZ(), getShooter().getTeam().getColor(), 1f);

            }

        }

        for(int x = (int) minX; x <= maxX; x++) {

            for(int y = (int) minY; y <= maxY; y++) {

                for(int z = (int) minZ; z <= maxZ; z++) {

                    Vector vector = new Vector(x,y,z);
                    double dist = vector.distance(item.getLocation().toVector());
                    if(dist <= explosionRadius) {

                        Block block = getLocation().getWorld().getBlockAt(x,y,z);
                        if(block.getType() == Material.AIR) {

                            Block grounded = BlockUtil.ground(block.getLocation(), 7);
                            getMatch().paint(grounded.getLocation().toVector(), getShooter());

                        } else {

                            getMatch().paint(block.getLocation().toVector(), getShooter());

                        }

                    }

                }

            }

        }

        remove();

    }

    @Override
    public AxisAlignedBB aabb() {

        return ((CraftItem)item).getHandle().getBoundingBox();

    }
}
