package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InkRail extends GameObject implements HitableEntity {

    public static final String NOT_ACTIVE = "§8Sepiadukt";
    private ArrayList<ArmorStand> joints = new ArrayList<>();

    private ArrayList<Vector> vectors = new ArrayList<>();
    public ArrayList<Vector> getVectors() { return vectors; }

    public static final double ARMORSTAND_HEAD_OFFSET = 1.625;

    public InkRail(Match match, Vector... vectors) {

        super(match);
        for(Vector vector : vectors) { this.vectors.add(vector); }

        // Indikator
        Location location = new Location(getMatch().getWorld(), vectors[0].getX(), vectors[0].getY(), vectors[0].getZ(), 0f, 0f);
        location.setY(location.getY() - ARMORSTAND_HEAD_OFFSET);

        double minX = Math.min(vectors[0].getX() - 0.5, vectors[0].getX() + 0.5);
        double minY = Math.min(vectors[0].getY() - 1, vectors[0].getY() + 1);
        double minZ = Math.min(vectors[0].getZ() - 0.5, vectors[0].getZ() + 0.5);
        double maxX = Math.max(vectors[0].getX() - 0.5, vectors[0].getX() + 0.5);
        double maxY = Math.max(vectors[0].getY() - 1, vectors[0].getY() + 1);
        double maxZ = Math.max(vectors[0].getZ() - 0.5, vectors[0].getZ() + 0.5);

        firstJointAABB = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        boolean first = true;
        for(Vector vector : vectors) {

            Location location1 = location.clone();

            ArmorStand stand = (ArmorStand) match.getWorld().spawnEntity(location1, EntityType.ARMOR_STAND);
            if(first) {

                stand.setCustomNameVisible(true);
                stand.setCustomName(NOT_ACTIVE);
                location1 = location1.set(vector.getX(), vector.getY() - (ARMORSTAND_HEAD_OFFSET / 2) - .3, vector.getZ());

            } else {

                location1 = location1.set(vector.getX(), vector.getY() - (ARMORSTAND_HEAD_OFFSET / 2) - .3, vector.getZ());


            }
            stand.teleport(location1);

            stand.setSmall(true);
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));
            joints.add(stand);
            first = false;

        }

    }

    public void updateOwningTeam(Team team) {

        this.owningTeam = team;
        if(team != null) {

            boolean first = true;
            for(ArmorStand stand : joints) {

                if(first) { stand.setCustomName(team.getColor().prefix() + "Sepiadukt"); }
                stand.setHelmet(new ItemStack(team.getColor().getGlass()));

                first = false;

            }

        } else {

            boolean first = true;
            for(ArmorStand stand : joints) {

                if(first) { stand.setCustomName(NOT_ACTIVE); }
                stand.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));

                first = false;

            }

        }

    }

    private ArrayList<Vector> track = new ArrayList<>();
    private ArrayList<Joint> jointIndexInfo = new ArrayList<>();

    public ArrayList<Vector> getTrack() { return track; }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile.getShooter() != null) {

            updateOwningTeam(projectile.getShooter().getTeam());

        }

    }

    private AxisAlignedBB firstJointAABB;

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            /*EntityArmorStand stand = ((CraftArmorStand)joints.get(0)).getHandle();
            Vector pos = stand.getBukkitEntity().getLocation().toVector();
            pos = pos.add(new Vector(0, joints.get(0).getEyeHeight() + .325, 0));
            return RayProjectile.rayCast(stand.getBukkitEntity().getWorld(), ((RayProjectile)projectile).originVec(), pos, stand.getId());
            */
            return true;

        }
        return firstJointAABB.c(projectile.aabb());

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().toVector().distance(joints.get(0).getLocation().toVector());
    }

    @Override
    public int getEntityID() {
        return joints.get(0).getEntityId();
    }

    @Override
    public Location getLocation() {
        return this.joints.get(0).getLocation().clone().add(0, 1.6, 0);
    }

    @Override
    public double height() {
        return 0.625;
    }

    @Override
    public AxisAlignedBB aabb() {

        ArmorStand stand = joints.get(0);
        return new AxisAlignedBB(stand.getLocation().getX() - .3, stand.getLocation().getY() + 1.6, stand.getLocation().getZ() - .3,
                stand.getLocation().getX() + .3, stand.getLocation().getY() + 2.2, stand.getLocation().getZ() + .3);

    }

    @Override
    public boolean isDead() {
        return false;
    }

    public class Joint {

        public Vector start, end;
        public int startIndx, endIndx;

    }

    public Joint getLineVectors(int ourIndex) {

        for(Joint joint : jointIndexInfo) {

            if(ourIndex >= joint.startIndx && ourIndex <= joint.endIndx) {

                return joint;

            }

        }
        return null;

    }

    public void interpolateVectors() throws InvalidObjectConfigurationException {

        for(int i = 0; i < (vectors.size()); i++) {

            int nextIndex = i+1;
            if(nextIndex < (vectors.size())) {

                Vector vectorA = vectors.get(i);
                Vector vectorB = vectors.get(nextIndex);
                double distance = vectorA.distance(vectorB);

                if(distance >= 1) {

                    Joint joint = new Joint();
                    joint.startIndx = track.size() - 1;
                    joint.start = vectorA.clone();
                    joint.end = vectorB.clone();

                    Vector unit = vectorA.clone().subtract(vectorB).multiply(-1).normalize().multiply(.1);
                    int times = (int) (distance * 10);
                    Vector cursor = vectorA.clone();
                    for(int x = 0; x < times; x++) {

                        Vector pos = cursor.clone().add(unit.clone().multiply(x));
                        track.add(pos);

                    }
                    joint.endIndx = track.size() - 1;
                    jointIndexInfo.add(joint);

                } else {

                    throw new InvalidObjectConfigurationException(ObjectType.INK_RAIL, "Ink-Rail-Vektoren haben einen ungültigen Abstand!");

                }

            }

        }

    }


    public ObjectType getObjectType() { return ObjectType.INK_RAIL; }

    private Team owningTeam = null;
    public Team getOwningTeam() { return owningTeam; }
    private int particleTicker = 0;

    @Override
    public void onTick() {

        for(SplatoonPlayer player : getMatch().getAllPlayers()) {

            if(player instanceof SplatoonHumanPlayer && player.getTeam() == owningTeam && !player.isRidingOnInkRail() && player.isSquid()) {

                for(Vector vector : track) {

                    if(vector.distance(player.getLocation().toVector()) < 2) {

                        double minX = Math.min(vector.getX() - .5, vector.getX() + .5);
                        double minY = Math.min(vector.getY() - .5, vector.getY() + .5);
                        double minZ = Math.min(vector.getZ() - .5, vector.getZ() + .5);
                        double maxX = Math.max(vector.getX() - .5, vector.getX() + .5);
                        double maxY = Math.max(vector.getY() - .5, vector.getY() + .5);
                        double maxZ = Math.max(vector.getZ() - .5, vector.getZ() + .5);
                        double curX = player.getLocation().getX();
                        double curY = player.getLocation().getY();
                        double curZ = player.getLocation().getZ();

                        if (player.canRideInkRail() && curX >= minX && curX <= maxX && curY >= minY && curY <= maxY && curZ >= minZ && curZ <= maxZ) {

                            player.beginRidingInkRail(this);

                        }

                    }

                }

            }

        }

        if(owningTeam != null) {

            particleTicker++;
            if(particleTicker > 6) {

                int particleSubTicker = 0;
                particleTicker = 0;
                for(Vector vector : track) {

                    particleSubTicker++;
                    if(particleSubTicker > 2) {

                        particleSubTicker = 0;
                        SplatoonServer.broadcastColorParticle(getMatch().getWorld(),
                                vector.getX(),
                                vector.getY(),
                                vector.getZ(),
                                owningTeam.getColor(), 1f);


                    }

                }

            }

        }

    }

    @Override
    public void reset() {

        updateOwningTeam(null);

    }

    @Override
    public void onRemove() {

        for(ArmorStand stand : joints) {

            stand.remove();

        }

    }

    public Location nearestLocation(Location loc) {

        Vector vector = loc.toVector();

        double lowestYet = -1;
        Vector targetVector = null;

        for(Vector vector1 : track) {

            boolean isLower = false;
            if(lowestYet == -1 || (vector1.distance(vector)) < lowestYet) { lowestYet = vector1.distance(vector); isLower = true; }
            if(isLower) { targetVector = vector1; }

        }

        return new Location(loc.getWorld(), targetVector.getX(), targetVector.getY(), targetVector.getZ(), loc.getYaw(), loc.getPitch());

    }

    public Vector vectorFor(int ridingVectorIndex) {

        if(ridingVectorIndex < track.size() - 1) {

            return track.get(ridingVectorIndex);

        }

        return null;

    }

    public void moveToNearestPosition(SplatoonPlayer splatoonPlayer) {

        Location loc = splatoonPlayer.getLocation();
        Vector vector = loc.toVector();

        double lowestYet = -1;
        int totindx = 0;
        int indx = 0;
        Vector targetVector = null;

        for(Vector vector1 : track) {

            boolean isLower = false;
            if(lowestYet == -1 || (vector1.distance(vector)) < lowestYet) { lowestYet = vector1.distance(vector); isLower = true; }
            if(isLower) { targetVector = vector1; indx = totindx; }
            totindx++;

        }

        Location location = new Location(loc.getWorld(), targetVector.getX(), targetVector.getY(), targetVector.getZ(), loc.getYaw(), loc.getPitch());
        splatoonPlayer.updateRidingIndex(indx);
        System.out.println("Index: " + indx);

        if(splatoonPlayer.isSquid()) {

            Vector start = splatoonPlayer.getLocation().toVector().clone();
            Vector dir = targetVector.clone().subtract(start);
            splatoonPlayer.forceNMSSquidPosition(location);

        } else {

            splatoonPlayer.teleport(location);

        }

    }

}
