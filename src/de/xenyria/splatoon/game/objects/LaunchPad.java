package de.xenyria.splatoon.game.objects;

import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.tutorial.TutorialMatch;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class LaunchPad extends GameObject {

    private Location location;
    private Color color;

    public LaunchPad(Match match, Location center, Color color, Vector targetPosition, Runnable runAfterJump) {

        super(match);
        this.color = color;
        this.location = center;
        this.targetPosition = targetPosition;
        this.runAfterJump = runAfterJump;

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private Vector targetPosition = new Vector();
    private Runnable runAfterJump;

    @Override
    public void onTick() {

        for(float yaw = 0f; yaw < 360; yaw+=22.5f) {

            Location center = location.clone();
            center.setPitch(0f);
            center.setYaw(yaw);
            Vector dir = center.getDirection().multiply(.7f);
            Location target = center.clone().add(dir);
            SplatoonServer.broadcastColorParticle(getMatch().getWorld(), target.getX(), target.getY(), target.getZ(), color, .7f);

        }

        for(SplatoonPlayer player : getMatch().getAllPlayers()) {

            if(!player.inSuperJump() && player.getLocation().distance(location) < .7 && player.isSquid() && player.getTeam().getColor().equals(color)) {

                if(player.getEquipment().getSpecialWeapon() == null || !player.getEquipment().getSpecialWeapon().isActive()) {

                    player.superJump(targetPosition.toLocation(getMatch().getWorld()), 27);

                    if(player instanceof SplatoonHumanPlayer) {

                        SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;

                        if (getMatch().getMatchType() == MatchType.TUTORIAL) {

                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                if(player1.isValid()) {

                                    player1.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, false, false));

                                }

                            }, 40l);
                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                if(player1.isValid()) {

                                    player1.endSuperJump();
                                    if(player1.isSquid()) {

                                        player1.leaveSquidForm();
                                        player1.lockSquidForm();

                                    }
                                    player1.getPlayer().setVelocity(new Vector(0,0,0));
                                    player1.getPlayer().setAllowFlight(true);
                                    player1.getPlayer().setFlying(true);
                                    TutorialMatch match = (TutorialMatch) player1.getMatch();
                                    match.endTutorial();
                                    player1.leaveMatch();
                                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                        player1.getPlayer().teleport(XenyriaSplatoon.getLobbyManager().getLobby().getLobbySpawn());
                                        player1.getPlayer().setAllowFlight(true);
                                        player1.getPlayer().setFlying(true);

                                    }, 10l);

                                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                        XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player1);
                                        XenyriaSplatoon.getLobbyManager().triggerIntro(player1);
                                        player1.unlockSquidForm();

                                    }, 20l);

                                    /**/

                                }

                            }, 60l);

                        }

                    }

                }

            }

        }

    }

    @Override
    public void reset() {

    }

    public Location getLocation() { return location; }
}
