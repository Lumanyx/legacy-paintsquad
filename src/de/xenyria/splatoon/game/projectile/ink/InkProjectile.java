package de.xenyria.splatoon.game.projectile.ink;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageDealingProjectile;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import it.unimi.dsi.fastutil.Hash;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class InkProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    private static CopyOnWriteArrayList<Integer> spawnedItemIDs = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Integer> getSpawnedItemIDs() { return spawnedItemIDs; }

    @Override
    public Location getLocation() {
        return item.getLocation();
    }

    public InkProjectile(SplatoonPlayer player, SplatoonWeapon weapon, Match match) { super(player, weapon, match); }

    @Override
    public void onRemove() {

        spawnedItemIDs.remove((Integer)item.getEntityId());
        item.remove();
        NMSUtil.broadcastEntityRemovalToSquids(item);

    }

    private DamageReason reason = DamageReason.HUMAN_ERROR;
    public DamageReason getReason() { return reason; }

    private double damage;

    @Override
    public boolean dealsDamage() {
        return damage != 0d;
    }

    public float getDamage() { return (float) damage; }

    private SplatoonWeapon weapon;
    public void withDamage(double damage) { this.damage = damage; }
    public void withReason(DamageReason reason) { this.reason = reason; }

    private double drippingRatio;
    public double getDrippingRatio() { return drippingRatio; }
    public void setDrippingRatio(double drippingRatio) { this.drippingRatio = drippingRatio; }

    private Item item;
    public Item getItem() { return item; }

    private int trajectoryIndex = 1;
    private double travelledDistance;

    private Trajectory trajectory;
    private ArrayList<Vector3f> targetPositions = new ArrayList<Vector3f>();
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

    public boolean usingTrajectory() { return trajectory != null; }

    private Vector spawnLocation;
    private Location trajectoryPlannedHitLocation;
    public void spawn(Trajectory trajectory, Location spawnLocation, Location trajectoryPlannedHitLocation) {

        this.spawnLocation = spawnLocation.toVector();
        this.trajectory = trajectory;
        this.trajectoryPlannedHitLocation = trajectoryPlannedHitLocation;
        ItemStack stack = new ItemStack(getTeam().getColor().getWool());
        EntityItem item1 = new EntityItem(((CraftWorld)spawnLocation.getWorld()).getHandle(),
                spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
                CraftItemStack.asNMSCopy(stack));
        item1.setPosition(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
        targetPositions = trajectory.getVectors();
        travelledDistance+=trajectory.getDistancePerVector();
        item = (Item) item1.getBukkitEntity();
        item1.setNoGravity(true);
        item1.world.addEntity(item1);


        item1.getBoundingBox().setFilter(NMSUtil.filter);
        if(getShooter() != null) {

            getShooter().getMatch().queueProjectile(this);

        }

    }

    public void spawn(Location location, float yaw, float pitch, float impulse) {

        if(pitch > 90f) { pitch = 90f; }
        if(pitch < -90f) { pitch = -90f; }

        ItemStack stack = new ItemStack(getTeam().getColor().getWool());

        EntityItem item1 = new EntityItem(((CraftWorld)location.getWorld()).getHandle(),
                location.getX(), location.getY(), location.getZ(),
                CraftItemStack.asNMSCopy(stack));
        item1.setPosition(location.getX(), location.getY(), location.getZ());
        item1.world.addEntity(item1);

        item = (Item) item1.getBukkitEntity();
        item.setCanMobPickup(false);
        item.setPickupDelay(99999);

        Location loc = location.clone();
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        Vector dir = loc.getDirection().normalize().multiply(impulse);
        item.setVelocity(dir);

        item1.getBoundingBox().setFilter(NMSUtil.filter);
        if(getShooter() != null) {

            getShooter().getMatch().queueProjectile(this);

        }

    }

    private boolean hitOnNextTick = false;
    private boolean ticker = false;

    @Override
    public void tick() {

        if(item != null) {

            ((CraftItem)item).getHandle().getBoundingBox().setFilter(NMSUtil.filter);
            SplatoonServer.broadcastColorParticle(item.getWorld(),
                    item.getLocation().getX(), item.getLocation().getY(), item.getLocation().getZ(),
                    getTeam().getColor(), 0.75f);

        }

        if(!hitOnNextTick) {

            if(paintBelow) {

                int minX = (int) Math.min((int)getLocation().getX() - paintBelowSize, (int)getLocation().getX() + paintBelowSize);
                int minZ = (int) Math.min((int)getLocation().getZ() - paintBelowSize, (int)getLocation().getZ() + paintBelowSize);
                int maxX = (int) Math.max((int)getLocation().getX() - paintBelowSize, (int)getLocation().getX() + paintBelowSize);
                int maxZ = (int) Math.max((int)getLocation().getZ() - paintBelowSize, (int)getLocation().getZ() + paintBelowSize);

                for(int x = minX; x <= maxX; x++) {

                    for(int z = minZ; z <= maxZ; z++) {

                        if(RandomUtil.random(paintBelowPercentage)) {

                            Block block = getLocation().clone().set(x, getLocation().getY(), z).getBlock();
                            block = BlockUtil.ground(block.getLocation(), paintBelowRange);
                            if(getMatch().isPaintable(getTeam(), block)) {

                                if(getShooter() != null) {

                                    getMatch().paint(getShooter(), block.getLocation().toVector(), getTeam());

                                } else {

                                    getMatch().paint(getShooter(), block.getLocation().toVector(), getTeam());

                                }

                            }

                        }

                    }

                }

            }

            Vector vector = item.getLocation().toVector();
            if(!usingTrajectory()) {

                vector = vector.add(item.getVelocity());
                Item item = getItem();

            } else {

                Vector targetPosition = interpolatePosition(trajectoryIndex);
                Vector current = item.getLocation().toVector();
                item.teleport(targetPosition.toLocation(getLocation().getWorld()));
                trajectoryIndex++;
                Vector nextDelta = interpolatePosition(trajectoryIndex);

                Vector vel = nextDelta.clone().subtract(targetPosition);
                vector = vector.add(vel);
                for(Player player : Bukkit.getOnlinePlayers()) {

                    if(player.getWorld().equals(getLocation().getWorld())) {

                        if(player.getLocation().distance(getLocation()) <= 64) {

                            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(((CraftItem)item).getHandle()));

                        }

                    }

                }

            }

            if(item.isOnGround() || item.getLocation().getY() < 0) {

                hitOnNextTick = true;
                return;

            }

            Location location = new Location(item.getWorld(), vector.getX(), vector.getY(), vector.getZ());
            Block block = location.getBlock();
            if (block.getType() != Material.AIR && !AABBUtil.isPassable(block.getType())) {

                VoxelShape shape = ((CraftWorld) location.getWorld()).getHandle().getType(new BlockPosition(block.getX(), block.getY(), block.getZ())).getCollisionShape(
                        ((CraftWorld) location.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ())
                );
                AxisAlignedBB itemAABB = new AxisAlignedBB(
                        Math.min(vector.getX() - 0.125, vector.getX() + .125),
                        Math.min(vector.getY() - 0, vector.getY() + .25),
                        Math.min(vector.getZ() - 0.125, vector.getZ() + .125),
                        Math.max(vector.getX() - 0.125, vector.getX() + .125),
                        Math.max(vector.getY() - 0, vector.getY() + .25),
                        Math.max(vector.getZ() - 0.125, vector.getZ() + .125)
                );

                if (shape != null && !shape.isEmpty()) {

                    for (Object bb1 : shape.d()) {

                        AxisAlignedBB bb = (AxisAlignedBB) bb1;
                        AxisAlignedBB realBB = new AxisAlignedBB(block.getX()+bb.minX, block.getY()+bb.minY, block.getZ()+bb.minZ,block.getX()+bb.maxX,block.getY()+bb.maxY, block.getZ()+bb.maxZ);
                        if (realBB.c(itemAABB)) {

                            hitOnNextTick = true;

                        }

                    }

                }

            }

            for(HitableEntity entity : getMatch().getHitableEntities()) {

                if(entity.isHit(this)) {

                    entity.onProjectileHit(this);
                    SplatoonServer.broadcastColorizedBreakParticle(getLocation().getWorld(),
                            getLocation().getX(), getLocation().getY(), getLocation().getZ(), getColor());
                    remove();

                }

            }

            if(usingTrajectory()) {

                travelledDistance+=trajectory.getDistancePerVector();

            }

        } else {

            Block block = null;

            if(trajectory == null) {

                block = getMatch().getWorld().getBlockAt(item.getLocation().getBlockX(), item.getLocation().getBlockY(), item.getLocation().getBlockZ());

            } else {

                block = getShooter().getWorld().getBlockAt((int)trajectoryPlannedHitLocation.getX(),
                        (int)trajectoryPlannedHitLocation.getY(),
                        (int)trajectoryPlannedHitLocation.getZ());

            }

            int minX = Math.min(block.getX() - 1, block.getX() + 1);
            int minY = Math.min(block.getY() - 1, block.getY() + 1);
            int minZ = Math.min(block.getZ() - 1, block.getZ() + 1);
            int maxX = Math.max(block.getX() - 1, block.getX() + 1);
            int maxY = Math.max(block.getY() - 1, block.getY() + 1);
            int maxZ = Math.max(block.getZ() - 1, block.getZ() + 1);

            for(int x = minX; x <= maxX; x++) {

                for(int y = minY; y <= maxY; y++) {

                    for(int z = minZ; z <= maxZ; z++) {

                        Vector vector = new Vector(x,y,z);
                        double dist = vector.distance(block.getLocation().toVector());
                        double percentage = (dist / (2.56d)) * 100d;
                        if(RandomUtil.random((int) percentage) || dist <= .51) {

                            if(getShooter() != null) {

                                getMatch().paint(vector, getShooter());
                                if (drippingRatio > 0) {

                                    if (RandomUtil.random((int) drippingRatio)) {

                                        getMatch().dripInk(vector, getShooter());

                                    }

                                }

                            } else {

                                getMatch().paint(null, vector, getTeam());

                            }

                        }

                    }

                }

            }

            remove();

        }

    }

    @Override
    public AxisAlignedBB aabb() {

        return new AxisAlignedBB(item.getLocation().getX() - .125, item.getLocation().getY(), item.getLocation().getZ() - .125,
                item.getLocation().getX() + .125, item.getLocation().getY() + .25, item.getLocation().getZ() + .125);

    }

    private boolean paintBelow = false;
    private int paintBelowPercentage, paintBelowRange, paintBelowSize;
    public void setPaintBelowRatio(int percentage, int range, int size) {

        paintBelow = true;
        this.paintBelowPercentage = percentage;
        this.paintBelowRange = range;
        this.paintBelowSize = size;

    }

}
