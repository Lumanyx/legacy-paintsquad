package de.xenyria.splatoon.game.objects.inkstorm;

import de.xenyria.core.math.AngleUtil;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.RandomUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Random;

public class InkCloud extends GameObject {

    public static final double HEIGHT = 12;
    private SplatoonPlayer player;
    private Vector direction;
    private Vector position;

    private float radius = 12f;
    private int ticksToLive = 300;

    public InkCloud(Match match, SplatoonPlayer shooter, Location location, Vector direction) {

        super(match);
        this.player = shooter;
        this.position = location.toVector().add(new Vector(0, 12, 0));
        this.direction = direction;
        match.addGameObject(this);

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.SPECIAL;
    }

    private int ticksToParticle = 0;
    private int ticksToDrop = 2;

    @Override
    public void onTick() {

        ticksToLive--;
        if(ticksToLive < 1) {

            getMatch().queueObjectRemoval(this);
            return;

        }

        ticksToParticle--;
        if(ticksToParticle < 1) {

            ticksToParticle = 4;
            position = position.add(direction.clone().multiply(.22));
            for (float i = 0; i < radius; i += 0.25f) {

                float angle = new Random().nextInt(360);
                Vector offset = DirectionUtil.yawAndPitchToDirection(angle, 0);
                Vector finalPos = position.clone().add(offset.clone().multiply(i));
                SplatoonServer.broadcastColorParticle(getMatch().getWorld(), finalPos.getX(), finalPos.getY(), finalPos.getZ(), player.getTeam().getColor(), 3f);

                if(RandomUtil.random(10)) {

                    Block below = BlockUtil.ground(position.toLocation(getMatch().getWorld()), 32);
                    if(getMatch().isPaintable(player.getTeam(), (int)finalPos.getX(),
                            (int)finalPos.getY(), (int)finalPos.getZ())) {

                        getMatch().paint(player, finalPos, player.getTeam());

                    }

                }

            }

        }
        ticksToDrop--;
        if(ticksToDrop < 1) {

            ticksToDrop = 8;
            for(int i = 0; i < 2; i++) {

                float rad = radius*new Random().nextFloat();
                int yaw = new Random().nextInt(360);
                Vector offset = DirectionUtil.yawAndPitchToDirection(yaw, 0);
                Vector finalPos = position.clone().add(offset.clone().multiply(rad));

                if(finalPos.toLocation(getMatch().getWorld()).getBlock().isEmpty()) {

                    InkProjectile projectile = new InkProjectile(player, player.getEquipment().getSpecialWeapon(), player.getMatch());
                    projectile.withDamage(14d);
                    projectile.spawn(finalPos.toLocation(getMatch().getWorld()), 0, 0, 0);

                }

            }

        }

    }

    @Override
    public void reset() {



    }

}
