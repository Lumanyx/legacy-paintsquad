package de.xenyria.splatoon.game.objects.toxicmist;

import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

public class ToxicMist extends GameObject {

    public ToxicMist(Match match, Location location, Team team) {

        super(match);
        this.location = location;
        this.team = team;

    }
    private Location location;
    private Team team;

    @Override
    public ObjectType getObjectType() { return ObjectType.SECONDARY; }

    private int ticksToLive = 20*8;
    private float yawOffset = 0f;
    private float pitchOffset = 0f;

    @Override
    public void onTick() {

        pitchOffset+=3f;
        yawOffset+=4.75f;

        double radius = 3.5d;
        for(int i = 0; i < 10; i++) {

            float rad = (float) (new Random().nextFloat()*radius);
            float yaw = new Random().nextFloat()*360;
            float pitch = (new Random().nextFloat()*180) - 90;

            Vector dir = DirectionUtil.yawAndPitchToDirection(yaw, pitch).multiply(rad);
            dir.add(location.toVector());

            SplatoonServer.broadcastColorParticle(location.getWorld(), dir.getX(), dir.getY(), dir.getZ(), team.getColor(), 3f);
            if(RandomUtil.random(10)) {

                SplatoonServer.broadcastColorizedBreakParticle(location.getWorld(), dir.getX(), dir.getY(), dir.getZ(), team.getColor());

            }

        }

        for(SplatoonPlayer player : getMatch().getHumanPlayers()) {

            if(!player.isSpectator()) {

                if(player.getTeam() != team) {

                    if(!player.isSplatted()) {

                        if(player.getLocation().distance(location) <= radius) {

                            player.setDebuffTicks(10);
                            //detectedTargets.add(player);

                        }

                    }

                }

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
}
