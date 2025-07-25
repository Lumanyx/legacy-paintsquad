package de.xenyria.splatoon.shootingrange;

import de.xenyria.core.chat.Characters;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchControlInterface;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.match.scoreboard.ScoreboardSlotIDs;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.RemovableGameObject;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;

public class ShootingRange extends Match {

    public boolean fromWeaponShop;

    public void apply(ArenaBoundaryConfiguration configuration, Vector offset) {

        for(ArenaBoundaryConfiguration.ArenaBoundaryBlock block : configuration.getPaintableSurfaces()) {

            Vector realPos = offset.clone().add(new Vector(block.x, block.y, block.z));
            BlockFlagManager.BlockFlag flag = getBlockFlagManager().getBlock(offset, realPos.getBlockX(), realPos.getBlockY(), realPos.getBlockZ());
            flag.setPaintable(true);
            flag.setWall(block.wall);

        }

    }

    private void setupScoreboard(Player player1) {

        XenyriaSpigotPlayer player = XenyriaSpigotPlayer.resolveByUUID(player1.getUniqueId()).getSpigotVariant();
        SplatoonHumanPlayer player2 = SplatoonHumanPlayer.getPlayer(player1);

        player.getScoreboard().reset();
        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            player.getScoreboard().setBoardName(player2.getColor().prefix() + "§lSplatoon §8" + Characters.SMALL_X + " §7Waffentest");
            player.getScoreboard().setLine(7, "§0");
            player.getScoreboard().setLine(6, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Punkte");
            player.getScoreboard().setLine(5, "§0");
            player.getScoreboard().setLine(4, "§0");
            player.getScoreboard().setLine(3, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Spezialwaffe");
            player.getScoreboard().setLine(2, "§0");
            player.getScoreboard().setLine(1, "§0");

        }, 1l);

    }

    public void updateValues(Player player1) {

        XenyriaSpigotPlayer player = XenyriaSpigotPlayer.resolveByUUID(player1.getUniqueId()).getSpigotVariant();
        SplatoonHumanPlayer player2 = SplatoonHumanPlayer.getPlayer(player1);
        player.getScoreboard().setLine(ScoreboardSlotIDs.SHOOTINGRANGE_SCORE, player2.getTeam().getColor().prefix() + "§o§l" + player2.getScoreboardManager().getPointValue());

        // Spezialwaffen-Fortschritt
        int currentPoints = (int) player2.getSpecialPoints();
        float percentage = 0f;
        if(currentPoints >= player2.getRequiredSpecialPoints()) {

            percentage = 100f;

        } else {

            percentage = ((float)currentPoints / (float)player2.getRequiredSpecialPoints()) * 100f;

        }

        if(percentage == 100f) {

            if(BattleMatch.globalTicker > 5) {

                player.getScoreboard().setLine(ScoreboardSlotIDs.SHOOTINGRANGE_SPECIAL_WEAPON, "§f§o§lBereit!");

            } else {

                player.getScoreboard().setLine(ScoreboardSlotIDs.SHOOTINGRANGE_SPECIAL_WEAPON, player2.getTeam().getColor().prefix() + "§o§lBereit!");

            }

        } else {

            int percentageVal = (int) Math.floor(percentage);
            player.getScoreboard().setLine(ScoreboardSlotIDs.SHOOTINGRANGE_SPECIAL_WEAPON, player2.getTeam().getColor().prefix() + "§o§l" + percentageVal + "%");

        }

        //player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_STATS, "§2§l" + Characters.SMALL_X + " §a" + player2.getStatistic().getSplats() + " §8/ §6§l+ §e" + player2.getStatistic().getAssists() + " §8/ §4§l" + Characters.BIG_X + " §c" + player2.getStatistic().getDeaths());


    }

    public void tick() {

        if(!getHumanPlayers().isEmpty()) {

            SplatoonHumanPlayer player = getHumanPlayers().get(0);
            updateValues(player.getPlayer());

        }
        super.tick();

    }

