package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.util.math.LocalCoordinateSystem;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class Sprinkler extends GameObject implements HitableEntity,RemovableGameObject {

    private SplatoonPlayer player;
    private BlockFace normal, face;
    private Block block;
    private Location spawnPos;

    private ArmorStand stand;

    public Sprinkler(Match match, SplatoonPlayer owner, Block block, BlockFace normal) {

        super(match);
        this.player = owner;
        this.block = block;
        this.normal = normal;
        face = normal.getOppositeFace();
        Location spawnLoc = new Location(player.getWorld(), (int)block.getX(), (int)block.getY(), (int)block.getZ());

        spawnLoc = spawnLoc.add(.5, .5, .5);
        spawnLoc = spawnLoc.add(normal.getDirection().clone().multiply(0.625));
        spawnLoc.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 0);
        spawnPos = spawnLoc.clone();
        axisAlignedBB = new AxisAlignedBB(spawnLoc.getX() - .25, spawnLoc.getY() - .25, spawnLoc.getZ() - .25,
                spawnLoc.getX() + .25, spawnLoc.getY() + .25, spawnLoc.getZ() + .25);

        spawnLoc = spawnLoc.add(0, -1.6, 0);

        stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);


        stand.setVisible(false);
        stand.setCanMove(false);
        stand.setCanTick(false);
        stand.setHelmet(new ItemStack(owner.getTeam().getColor().getGlass()));

        ShootingRay ray = new ShootingRay();
        if(face == BlockFace.UP) {

            ray.pitch = (float) angle;

        } else if(face == BlockFace.DOWN) {

            ray.pitch = -(float) angle;

        } else {

            if(face == BlockFace.NORTH) {

                ray.yaw = (float) (-90f + angle);

            } else if(face == BlockFace.EAST) {

                ray.yaw = (float) angle;

            } else if(face == BlockFace.WEST) {

                ray.yaw = (float) (-180 + angle);

            } else {

                ray.yaw = (float) angle + 90f;

            }

        }
        rays.add(ray);

        ShootingRay secondRay = new ShootingRay();
        secondRay.pitch = ray.pitch;
        secondRay.yaw = ray.yaw;
        rays.add(secondRay);

        if(face == BlockFace.UP || face == BlockFace.DOWN) {

            secondRay.yaw += 180f;

        } else {

            secondRay.pitch += 180f;

        }

    }

    private final double angle = 60f;

    @Override
    public ObjectType getObjectType() {
        return ObjectType.SPRINKLER;
    }

    public void remove() {

        remove = true;
        stand.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnPos, 0);
        stand.remove();
        NMSUtil.broadcastEntityRemovalToSquids(stand);
        getMatch().queueObjectRemoval(this);

    }

    private double health = 25d;
    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile.getShooter() != null && projectile.getShooter().getTeam() != player.getTeam()) {

            if(projectile instanceof DamageDealingProjectile) {

                DamageDealingProjectile projectile1 = (DamageDealingProjectile)projectile;
                if(projectile1.dealsDamage()) {

                    health-=projectile1.getDamage();
                    if(health <= 0d) {

                        remove();
                        player.getMatch().queueObjectRemoval(this);

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
        return projectile.getLocation().distance(spawnPos.clone().add(0, 0, 0));
    }

    @Override
    public int getEntityID() {
        return stand.getEntityId();
    }

    @Override
    public Location getLocation() {
        return spawnPos.clone();
    }

    @Override
    public double height() {
        return 0.625;
    }

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(spawnPos.getX() - .3, spawnPos.getY() - .3, spawnPos.getZ() - .3, spawnPos.getX() + .3, spawnPos.getY() + .3, spawnPos.getZ() + .3);
    }

    private boolean remove;
    @Override
    public boolean isDead() {
        return remove;
    }

    public SplatoonPlayer getOwner() { return player; }


    public class ShootingRay {

        private float yaw,pitch;

    }

    private ArrayList<ShootingRay> rays = new ArrayList<>();
    private final float rotationSpeed = 4f;
    private int rayShootTicker = 0;

    private AxisAlignedBB axisAlignedBB;

    private LocalCoordinateSystem lcs = new LocalCoordinateSystem();

    @Override
    public void onTick() {

        Location location = stand.getLocation();

        if(face == BlockFace.UP || face == BlockFace.DOWN) {

            location.setYaw(location.getYaw() + 1f);
            stand.teleport(location);

            for(ShootingRay ray : rays) {

                ray.yaw+=rotationSpeed;

            }

        } else {

            if(face == BlockFace.NORTH || face == BlockFace.SOUTH) {

                if(face == BlockFace.NORTH) {

                    stand.setHeadPose(stand.getHeadPose().setZ(stand.getHeadPose().getZ() + Math.toRadians(rotationSpeed)));

                } else {

                    stand.setHeadPose(stand.getHeadPose().setZ(stand.getHeadPose().getZ() - Math.toRadians(rotationSpeed)));

                }

            } else {

                if(face == BlockFace.WEST) {

                    stand.setHeadPose(stand.getHeadPose().setX(stand.getHeadPose().getX() - Math.toRadians(rotationSpeed)));

                } else {

                    stand.setHeadPose(stand.getHeadPose().setX(stand.getHeadPose().getX() + Math.toRadians(rotationSpeed)));

                }


            }
            for(ShootingRay ray : rays) {

                ray.pitch+=rotationSpeed;

            }

        }

        rayShootTicker++;
        if(rayShootTicker > 8) {

            rayShootTicker = 0;
            for (ShootingRay ray : rays) {

                if(face != BlockFace.UP && face != BlockFace.DOWN) {

                    Vector targetLoc = (spawnPos.toVector().clone().add(face.getDirection().clone().multiply(-1)));
                    SplatoonServer.broadcastColorParticle(getLocation().getWorld(), targetLoc.getX(), targetLoc.getY(), targetLoc.getZ(), Color.PINK, 1f);
                    BlockFace[] faces = new BlockFace[]{
                            BlockFace.NORTH,
                            BlockFace.EAST,
                            BlockFace.SOUTH,
                            BlockFace.WEST
                    };
                    int i = 0;
                    for(int id = 0; id < faces.length; id++) {

                        if(face == faces[id]) { i = id; break; }

                    }
                    i--;
                    if(i < 0) { i = 3; } else if(i > 3) { i = 0; }
                    BlockFace normalFace = faces[i];
                    Location location1 = new Location(getMatch().getWorld(), targetLoc.getX(), targetLoc.getY(), targetLoc.getZ());
                    location1.setDirection(normalFace.getDirection().clone().normalize());
                    location1.setPitch(ray.pitch);
                    SplatoonServer.broadcastColorParticle(getLocation().getWorld(), location1.getX(), location1.getY(), location1.getZ(), Color.PINK, 1f);

                    Vector end = location1.clone().add(location1.getDirection().normalize().clone().multiply(2)).toVector();
                    Vector start = spawnPos.toVector();


                    Vector dir = end.clone().subtract(start);

                    Location derV = location1.clone();
                    derV.setDirection(dir);

                    InkProjectile projectile = new InkProjectile(player, player.getEquipment().getSecondaryWeapon(), getMatch());
                    projectile.withDamage(3d);
                    projectile.spawn(location1, derV.getYaw(), derV.getPitch(), 0.35f);

                } else {

                    InkProjectile projectile = new InkProjectile(player, player.getEquipment().getSecondaryWeapon(), getMatch());
                    projectile.withDamage(3d);
                    projectile.spawn(this.spawnPos.clone().add(normal.getDirection()), ray.yaw, ray.pitch, 0.35f);

                }

                //getMatch().getWorld().spawnParticle(Particle.END_ROD, origin, 0);

                /*InkProjectile projectile = new InkProjectile(player, player.getEquipment().getSecondaryWeapon(), getMatch());
                projectile.withDamage(3d);
                projectile.spawn(origin, ray.yaw, ray.pitch, 0.35f);*/

            }

        }


    }

    @Override
    public void reset() {
        remove();
        remove = false;
    }
}
