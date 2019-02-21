package de.xenyria.splatoon.game.match;

import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;

public class DebugMatch extends Match {

    public DebugMatch() {

        super(Bukkit.getWorld("world"));
        setMatchController(new MatchControlInterface() {

            @Override
            public ArrayList<JumpPoint> getJumpPoints(Team team) {

                ArrayList<JumpPoint> points = new ArrayList<>();
                for(SplatoonPlayer player : getPlayers(team)) {

                    JumpPoint.Player plr = jumpPoints.get(player);
                    if(plr.isAvailable(team)) {

                        points.add(plr);

                    }

                }
                for(BeaconObject object : beacons.keySet()) {

                    if(beacons.get(object).isAvailable(team)) {

                        points.add(beacons.get(object));

                    }

                }

                return points;

            }

            private HashMap<SplatoonPlayer, JumpPoint.Player> jumpPoints = new HashMap<>();
            private HashMap<BeaconObject, JumpPoint.Beacon> beacons = new HashMap<>();

            @Override
            public void playerAdded(SplatoonPlayer player) {

                jumpPoints.put(player, new JumpPoint.Player(player));

            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

                jumpPoints.remove(player);

            }

            @Override
            public void objectAdded(GameObject object) {

                if(object instanceof BeaconObject) {

                    BeaconObject object1 = (BeaconObject) object;
                    beacons.put(object1, new JumpPoint.Beacon(object1.getOwner(), object1));

                }

            }

            @Override
            public void objectRemoved(GameObject object) {

                if(object instanceof BeaconObject) { beacons.remove(object); }

            }

            @Override
            public void teamAdded(Team team) {

                getOrCreateJumpMenu(team);

            }

            @Override
            public void addGUIItems(SplatoonPlayer player) {

                if(player instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer)player).getPlayer().getInventory().setItem(6, StaticItems.OPEN_JUMP_MENU);

                }

            }

            @Override
            public void handleSplat(SplatoonPlayer player, SplatoonPlayer shooter, SplatoonProjectile projectile) {

                if(shooter != null) {

                    shooter.sendMessage(" " + shooter.getTeam().getColor().prefix() + Characters.SMALL_X + " §8| §7" + player.getName() + " erledigt.");
                    for (SplatoonPlayer player1 : getAllPlayers()) {

                        if (player1 != player && player1 != shooter) {

                            player1.sendMessage(" §8" + Characters.ARROW_RIGHT_FROM_TOP + " " + shooter.getTeam().getColor().prefix() + shooter.getName() + " §7erledigte " + player.getTeam().getColor().prefix() + player.getName());

                        }

                    }

                }

            }

            @Override
            public void teamChanged(SplatoonPlayer splatoonHumanPlayer, Team oldTeam, Team team) {

            }


        });

    }

    @Override
    public MatchType getMatchType() {
        return MatchType.TURF_WAR;
    }

    @Override
    public void removeBeacon(BeaconObject object) {

    }
}