    private Location location;

    public void reset() {

        rollbackCall();

    }

    private void rollbackCall() {

        jumpPoints.clear();

        ArrayList<GameObject> toRemove = new ArrayList<>();
        for(GameObject object : getGameObjects()) {

            object.reset();

            if(object instanceof RemovableGameObject) {

                ((RemovableGameObject)object).remove();
                toRemove.add(object);

            }

        }
        getGameObjects().removeAll(toRemove);
        for(SplatoonProjectile projectile : getProjectiles()) {

            projectile.onRemove();

        }

        clearQueues();
        rollback(true);
        lastReset = System.currentTimeMillis();

    }

    @Override
    public void removeBeacon(BeaconObject object) {

        if(jumpPoints.containsKey(object)) { jumpPoints.remove(object); }

    }
    private HashMap<BeaconObject, JumpPoint.Beacon> jumpPoints = new HashMap<>();
    public ShootingRange(World world, Location spawn) {

        super(world);
        this.location = spawn;
        enableRollback();
        setMatchController(new MatchControlInterface() {

            @Override
            public ArrayList<JumpPoint> getJumpPoints(Team team) {

                ArrayList<JumpPoint> points = new ArrayList<>();
                for(BeaconObject object : jumpPoints.keySet()) {

                    points.add(jumpPoints.get(object));

                }

                return points;
            }

            @Override
            public void playerAdded(SplatoonPlayer player) {

                player.setTeam(getRegisteredTeams().get(0));
                if(player instanceof SplatoonHumanPlayer) {

                    setupScoreboard(((SplatoonHumanPlayer)player).getPlayer());
                    ((SplatoonHumanPlayer) player).updateInventory();

                }
                player.teleport(location);
                player.setSpawnPoint(location);


            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

                rollbackCall();
                player.getEquipment().resetWeapons();

            }

            @Override
            public void objectAdded(GameObject object) {

                if(object instanceof BeaconObject) {

                    BeaconObject object1 = (BeaconObject)object;
                    JumpPoint.Beacon beacon = new JumpPoint.Beacon(getAllPlayers().get(0), object1);
                    jumpPoints.put(object1, beacon);

                }

            }

            @Override
            public void objectRemoved(GameObject object) {

            }

            @Override
            public void teamAdded(Team team) {

            }

            @Override
            public void addGUIItems(SplatoonPlayer player) {

                if(player instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;
                    player1.getPlayer().getInventory().setItem(5, StaticItems.OPEN_JUMP_MENU);

                    /*

            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 6. Slot zum zurücksetzen der Map.");
            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 7. Slot zum Wählen einer anderen Waffe.");
            player.getPlayer().sendMessage(" §8- §eNutze das Item auf dem 8. Slot zum zurückkehren zur Lobby.");
                     */
                    player1.getPlayer().getInventory().setItem(6, StaticItems.RESET_MAP);
                    player1.getPlayer().getInventory().setItem(7, StaticItems.CHANGE_WEAPON);

                    if(!fromWeaponShop) {

                        player1.getPlayer().getInventory().setItem(8, StaticItems.RETURN_TO_LOBBY);

                    } else {

                        player1.getPlayer().getInventory().setItem(8, StaticItems.RETURN_TO_WEAPONSHOP);

                    }


                }

            }

            @Override
            public void handleSplat(SplatoonPlayer player, SplatoonPlayer shooter, SplatoonProjectile projectile) {

            }

            @Override
            public void teamChanged(SplatoonPlayer splatoonHumanPlayer, Team oldTeam, Team team) {

            }
        });
    }

    @Override
    public MatchType getMatchType() {
        return MatchType.SHOOTING_RANGE;
    }

    private long lastReset;
    public long lastResetMillis() { return (System.currentTimeMillis() - lastReset); }
}
