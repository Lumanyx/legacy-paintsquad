package de.xenyria.splatoon.ai.projectile;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.TrajectoryCalculation;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.TeamEntity;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public class ProjectileExaminer {

    public static Result examineRayProjectile(Location begin, Location target, double range, Match match, Team team, EntityNPC npc) {

        Vector direction = target.toVector().subtract(begin.toVector()).normalize();
        if(VectorUtil.isValid(direction)) {

            RayTraceResult result = match.getWorldInformationProvider().rayTraceBlocks(begin.toVector(), direction, range, true);
            if(result == null) {

                return new Result(null, true, target, null);

            } else {

                return new Result(null, false, result.getHitPosition().toLocation(match.getWorld()), null);

            }

        } else {

            return new Result(null, false, null, null);

        }

    }

    public static class Result {

        private HitableEntity hitEntity;
        public HitableEntity getHitEntity() { return hitEntity; }

        private boolean targetReached;
        public boolean isTargetReached() { return targetReached; }

        private Location hitLocation;
        public Location getHitLocation() { return hitLocation; }


        public Result(HitableEntity entity, boolean reached, Location hitLocation, Trajectory trajectory) {

            this.hitEntity = entity;
            this.targetReached = reached;
            this.hitLocation = hitLocation;
            this.trajectory = trajectory;

        }

        private Trajectory trajectory;
        public Trajectory getTrajectory() { return trajectory; }

    }

    public static double GRAVITY_CONSTANT = 0.098/2;

    public static Result examineInkProjectile(Location location, Location target, double projectileImpulse, Match match, Team team, SplatoonPlayer plr) {

        return examineInkProjectile(location, target, projectileImpulse, match, team, null, plr, 1d);

    }

    public static Result examineInkProjectile(Location location, Location target, double projectileImpulse, Match match, Team team, @Nullable Block targetBlock, SplatoonPlayer plr, double splitMod) {

        World world = location.getWorld();
        Vector startVector = location.toVector();
        Vector endVector = target.toVector();
        double distance = endVector.distance(startVector);
        boolean isBlock = targetBlock != null;

        if(distance > 1) {

            TrajectoryCalculation calc = new TrajectoryCalculation(new Vector3f(startVector.getX(), startVector.getY(), startVector.getZ()),
                    new Vector3f(endVector.getX(), endVector.getY(), endVector.getZ()), projectileImpulse, GRAVITY_CONSTANT);

            calc.calculate(projectileImpulse*splitMod);

            // Vorberechnete Flugbahnen testen
            ArrayList<Trajectory> foundTrajectories = new ArrayList<>();
            if(calc.found()) {

                if (calc.getBestResult() != null && calc.getBestResult().getVectors().size() >= 1) { foundTrajectories.add(calc.getBestResult()); }
                if (calc.getWorstResult() != null && calc.getWorstResult().getVectors().size() >= 1) { foundTrajectories.add(calc.getWorstResult()); }
                if (!foundTrajectories.isEmpty()) {

                    for (Trajectory trajectory : foundTrajectories) {

                        Vector direction = new Vector(trajectory.getDirection().x, trajectory.getDirection().y, trajectory.getDirection().z);

                        Vector3f firstVec = trajectory.getVectors().get(0);
                        Vector lastPosition = startVector.clone();
                        Vector cursor = new Vector(firstVec.x, firstVec.y, firstVec.z);

                        boolean destroyed = false;
                        int index = 1;
                        double travelledDistance = trajectory.getDistancePerVector();

                        while (!destroyed) {

                            lastPosition = cursor.clone();

                            Vector3f vec3f = null;
                            if (index <= (trajectory.getVectors().size() - 1)) {

                                vec3f = trajectory.getVectors().get(index);

                            } else {

                                Vector position = direction.clone().multiply(travelledDistance);
                                position = position.add(startVector);
                                position = position.add(new Vector(0, trajectory.computeY(travelledDistance, GRAVITY_CONSTANT, projectileImpulse), 0));
                                vec3f = new Vector3f(position.getX(), position.getY(), position.getZ());

                            }

                            cursor = new Vector(vec3f.x, vec3f.y, vec3f.z);
                            if (cursor.getY() <= 0) {

                                destroyed = true;
                                return new Result(null, false, cursor.toLocation(world), trajectory);

                            }

                            // Movement auf Hindernisse überprüfen
                            Vector startMovement = lastPosition.clone();
                            Vector endMovement = cursor.clone();
                            boolean finalMovement = false;
                            if(endMovement.distance(target.toVector()) < trajectory.getDistancePerVector()) {

                                endMovement = target.toVector();
                                finalMovement = true;

                            }

                            //world.spawnParticle(Particle.END_ROD, cursor.getX(), cursor.getY(), cursor.getZ(), 0);

                            Vector directionMovement = endMovement.clone().subtract(startMovement).normalize();
                            AxisAlignedBB bb = new AxisAlignedBB(cursor.getX() - .25, cursor.getY()-.25, cursor.getZ() - .25, cursor.getX() + .25, cursor.getY() + .25, cursor.getZ() + .25);

                            RayProjectile projectile = new RayProjectile(plr, SplatoonWeaponRegistry.getDummy(1), match, startMovement.toLocation(world), directionMovement, 0f);
                            RayTraceResult result = world.rayTraceBlocks(startMovement.toLocation(world), directionMovement, endMovement.distance(startMovement) + .01);
                            Block hitBlock = null;
                            if (result != null) {

                                if (result.getHitBlock() != null) { hitBlock = result.getHitBlock(); }

                            }
                            if (hitBlock == null || !finalMovement) {

                                double minX = Math.min(Math.floor(cursor.getX() - 1), Math.ceil(cursor.getX() + 1));
                                double minY = Math.min(Math.floor(cursor.getY() - 1), Math.ceil(cursor.getY() + 1));
                                double minZ = Math.min(Math.floor(cursor.getZ() - 1), Math.ceil(cursor.getZ() + 1));
                                double maxX = Math.max(Math.floor(cursor.getX() - 1), Math.ceil(cursor.getX() + 1));
                                double maxY = Math.max(Math.floor(cursor.getY() - 1), Math.ceil(cursor.getY() + 1));
                                double maxZ = Math.max(Math.floor(cursor.getZ() - 1), Math.ceil(cursor.getZ() + 1));
                                WorldServer server = ((CraftWorld) world).getHandle();

                                for (double x = minX; x <= maxX; x++) {

                                    for (double y = minY; y <= maxY; y++) {

                                        for (double z = minZ; z <= maxZ; z++) {

                                            boolean exitAfterwards = false;
                                            IBlockData data = server.getType(new BlockPosition(x, y, z));
                                            if (data != null && !AABBUtil.isPassable(CraftBlockData.createData(data).getMaterial())) {

                                                VoxelShape shape = data.getCollisionShape(server, new BlockPosition(x, y, z));
                                                if (shape != null && !shape.isEmpty()) {

                                                    for (Object bb2 : shape.d()) {

                                                        AxisAlignedBB foundBB = (AxisAlignedBB) bb2;
                                                        AxisAlignedBB worldBB = new AxisAlignedBB(
                                                                x + foundBB.minX,
                                                                y + foundBB.minY,
                                                                z + foundBB.minZ,
                                                                x + foundBB.maxX,
                                                                y + foundBB.maxY,
                                                                z + foundBB.maxZ);

                                                        if (worldBB.c(bb)) {

                                                            if(isBlock) {

                                                                int x1 = (int)x, y1 = (int)y, z1 = (int)z;
                                                                if(x1 == targetBlock.getX() && y1 == targetBlock.getY() && z1 == targetBlock.getZ()) {

                                                                    return new Result(null, true, cursor.toLocation(world), trajectory);

                                                                } else {

                                                                    exitAfterwards = true;
                                                                }

                                                            } else {

                                                                // Kollision
                                                                return new Result(null, lastPosition.distance(target.toVector()) < .05, cursor.toLocation(world), trajectory);

                                                            }

                                                        }

                                                    }

                                                }

                                            }

                                            if(exitAfterwards) {

                                                return new Result(null, false, lastPosition.toLocation(plr.getWorld()),  null);

                                            }

                                        }

                                    }

                                }

                            }

                            HitableEntity entity = projectile.getHitEntity(startMovement.distance(endMovement) + .01d, new Predicate<HitableEntity>() {
                                @Override
                                public boolean test(HitableEntity hitableEntity) {

                                    boolean friendlyFire = (hitableEntity instanceof TeamEntity && ((TeamEntity) hitableEntity).getTeam().equals(team));
                                    return !friendlyFire;

                                }
                            }, true, true);
                            if (entity != null) {

                                return new Result(entity, lastPosition.distance(cursor) < .05, cursor.toLocation(world), trajectory);

                            } else {

                                if (cursor.distance(target.toVector()) < 0.05 || finalMovement) {

                                    return new Result(null, true, cursor.toLocation(world), trajectory);

                                }

                            }

                            index++;
                            travelledDistance += trajectory.getDistancePerVector();

                        }

                    }

                }

            } else {

                return new Result(null, false, null, null);

            }

        } else {

            Vector direction = endVector.clone().subtract(startVector).normalize();
            if(VectorUtil.isValid(direction)) {

                RayTraceResult result = world.rayTraceBlocks(location, direction, 1d);
                if(result == null || result.getHitPosition().distance(target.toVector()) <= 0.51) {

                    return new Result(null, true, target, null);

                }

            }

        }

        return new Result(null, false, null, null);

    }

}
