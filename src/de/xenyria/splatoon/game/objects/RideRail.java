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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class RideRail extends GameObject implements HitableEntity {

    public static final String NOT_ACTIVE = "§8Surfschiene";
    private ArmorStand stand;

    private ArrayList<Vector> vectors = new ArrayList<>();
    public ArrayList<Vector> getVectors() { return vectors; }

    private AxisAlignedBB bb;

    public RideRail(Match match, Vector... vectors) {

        super(match);
        for(Vector vector : vectors) { this.vectors.add(vector); }


        double minX = Math.min(vectors[0].getX() - 0.5, vectors[0].getX() + 0.5);
        double minY = Math.min(vectors[0].getY() - 0.5, vectors[0].getY() + 0.5);
        double minZ = Math.min(vectors[0].getZ() - 0.5, vectors[0].getZ() + 0.5);
        double maxX = Math.max(vectors[0].getX() - 0.5, vectors[0].getX() + 0.5);
        double maxY = Math.max(vectors[0].getY() - 0.5, vectors[0].getY() + 0.5);
        double maxZ = Math.max(vectors[0].getZ() - 0.5, vectors[0].getZ() + 0.5);

        bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        // Indikator
        Location location = new Location(getMatch().getWorld(), vectors[0].getX(), vectors[0].getY(), vectors[0].getZ(), 0f, 0f);
        location.setY(location.getY() - 1.625f);
        stand = (ArmorStand) match.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setCustomNameVisible(true);
        stand.setCustomName(NOT_ACTIVE);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));

    }

    public void updateOwningTeam(Team team) {

        this.owningTeam = team;
        if(team != null) {

            stand.setCustomName(team.getColor().prefix() + "Surfschiene");
            stand.setHelmet(new ItemStack(team.getColor().getGlass()));

        } else {

            stand.setCustomName(NOT_ACTIVE);
            stand.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));

        }

    }

    private int initializationTicker = 0;
    public boolean isInitializing() { return initializationTicker != (track.size() - 1); }

    private ArrayList<Vector> track = new ArrayList<>();
    public ArrayList<Vector> getTrack() { return track; }

    public void interpolateVectors() throws InvalidObjectConfigurationException {

        for(int i = 0; i < (vectors.size()); i++) {

            int nextIndex = i+1;
            if(nextIndex < (vectors.size())) {

                Vector vectorA = vectors.get(i);
                Vector vectorB = vectors.get(nextIndex);
                double distance = vectorA.distance(vectorB);

                if(distance >= 1) {

                    Vector unit = vectorA.clone().subtract(vectorB).multiply(-1).normalize().multiply(.25);
                    int times = (int) (distance * 4);
                    Vector cursor = vectorA.clone();
                    for(int x = 0; x < times; x++) {

                        Vector pos = cursor.clone().add(unit.clone().multiply(x));
                        track.add(pos);

                    }

                } else {

                    throw new InvalidObjectConfigurationException(ObjectType.INK_RAIL, "Ride-Rail-Vektoren haben einen ungültigen Abstand!");

                }

            }

        }

    }


    @Override
    public ObjectType getObjectType() { return ObjectType.RIDE_RAIL; }

    private Team owningTeam = null;
    public Team getOwningTeam() { return owningTeam; }
    private int particleTicker = 0;
    private float rotationMomentum = 0f;
    public static final float PEAK_ROTATION_MOMENTUM = 12f;
    public static final float ROTATION_MOMENTUM_INCREMENT = 0.12f;

    @Override
    public void onTick() {

        if(owningTeam != null) {

            if(rotationMomentum < PEAK_ROTATION_MOMENTUM) { rotationMomentum+=ROTATION_MOMENTUM_INCREMENT; }

        } else {

            if(rotationMomentum > 0f) {

                rotationMomentum-=PEAK_ROTATION_MOMENTUM;

            }

        }
        if(rotationMomentum < 0f) { rotationMomentum = 0f; }

        if(rotationMomentum != 0f) {

            Location location = stand.getLocation();
            location.setYaw(location.getYaw() + rotationMomentum);
            stand.teleport(location);

        }

        for(SplatoonPlayer player : getMatch().getAllPlayers()) {

            if(player instanceof SplatoonHumanPlayer && player.getTeam() == owningTeam && !player.isRidingOnInkRail()) {

                for(Vector vector : track) {

                    double minX = Math.min(vector.getX() - .5, vector.getX() + .5);
                    double minY = Math.min(vector.getY() - .5, vector.getY() + .5);
                    double minZ = Math.min(vector.getZ() - .5, vector.getZ() + .5);
                    double maxX = Math.max(vector.getX() - .5, vector.getX() + .5);
                    double maxY = Math.max(vector.getY() - .5, vector.getY() + .5);
                    double maxZ = Math.max(vector.getZ() - .5, vector.getZ() + .5);
                    double curX = player.getLocation().getX();
                    double curY = player.getLocation().getY();
                    double curZ = player.getLocation().getZ();
                    if(player.canRideRideRail() && curX >= minX && curX <= maxX && curY >= minY && curY <= maxY && curZ >= minZ && curZ <= maxZ) { player.beginRidingRideRail(this); }

                }

            }

        }

        boolean init = isInitializing();
        if(init) {

            initializationTicker++;

        }

        if(owningTeam != null) {

            particleTicker++;
            if(particleTicker > 3) {

                particleTicker = 0;
                int index = 0;
                for(Vector vector : track) {

                    if(!(init && index > initializationTicker)) {

                        SplatoonServer.broadcastColorParticle(getMatch().getWorld(),
                                vector.getX(),
                                vector.getY(),
                                vector.getZ(),
                                owningTeam.getColor(), 1f);

                    }
                    index++;

                }

            }

        }

    }

    @Override
    public void reset() {

        updateOwningTeam(null);

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
        if(splatoonPlayer.isSquid()) {

            Vector vector1 = splatoonPlayer.getLocation().toVector();
            Vector vector2 = location.toVector();
            Vector delta = vector2.subtract(vector1);
            //splatoonPlayer.forceSquidMovement(delta.getX(), delta.getY(), delta.getZ(), true);

        } else {

            splatoonPlayer.teleport(location);

        }

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile.getShooter() != null) {

            updateOwningTeam(projectile.getShooter().getTeam());

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            /*Vector pos = stand.getLocation().toVector();
            pos = pos.add(new Vector(0, stand.getEyeHeight() + .325, 0));
            boolean res = RayProjectile.rayCast(stand.getWorld(), ((RayProjectile)projectile).originVec(), pos, stand.getEntityId());
            return res;*/
            return true;

        }

        return bb.c(projectile.aabb());

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().distance(stand.getLocation());
    }

    @Override
    public int getEntityID() {
        return stand.getEntityId();
    }

    @Override
    public Location getLocation() {
        return stand.getLocation().clone().add(0, 1.6, 0);
    }

    @Override
    public double height() {
        return 0.625;
    }

    @Override
    public AxisAlignedBB aabb() {
        return bb;
    }

    @Override
    public boolean isDead() {
        return false;
    }
}
