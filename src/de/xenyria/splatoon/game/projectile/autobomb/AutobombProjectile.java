package de.xenyria.splatoon.game.projectile.autobomb;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.AutobombObject;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.GroupedObject;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public class AutobombProjectile extends SplatoonProjectile {

    private float radius;
    private float maxDamage;

    public AutobombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

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

    public ItemStack getBombItemStack() {

        return new ItemBuilder(Material.CHICKEN_SPAWN_EGG).create();

    }

    private Vector spawnLocation = null;
    private Location targetLocation = null;
    private Trajectory trajectory = null;
    private ArrayList<Vector3f> targetPositions = null;
    private double travelledDistance = 0d;
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
                    spawnedUUIDs.add(player.getUniqueId());

                }

            }

        }
        applyVelocity();

    }
    private ArrayList<UUID> spawnedUUIDs = new ArrayList<>();

    public void spawn(double impulse, Location location) {

        float yaw = getShooter().yaw();
        float pitch = getShooter().pitch() - 35f;

        location = location.clone();
        location.setYaw(yaw); location.setPitch(pitch);
        Vector vec = location.getDirection();

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setCanMobPickup(false);
        item.setPickupDelay(9999);
        item.setItemStack(new ItemBuilder(Material.CHICKEN_SPAWN_EGG).create());
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

        if(useTrajectory()) {

            NMSUtil.broadcastEntityRemovalToSquids(item);
            for(UUID uuid : spawnedUUIDs) {

                Player player = Bukkit.getPlayer(uuid);
                if(player != null) {

                    SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

                    player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(item.getEntityId()));

                }

            }

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
        HitableEntity entity = projectile.getHitEntity(1d, new Predicate<HitableEntity>() {
            @Override
            public boolean test(HitableEntity hitableEntity) {

                boolean friendlyFire = (hitableEntity instanceof SplatoonPlayer && ((SplatoonPlayer) hitableEntity).getTeam() == getTeam());

                return hitableEntity != getShooter() && !friendlyFire;

            }
        }, true,true);
        if(entity != null) {

            detonate();

        }

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

        if(useTrajectory()) {

            Vector nextPos = interpolatePosition(trajectoryIndex+1);
            World world = ((CraftWorld)getLocation().getWorld()).getHandle();

            BlockPosition position = new BlockPosition(nextPos.getX(), nextPos.getY(), nextPos.getZ());
            WorldServer server = (WorldServer) world;
            IBlockData data = server.getTypeIfLoaded(position);
            if(data != null) {

                VoxelShape shape = data.getCollisionShape(world, position);
                if(shape != null && !shape.isEmpty()) {

                    for(Object bb : shape.d()) {

                        AxisAlignedBB bb1 = (AxisAlignedBB) bb;

                        AxisAlignedBB globalAabb = new AxisAlignedBB(
                                position.getX()+bb1.minX,
                                position.getY()+bb1.minY,
                                position.getZ()+bb1.minZ,
                                position.getX()+bb1.maxX,
                                position.getY()+bb1.maxY,
                                position.getZ()+bb1.maxZ
                        );

                        if(globalAabb.c(aabb())) {

                            detonate();
                            remove();
                            return;

                        }

                    }

                }

            }

            if (item.isOnGround()) {

                detonate();
                remove();

            }

        } else {

            RayTraceResult result = getLocation().getWorld().rayTraceBlocks(getLocation(), item.getVelocity().clone().normalize(), 1d);
            if (result != null) {

                if (result.getHitBlock() != null) {

                    detonate();
                    remove();

                }

            } else if (item.isOnGround()) {

                detonate();
                remove();

            }

        }

    }

    private boolean useTrajectory() { return trajectory != null; }

    public void detonate() {

        Location lastItemLoc = item.getLocation().clone();
        AutobombObject object = new AutobombObject(getMatch(), lastItemLoc, getShooter());
        getMatch().addGameObject(object);
        remove();

    }

    private Item item;

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }

}
