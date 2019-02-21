package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;

public class RayProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    private Location origin;
    private Vector direction;
    public boolean isNullVector() { return direction.length() == 0; }

    public RayProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, Location location, Vector direction, float damage) {

        super(shooter, weapon, match);
        this.origin = location.clone();
        this.direction = direction;
        this.damage = damage;

    }

    public HitableEntity getHitEntity(double maxRange, boolean passPassables) {

        return getHitEntity(maxRange, true, passPassables);

    }

    public HitableEntity getHitEntity(double maxRange, boolean checkObstruction, boolean passPassables) {

        return getHitEntity(maxRange, new Predicate<HitableEntity>() {
            @Override
            public boolean test(HitableEntity hitableEntity) {
                return true;
            }
        }, checkObstruction, passPassables);

    }

    private static void gatherBlocks(Vector start, Vector end, ArrayList<Block> blocks, World world) {

        Vector dir = end.clone().subtract(start).normalize();
        for(double x = 0; x < end.distance(start) + 1; x+=0.5) {

            Vector cursor = start.clone().add(dir.clone().multiply(x));
            Block block = world.getBlockAt((int)cursor.getX(), (int)cursor.getY(), (int)cursor.getZ());
            if(!blocks.contains(block)) { blocks.add(block); }

        }

    }

    public static ArrayList<Block> rayCastBlocks(double finalRange, Location startLocation, Vector direction, Match match) {

        ArrayList<Block> blocks = new ArrayList<>();
        RayTraceResult result1 = match.getWorldInformationProvider().rayTraceBlocks(startLocation.toVector(), direction, finalRange, true);
        if(result1 != null) {

            finalRange = result1.getHitPosition().distance(startLocation.toVector());

        }
        finalRange+=2d;

        Vector cursor = startLocation.toVector();
        for(double dist = 0; dist < finalRange; dist+=0.25d) {

            cursor.add(direction.clone().multiply(.25d));
            Location location = new Location(startLocation.getWorld(), (int)cursor.getX(), (int)cursor.getY(), (int)cursor.getZ());
            Block block1 = BlockUtil.ground(location, 16);
            blocks.add(block1);

        }
        return blocks;

        /*double travelledDistance = 0d;
        if(result1 != null) {

            travelledDistance += startLocation.toVector().distance(result1.getHitPosition());
            gatherBlocks(startLocation.toVector(), result1.getHitPosition(), blocks, startLocation.getWorld());
            Location hitLoc = result1.getHitPosition().toLocation(startLocation.getWorld());
            Block block = result1.getHitBlock();

            if (AABBUtil.isPassable(block.getType())) {

                Location newStartingPoint = hitLoc.add(direction);
                boolean atEnd = false;
                while (!atEnd) {

                    double remainingDist = finalRange - travelledDistance;
                    if(remainingDist > 0.2) {

                        RayTraceResult result2 = startLocation.getWorld().rayTraceBlocks(
                                newStartingPoint, direction, (remainingDist)
                        );

                        if(result2 != null) {

                            Block block1 = result2.getHitBlock();
                            gatherBlocks(newStartingPoint.toVector(), result2.getHitPosition(), blocks, startLocation.getWorld());
                            if(AABBUtil.isPassable(block1.getType())) {

                                newStartingPoint = result2.getHitPosition().add(direction).toLocation(startLocation.getWorld());
                                //if(!blocks.contains(newStartingPoint.getBlock())) { blocks.add(newStartingPoint.getBlock()); }

                            } else {

                                atEnd = true;
                                finalRange = (float) travelledDistance + 1f;

                            }

                        } else {

                            gatherBlocks(newStartingPoint.toVector(), startLocation.clone().add(direction.clone().multiply(finalRange)).toVector(), blocks, startLocation.getWorld());
                            atEnd = true;

                        }

                    } else {

                        atEnd = true;

                    }

                }

            } else {

                finalRange = (float) travelledDistance;

            }

        } else {

            gatherBlocks(startLocation.toVector(), startLocation.clone().add(direction.clone().multiply(finalRange)).toVector(), blocks, startLocation.getWorld());

        }
        return blocks;*/

    }

    public HitableEntity getHitEntity(double maxRange, java.util.function.Predicate<HitableEntity> filter, boolean checkObstruction, boolean canPassPassableBlocks) {

        ArrayList<HitableEntity> arrayList = new ArrayList<>();
        for(HitableEntity entity : getShooter().getMatch().getHitableEntities()) {

            if(ProjectileUtil.manhattanDistance(this, entity) <= (maxRange+1d)) {

                AxisAlignedBB entityAABB = entity.aabb();
                RayTraceResult result = rayTrace(entityAABB, maxRange);
                if (result != null) {

                    double dist = entity.distance(this);
                    boolean obstructionOkay = !checkObstruction || rayTraceWithoutObstruction(entityAABB, maxRange, canPassPassableBlocks);

                    if (entityAABB != null && dist <= maxRange && obstructionOkay) {

                        if (entity.isHit(this) && filter.test(entity)) {

                            arrayList.add(entity);

                        }

                    }

                }

            }

        }
        SplatoonProjectile projectile = this;
        Collections.sort(arrayList, new Comparator<HitableEntity>() {
            @Override
            public int compare(HitableEntity o1, HitableEntity o2) {

                return Double.compare(
                        o1.distance(projectile),
                        o2.distance(projectile)
                );

            }
        });
        if(!arrayList.isEmpty()) {

            return arrayList.get(0);

        }
        return null;

    }

    public RayTraceResult rayTrace(AxisAlignedBB bb, double range) {

        BoundingBox boundingBox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        if(VectorUtil.isValid(direction) && direction.length() > 0) {

            return boundingBox.rayTrace(origin.toVector(), direction, range);

        } else {

            return null;

        }

    }
    public RayTraceResult rayTrace(AxisAlignedBB bb, double range, Vector dir) {

        BoundingBox boundingBox = new BoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        return boundingBox.rayTrace(origin.toVector(), dir, range);

    }
    public boolean rayTraceWithoutObstruction(AxisAlignedBB targetBB, double range, Vector dir, boolean passPassableBlocks) {

        RayTraceResult result = rayTrace(targetBB, range, dir);
        if(result != null) {

            RayTraceResult newRes = checkResultForBlockCollision(result, passPassableBlocks);
            if(newRes == null || newRes.getHitBlock() == null) {

                return true;

            }

        }
        return false;

    }
    public boolean rayTraceWithoutObstruction(AxisAlignedBB targetBB, double range, boolean passPassables) {

        if(isNullVector()) { return true; }

        RayTraceResult result = rayTrace(targetBB, range);
        if(result != null) {

            RayTraceResult newRes = checkResultForBlockCollision(result, passPassables);
            if(newRes == null || newRes.getHitBlock() == null) {

                return true;

            }

        }
        return false;

    }

    public RayTraceResult checkResultForBlockCollision(RayTraceResult result, boolean passPassables) {

        Vector targetPos = result.getHitPosition();
        World world = origin.getWorld();
        double dist = origin.toVector().distance(result.getHitPosition());
        if(isNullVector()) {

            return new RayTraceResult(targetPos.clone(), targetPos.toLocation(getMatch().getWorld()).getBlock(), BlockFace.SELF);

        }

        return getMatch().getWorldInformationProvider().rayTraceBlocks(origin.toVector(), direction, dist, !passPassables);
        //RayTraceResult newRes = world.rayTraceBlocks(origin, direction, dist);
        //return newRes;

    }

    @Override
    public Location getLocation() {
        return origin;
    }

    @Override
    public void onRemove() {

    }

    @Override
    public void tick() {

    }

    @Override
    public AxisAlignedBB aabb() {
        return null;
    }

    public static boolean rayCast(World world, Vector a, Vector b) {

        WorldServer server = ((CraftWorld)world).getHandle();
        MovingObjectPosition pos = server.rayTrace(new Vec3D(a.getX(), a.getY(), a.getZ()),
                new Vec3D(b.getX(), b.getY(), b.getZ()),
                FluidCollisionOption.NEVER, true, true);
        return pos == null;

    }
    public static boolean rayCast(World world, Vector a, Vector b, int... acceptedEntityIDs) {

        WorldServer server = ((CraftWorld)world).getHandle();
        MovingObjectPosition pos = server.rayTrace(new Vec3D(a.getX(), a.getY(), a.getZ()),
                new Vec3D(b.getX(), b.getY(), b.getZ()),
                FluidCollisionOption.NEVER, true, true);

        boolean flag = false;
        if(pos != null && pos.entity != null) {

            for(int i : acceptedEntityIDs) { if(i == pos.entity.getId()) { flag = true; break; }}

        }

        return pos == null || pos.type == MovingObjectPosition.EnumMovingObjectType.MISS || flag;

    }

    public Vector originVec() {

        return origin.toVector();

    }

    private float damage;
    public void updateDamage(float dmg) { this.damage = dmg; }

    @Override
    public boolean dealsDamage() {
        return getDamage() != 0f;
    }

    @Override
    public float getDamage() {
        return damage;
    }
}
