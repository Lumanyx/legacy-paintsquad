package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

public class BurstBombProjectile extends SplatoonProjectile {

    private float radius;
    private float maxDamage;

    public BurstBombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, float radius, float maxDamage) {

        super(shooter, weapon, match);
        this.maxDamage = maxDamage;
        this.radius = radius;

    }

    public void spawn(double impulse, Location location) {

        float yaw = getShooter().yaw();
        float pitch = getShooter().pitch() - 35f;

        location = location.clone();
        location.setYaw(yaw); location.setPitch(pitch);
        Vector vec = location.getDirection();

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setCanMobPickup(false);
        item.setPickupDelay(9999);
        item.setItemStack(new ItemBuilder(Material.SUGAR).create());
        item.setVelocity(vec.multiply(impulse));
        getShooter().getMatch().queueProjectile(this);
        ((CraftItem)item).getHandle().getBoundingBox().setFilter(NMSUtil.filter);

    }

    @Override
    public Location getLocation() {
        return item.getLocation();
    }

    @Override
    public void onRemove() {

        if(item != null && !item.isDead()) {

            item.remove();

        }

    }

    @Override
    public void tick() {

        if(item.getLocation().getY() < 0) {

            remove();
            return;

        }

        Vector start = item.getLocation().toVector();
        Vector end = start.clone().add(item.getVelocity());
        Vector dir = end.clone().subtract(start);

        RayProjectile projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), item.getLocation(), dir, maxDamage);
        HitableEntity entity = projectile.getHitEntity(item.getVelocity().length() + .2, new Predicate<HitableEntity>() {
            @Override
            public boolean test(HitableEntity hitableEntity) {

                boolean friendlyFire = (hitableEntity instanceof SplatoonPlayer && ((SplatoonPlayer) hitableEntity).getTeam() == getTeam());

                return hitableEntity != getShooter() && !friendlyFire;

            }
        });
        if(entity != null) {

            detonate();

        }

        RayTraceResult result = getLocation().getWorld().rayTraceBlocks(getLocation(), item.getVelocity().clone().normalize(), 1d);
        if(result != null) {

            if(result.getHitBlock() != null) {

                detonate();
                remove();

            }

        } else if(item.isOnGround()) {

            detonate();
            remove();

        }

    }

    public void detonate() {

        Location lastItemLoc = item.getLocation().clone();

        ArrayList<Vector> positions = new ArrayList<>();
        ArrayList<Vector> directions = new ArrayList<>();
        for(float yaw = 0f; yaw < 360f; yaw+=22.5f) {

            for(float pitch = -90f; pitch < 90f; pitch+=10f) {

                Location location = new Location(getShooter().getLocation().getWorld(), 0,0,0, yaw, pitch);
                Vector origDir = location.getDirection().clone();
                Vector origPos = item.getLocation().toVector();
                origPos = origPos.add(origDir.clone().multiply(radius / 1.33f));

                positions.add(origPos);
                directions.add(location.getDirection().clone().multiply(radius));

            }

        }

        getShooter().getWorld().playSound(item.getLocation(),
                Sound.BLOCK_STONE_BREAK, 1f, 1f);
        getShooter().getWorld().playSound(item.getLocation(),
                Sound.BLOCK_WOOL_HIT, 1f, 2f);
        getShooter().getWorld().playSound(item.getLocation(),
                Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1f, 0.2f);
        item.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE, item.getLocation(), 0);
        for(int i = 0; i < directions.size(); i++) {

            Vector direction = directions.get(i);
            Vector position = positions.get(i);

            double offsetRatio = 0.08;
            double offsetX = new Random().nextDouble() * offsetRatio;
            double offsetY = new Random().nextDouble() * offsetRatio;
            double offsetZ = new Random().nextDouble() * offsetRatio;
            if(new Random().nextBoolean()) { offsetX *= -1; }
            if(new Random().nextBoolean()) { offsetY *= -1; }
            if(new Random().nextBoolean()) { offsetZ *= -1; }

            SplatoonServer.broadcastColorizedBreakParticle(getShooter().getWorld(),
                    position.getX() + offsetX,
                    position.getY() + offsetY,
                    position.getZ() + offsetZ,
                    direction.getX(),
                    direction.getY(),
                    direction.getZ(),
                    getShooter().getTeam().getColor());

        }

        Vector min = new Vector(item.getLocation().getX() - radius, item.getLocation().getY() - radius, item.getLocation().getZ() - radius);
        Vector max = new Vector(item.getLocation().getX() + radius, item.getLocation().getY() + radius, item.getLocation().getZ() + radius);
        double minX = Math.min(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxX = Math.max(min.getX(), max.getX());
        double maxY = Math.max(min.getY(), max.getY());
        double maxZ = Math.max(min.getZ(), max.getZ());

        for(int x = (int)minX; x <= maxX; x++) {

            for(int y = (int)minY; y <= maxY; y++) {

                for(int z = (int)minZ; z <= maxZ; z++) {

                    Vector vec = new Vector(x+.5, y+.5,z+.5);
                    float dist = (float) vec.distance(item.getLocation().toVector());
                    int distInt = (int) dist;
                    if(dist < radius) {

                        final int fX = x;
                        final int fY = y;
                        final int fZ = z;

                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            if(getMatch().isPaintable(getShooter().getTeam(), item.getWorld().getBlockAt(fX,fY,fZ))) {

                                getMatch().paint(new Vector(fX,fY,fZ), getShooter());

                            }

                        }, distInt * 2);


                    }

                }

            }

        }

        // Explosionsschaden
        ArrayList<GameObject> hitObjects = new ArrayList<>();

        for(HitableEntity entity : getMatch().getHitableEntities()) {

            double dist = entity.distance(this);
            if(dist < radius) {

                Vector targetVec = entity.centeredHeightVector();
                Vector direction = targetVec.clone().subtract(lastItemLoc.toVector()).normalize();

                RayProjectile projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), lastItemLoc, direction, 0f);
                if (projectile.rayTraceWithoutObstruction(entity.aabb(), radius, direction)) {

                    if(entity.isHit(projectile)) {

                        if(entity instanceof GroupedObject) {

                            GroupedObject object = (GroupedObject)entity;
                            GameObject root = object.getRoot();

                            if(!hitObjects.contains(root)) {

                                hitObjects.add(root);
                                entity = object.getNearestObject(entity, lastItemLoc);
                                dist = entity.distance(this);

                            } else {

                                continue;

                            }

                        }

                        double maxDist = radius;
                        double factor = dist / maxDist;

                        if(dist >= 1.1d) {

                            double dealtDamage = maxDamage - (maxDamage * factor);
                            projectile.updateDamage((float) dealtDamage);

                        } else {

                            projectile.updateDamage(maxDamage);

                        }

                        entity.onProjectileHit(projectile);

                    }

                }

            }

        }

        remove();

    }

    private Item item;

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }

}
