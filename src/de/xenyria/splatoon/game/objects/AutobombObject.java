package de.xenyria.splatoon.game.objects;

import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

public class AutobombObject extends GameObject {

    private EntityChicken chicken;

    private double velocityY;
    private ArrayList<Player> trackers = new ArrayList<>();
    private SplatoonPlayer thrower;

    public AutobombObject(Match match, Location location, SplatoonPlayer thrower) {

        super(match);

        this.thrower = thrower;

        chicken = new EntityChicken(((CraftWorld)location.getWorld()).getHandle());
        chicken.locX = location.getX();
        chicken.locY = location.getY();
        chicken.locZ = location.getZ();
        chicken.setPosition(location.getX(), location.getY(), location.getZ());
        for(SplatoonHumanPlayer player : match.getHumanPlayers()) {

            if(player.getLocation().distance(location) <= 96D) {

                trackers.add(player.getPlayer());

            }

        }
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(chicken));

        }

    }

    private int remainingTicks = 300;
    private TentaMissleTarget target = null;

    @Override
    public ObjectType getObjectType() {
        return ObjectType.DUMMY;
    }

    public void onRemove() {

        chicken = null;

    }

    @Override
    public void onTick() {

        Vector direction = new Vector(0,-0.08,0);

        if(target == null && remainingTicks > 60) {

            ArrayList<TentaMissleTarget> availableTargets = new ArrayList<>();
            for(Team team1 : getMatch().getEnemyTeams(thrower.getTeam())) {

                for(SplatoonPlayer player : getMatch().getPlayers(team1)) {

                    if(!player.isSplatted()) {

                        availableTargets.add(player);

                    }

                }

            }
            for(GameObject object : getMatch().getGameObjects()) {

                if(object instanceof TentaMissleTarget) {

                    TentaMissleTarget target = (TentaMissleTarget) object;
                    if(object instanceof Dummy || target.getTeam() != thrower.getTeam()) {

                        availableTargets.add(target);

                    }

                }

            }

            if(!availableTargets.isEmpty()) {

                TentaMissleTarget lowest = null;;
                double lowestDist = 0d;

                for(TentaMissleTarget target : availableTargets) {

                    if(target.isTargetable()) {

                        if(target.getTargetLocationProvider().getLocation().distance(chicken.getBukkitEntity().getLocation()) <= 8d) {

                            Vector begin = chicken.getBukkitEntity().getLocation().toVector().add(new Vector(0, 0.4, 0));
                            Vector end = target.getTargetLocationProvider().getLocation().toVector().add(new Vector(0,.1,0));
                            Vector dirToTarget = end.clone().subtract(begin).normalize();

                            if(lowest == null || (end.distance(begin)) < lowestDist) {

                                if (VectorUtil.isValid(dirToTarget)) {

                                    RayTraceResult result = getMatch().getWorldInformationProvider().rayTraceBlocks(begin, dirToTarget, begin.distance(end), true);
                                    if (result == null || (result.getHitPosition() != null && result.getHitPosition().distance(begin) <= (end.distance(begin)))) {

                                        lowest = target;
                                        lowestDist = end.distance(begin);

                                    }

                                }

                            }

                        }

                    }

                }

                if(lowest != null) {

                    target = lowest;

                }

            }

            remainingTicks-=4;

        } else {

            if(remainingTicks > 60) {

                Location location = target.getTargetLocationProvider().getLocation();
                if (location.distance(chicken.getBukkitEntity().getLocation()) <= 1d) {

                    remainingTicks -= 8;

                } else {

                    Vector dirToEnemy = location.toVector().subtract(chicken.getBukkitEntity().getLocation().toVector());
                    dirToEnemy.setY(0);
                    dirToEnemy.normalize();
                    if (VectorUtil.isValid(dirToEnemy)) {

                        direction.add(dirToEnemy.multiply(0.1f));
                        float[] dir = DirectionUtil.directionToYawPitch(dirToEnemy);
                        chicken.yaw = dir[0];
                        chicken.pitch = 0f;
                        chicken.aS = dir[0];

                    }

                    remainingTicks -= 1;

                }

            } else {

                remainingTicks-=2;

            }

        }

        Vector targetPos = chicken.getBukkitEntity().getLocation().toVector().add(direction);
        AxisAlignedBB bb = new AxisAlignedBB(targetPos.getX()-.2, targetPos.getY(), targetPos.getZ()+.2, targetPos.getX()+.2, targetPos.getY()+.7, targetPos.getZ()+.2);

        if(!AABBUtil.hasSpace(getMatch().getWorld(), bb)) {

            Vector wrap = AABBUtil.resolveWrap(getMatch().getWorld(), targetPos, bb);
            if(wrap != null) {

                targetPos = wrap.clone();

            }

        }

        double yBefore = chicken.locY;
        Vector offset = targetPos.clone().subtract(chicken.getBukkitEntity().getLocation().toVector());

        if(remainingTicks > 60) {

            chicken.move(EnumMoveType.SELF, offset.getX(), offset.getY(), offset.getZ());
            for (Player player : trackers) {

                if (player.isOnline()) {

                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(chicken));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(chicken, (byte) (chicken.yaw * 0.70333)));

                }

            }

        }
        double yAfterwards = chicken.locY;
        double exceptedY = (yBefore+direction.getY());

        soundTicker++;
        if(soundTicker > 35) {

            soundTicker = 0;
            getMatch().getWorld().playSound(chicken.getBukkitEntity().getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1f, 1f);

        }

        if(remainingTicks < 1) {

            BombProjectile projectile = new BombProjectile(thrower, thrower.getEquipment().getSecondaryWeapon(), getMatch(), 4f, 0, 180, false);
            projectile.spawn(0, chicken.getBukkitEntity().getLocation());

            for(Player player : trackers) {

                if(player.isOnline()) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(chicken.getId()));

                }

            }
            getMatch().queueObjectRemoval(this);

        }

        if(remainingTicks < 100) {

            int x = remainingTicks%4;
            if(x <= 1) {

                chicken.getBukkitEntity().setCustomNameVisible(true);
                chicken.getBukkitEntity().setCustomName("§8!");

            } else {

                chicken.getBukkitEntity().setCustomNameVisible(true);
                chicken.getBukkitEntity().setCustomName(thrower.getTeam().getColor().prefix() + "!");

            }

            for(Player player : trackers) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(
                        chicken.getId(), chicken.getDataWatcher(), chicken.onGround
                ));

            }

        }

    }

    private int soundTicker = 0;
    private boolean warn = false;

    @Override
    public void reset() {

    }
}
