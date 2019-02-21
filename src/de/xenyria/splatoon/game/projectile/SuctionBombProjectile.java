package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Sprinkler;
import de.xenyria.splatoon.game.objects.SuctionBomb;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public class SuctionBombProjectile extends SplatoonProjectile {

    public SuctionBombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

    private Item item;

    public void spawn(double impulse, Location location, Vector direction) {

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(new ItemBuilder(getShooter().getTeam().getColor().getClay()).addEnchantment(Enchantment.DURABILITY, 1).create());
        item.setVelocity(direction.clone().multiply(impulse));
        ((CraftItem)item).getHandle().getBoundingBox().setFilter(NMSUtil.filter);

    }

    @Override
    public Location getLocation() {
        return item.getLocation();
    }

    private ArrayList<UUID> spawnedUUIDs = new ArrayList<>();
    @Override
    public void onRemove() {

        item.remove();
        NMSUtil.broadcastEntityRemovalToSquids(item);
        for(UUID uuid : spawnedUUIDs) {

            Player player = Bukkit.getPlayer(uuid);
            if(player != null) {

                SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

                player1.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(item.getEntityId()));

            }

        }

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

        return new ItemBuilder(getShooter().getTeam().getColor().getClay()).addEnchantment(Enchantment.DURABILITY, 1).create();

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

    private boolean useTrajectory() { return trajectory != null; }

    @Override
    public void tick() {

        if(item.getLocation().getY() < 0) {

            remove();
            return;

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

        Vector nextPos = item.getLocation().toVector();
        if(!useTrajectory()) {

            nextPos = nextPos.add(item.getVelocity().clone());

        } else {

            nextPos = interpolatePosition(trajectoryIndex+1);

        }
        Vector direction = nextPos.clone().subtract(item.getLocation().toVector()).normalize();

        if(VectorUtil.isValid(direction)) {

            RayTraceResult result = getLocation().getWorld().rayTraceBlocks(getLocation(), direction, 1.5d);
            if (result != null) {

                if (result.getHitBlock() != null && canMountTo(result.getHitBlock())) {

                    mount(getLocation().getWorld().getBlockAt(
                            (int) result.getHitBlock().getX(),
                            (int) result.getHitBlock().getY(),
                            (int) result.getHitBlock().getZ()
                    ), result.getHitBlockFace().getOppositeFace());

                }

            } else if (item.isOnGround()) {

                if (canMountTo(item.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                    mount(item.getLocation().getBlock().getRelative(BlockFace.DOWN), BlockFace.DOWN);

                }

            }

        } else {

            if (item.isOnGround()) {

                if (canMountTo(item.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                    mount(item.getLocation().getBlock().getRelative(BlockFace.DOWN), BlockFace.DOWN);

                }

            }

        }

    }

    public boolean canMountTo(Block block) {

        return block.getType().isSolid() && !AABBUtil.isPassable(block.getType());

    }

    public void mount(Block block, BlockFace face) {

        SuctionBomb bomb = new SuctionBomb(getMatch(), getShooter(), block, face.getOppositeFace(), 3.5f);
        getMatch().addGameObject(bomb);
        remove();

    }

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }
}
