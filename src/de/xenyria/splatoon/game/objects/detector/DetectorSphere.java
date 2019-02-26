package de.xenyria.splatoon.game.objects.detector;

import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

public class DetectorSphere extends GameObject {

    public DetectorSphere(Match match, Location location, Team team) {

        super(match);
        this.location = location;
        this.team = team;

    }
    private Location location;
    private Team team;

    @Override
    public ObjectType getObjectType() { return ObjectType.SECONDARY; }

    private int ticksToLive = 50;
    private float yawOffset = 0f;
    private float pitchOffset = 0f;

    @Override
    public void onTick() {

        pitchOffset+=3f;
        yawOffset+=4.75f;

        float[] yawOffsets = new float[]{0, 90,180,270};

        double radius = 4.25d;
        for(float f : yawOffsets) {

            float yawOffset1 = yawOffset+f;
            float yawOffset2 = -yawOffset-f;

            Vector dir1 = DirectionUtil.yawAndPitchToDirection(yawOffset1, pitchOffset).multiply(radius);
            Vector dir2 = DirectionUtil.yawAndPitchToDirection(yawOffset2, pitchOffset).multiply(radius);
            Vector dir3 = DirectionUtil.yawAndPitchToDirection(yawOffset1, -pitchOffset).multiply(radius);
            Vector dir4 = DirectionUtil.yawAndPitchToDirection(yawOffset2, -pitchOffset).multiply(radius);
            dir1.add(location.toVector());
            dir2.add(location.toVector());
            dir3.add(location.toVector());
            dir4.add(location.toVector());

            //location.getWorld().spawnParticle(Particle.CRIT, dir1.getX(), dir1.getY(), dir1.getZ(), 0);
            //location.getWorld().spawnParticle(Particle.CRIT, dir2.getX(), dir2.getY(), dir2.getZ(), 0);
            SplatoonServer.broadcastColorParticle(location.getWorld(), dir1.getX(), dir1.getY(), dir1.getZ(), team.getColor(), 1.2f);
            SplatoonServer.broadcastColorParticle(location.getWorld(), dir2.getX(), dir2.getY(), dir2.getZ(), team.getColor(), 1.2f);
            SplatoonServer.broadcastColorParticle(location.getWorld(), dir3.getX(), dir3.getY(), dir3.getZ(), team.getColor(), 1.2f);
            SplatoonServer.broadcastColorParticle(location.getWorld(), dir4.getX(), dir4.getY(), dir4.getZ(), team.getColor(), 1.2f);

        }

        ArrayList<TentaMissleTarget> detectedTargets = new ArrayList<>();
        for(SplatoonPlayer player : getMatch().getHumanPlayers()) {

            if(!player.isSpectator()) {

                if(player.getTeam() != team) {

                    if(!player.isSplatted()) {

                        if(player.getLocation().distance(location) <= radius) {

                            detectedTargets.add(player);

                        }

                    }

                }

            }

        }
        for(GameObject object : getMatch().getGameObjects()) {

            if(object instanceof TentaMissleTarget) {

                if(((TentaMissleTarget) object).getTargetLocationProvider().getLocation().distance(location) <= radius) {

                    detectedTargets.add((TentaMissleTarget) object);

                }

            }

        }

        for(SplatoonPlayer player : getMatch().getPlayers(team)) {

            for(TentaMissleTarget target : detectedTargets) {

                player.highlight(target, 40);

            }

        }

        ticksToLive--;
        if(ticksToLive < 1) {

            getMatch().queueObjectRemoval(this);

        }

    }

    @Override
    public void reset() {

    }

    @Override
    public void onRemove() {

    }
}
