package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

public class BombProjectile extends SplatoonProjectile {

    private int explosionTicks;
    private float radius;
    private float maxDamage;
    private boolean sound = false;

    public BombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, float radius, int explosionTicks, float maxDamage, boolean sound) {

        super(shooter, weapon, match);
        this.explosionTicks = explosionTicks;
        this.maxDamage = maxDamage;
        this.radius = radius;

    }

    private Trajectory trajectory = null;
    private Vector spawnLocation;
    private Location targetLocation;
    private ArrayList<Vector3f> targetPositions = new ArrayList<>();
    private double travelledDistance = 0d;

    public ItemStack getBombItemStack() {

        return new ItemBuilder(Material.GUNPOWDER).create();

    }

    public void applyVelocity() {

        if(!item.isOnGround()) {

            Vector spawnTarget = interpolatePosition(trajectoryIndex+1);
            Vector delta = spawnTarget.clone().subtract(getLocation().toVector());
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (player.getWorld().equals(getLocation().getWorld())) {

                    if (player.getLocation().distance(getLocation()) <= 64) {

                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityVelocity(
                                item.getEntityId(),
                                delta.getX(),
                                delta.getY(),
                                delta.getZ()
                        ));

                    }

                }

            }

        } else {

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (player.getWorld().equals(getLocation().getWorld())) {

                    if (player.getLocation().distance(getLocation()) <= 64) {

                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityVelocity(
                                item.getEntityId(),
                                0d, 0d, 0d
                        ));

                    }

                }

            }

        }

    }

    public boolean useTrajectory() { return trajectory != null; }
    public void spawn(Trajectory trajectory, Location spawnLocation, Location targetLocation) {

        this.spawnLocation = spawnLocation.toVector();
        this.trajectory = trajectory;
        this.targetLocation = targetLocation;
        ItemStack stack = getBombItemStack();
        EntityItem item1 = new EntityItem(((CraftWorld)spawnLocation.getWorld()).getHandle(),
                spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
                CraftItemStack.asNMSCopy(stack));
        item1.setPosition(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
        targetPositions = trajectory.getVectors();
        travelledDistance+=trajectory.getDistancePerVector();
        item = (Item) item1.getBukkitEntity();
        item1.setNoGravity(true);
        item1.locX = spawnLocation.getX();
        item1.locY = spawnLocation.getY();
        item1.locZ = spawnLocation.getZ();
        item1.motX = 0d;
        item1.motY = 0d;
        item1.motZ = 0d;

        item1.getBoundingBox().setFilter(NMSUtil.filter);
        getShooter().getMatch().queueProjectile(this);

        for(Player player : Bukkit.getOnlinePlayers()) {

            if(player.getWorld().equals(spawnLocation.getWorld())) {

                if(player.getLocation().distance(spawnLocation) <= 64) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                            new PacketPlayOutSpawnEntity(item1, 2, 32, new BlockPosition(
                                    spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ()
                            ))
                    );
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                            new PacketPlayOutEntityMetadata(item1.getId(), item1.getDataWatcher(), false
                            )
                    );

                }

            }

        }
        applyVelocity();

    }

    public void spawn(double impulse, Location location) {

        float yaw = getShooter().yaw();
        float pitch = getShooter().pitch()-35f;

        location = location.clone();
        location.setYaw(yaw); location.setPitch(pitch);
        Vector vec = location.getDirection();

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setCanMobPickup(false);
        item.setPickupDelay(9999);
        item.setItemStack(getBombItemStack());
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
            NMSUtil.broadcastEntityRemovalToSquids(item);

        }

    }

    private boolean warningSound = false;

    private boolean forceExplosion = false;
    public boolean doForceExplosion() { return forceExplosion; }
    public void forceExplosion() { forceExplosion = true; }

    public Vector interpolatePosition(int trajectoryIndex) {

        if(trajectoryIndex <= (targetPositions.size() - 1)) {

            Vector3f vec = targetPositions.get(trajectoryIndex);
            return new Vector(vec.x, vec.y, vec.z);

        } else {

            Vector dir = new Vector(trajectory.getDirection().x, trajectory.getDirection().y, trajectory.getDirection().z);
            Vector vector = spawnLocation.clone().add(dir.clone().multiply(travelledDistance));
            vector = vector.add(new Vector(0, trajectory.computeY(travelledDistance, ProjectileExaminer.GRAVITY_CONSTANT, trajectory.getOriginSpeed()), 0));
            return vector;

        }

    }

    private int trajectoryIndex = 1;

    @Override
    public void tick() {

        if(item != null && !item.isDead()) {

            ((CraftItem)item).getHandle().getBoundingBox().setFilter(NMSUtil.filter);

        }

        if(item.getLocation().getY() < 0) {

            item.remove();
            for(Player player : Bukkit.getOnlinePlayers()) {

                if(player.getWorld().equals(getLocation().getWorld())) {

                    if(player.getLocation().distance(getLocation()) <= 64) {

                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                                new PacketPlayOutEntityDestroy(item.getEntityId())
                        );

                    }

                }

            }
            NMSUtil.broadcastEntityRemovalToSquids(item);
            remove();
            return;

        }

        if(explosionTicks < 7 && !warningSound) {

            if(sound) {

                item.getLocation().getWorld().playSound(item.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.6f);

            }
            warningSound = true;

        }
        if(explosionTicks < 14) {

            item.getLocation().getWorld().spawnParticle(Particle.SMOKE_NORMAL, item.getLocation(), 0);

        }
        Vector start = item.getLocation().toVector();
        Vector end = start.clone().add(item.getVelocity());
        Vector dir = end.clone().subtract(start);



        if(useTrajectory() && !item.isOnGround()) {

            Vector targetPosition = interpolatePosition(trajectoryIndex);
            Vector current = item.getLocation().toVector();
            Vector delta = targetPosition.clone().subtract(current);

            double expectedY = item.getLocation().getY() + delta.getY();

            ((CraftItem)item).getHandle().move(EnumMoveType.SELF,
                    delta.getX(), delta.getY(), delta.getZ());

            if(item.getLocation().getY() != expectedY) {

                ((CraftItem)item).getHandle().onGround = true;

            }

            trajectoryIndex++;
            Vector nextDelta = interpolatePosition(trajectoryIndex);

            Vector vel = nextDelta.clone().subtract(targetPosition);
            //vector = vector.add(vel);
            for(Player player : Bukkit.getOnlinePlayers()) {

                if(player.getWorld().equals(getLocation().getWorld())) {

                    if(player.getLocation().distance(getLocation()) <= 64) {

                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(((CraftItem)item).getHandle()));

                    }

                }

            }
            travelledDistance+=trajectory.getDistancePerVector();
            applyVelocity();

        }

        if(useTrajectory() && item.isOnGround()) {

            item.setVelocity(new Vector(0,0,0));
            applyVelocity();

        }

        if(explosionTicks < 1 || forceExplosion) {

            if(sound) {

                item.getLocation().getWorld().playSound(item.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

            }

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
                    Sound.ENTITY_GENERIC_EXPLODE, 0.2f, 0.2f);
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

                                if(getMatch().isPaintable(getShooter().getTeam(), fX,fY,fZ)) {

                                    getMatch().paint(getShooter(), new Vector(fX,fY,fZ), getShooter().getTeam());

                                }

                            }, distInt * 2);


                        }

                    }

                }

            }

            Location lastItemLoc = item.getLocation();

            if(useTrajectory()) {

                for(Player player : Bukkit.getOnlinePlayers()) {

                    if(player.getWorld().equals(getLocation().getWorld())) {

                        if(player.getLocation().distance(getLocation()) <= 256) {

                            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                                    new PacketPlayOutEntityDestroy(item.getEntityId())
                            );

                        }

                    }

                }

            }

            item.remove();
            NMSUtil.broadcastEntityRemovalToSquids(item);
            remove();

            // Explosionsschaden
            ArrayList<GameObject> hitObjects = new ArrayList<>();

            for(HitableEntity entity : getMatch().getHitableEntities()) {

                double dist = entity.distance(this);
                if(dist < radius) {

                    Vector targetVec = entity.centeredHeightVector();
                    Vector direction = targetVec.clone().subtract(lastItemLoc.toVector()).normalize();

                    RayProjectile projectile = new RayProjectile(getShooter(), getWeapon(), getMatch(), lastItemLoc.clone().add(0, 0.125, 0), direction, 0f);
                    if (projectile.rayTraceWithoutObstruction(entity.aabb(), radius, direction, true)) {

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

                            double dealtDamage = maxDamage - (maxDamage * factor);
                            projectile.updateDamage((float) dealtDamage);
                            entity.onProjectileHit(projectile);

                        }

                    }

                }

            }

        } else {

            if(item.isOnGround()) {

                explosionTicks--;

            }

        }

    }

    private Item item;

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }

}
