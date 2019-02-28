package de.xenyria.splatoon.game.match;

import com.destroystokyo.paper.Title;
import com.mojang.authlib.GameProfile;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.core.timer.Timer;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.servercore.room.Room;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.servercore.spigot.room.SpigotRoom;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.arena.ArenaCategory;
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.arena.builder.ArenaBuilder;
import de.xenyria.splatoon.game.color.ColorCombination;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissles;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.match.scoreboard.ScoreboardSlotIDs;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Gusher;
import de.xenyria.splatoon.game.objects.InkRail;
import de.xenyria.splatoon.game.objects.RideRail;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.MapDamageProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.sound.MusicTrack;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.VectorUtil;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.ChunkCoordIntPair;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public abstract class BattleMatch extends Match {

    public static final String CHOOSE_MAP = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWähle eine Arena";
    public static final String PLAYER_MANAGE_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cSpielerverwaltung";
    private SpigotRoom room;

    @Override
    public void removeBeacon(BeaconObject object) {

        if(beacons.containsKey(object)) { beacons.remove(object); }

    }
    private HashMap<BeaconObject, JumpPoint.Beacon> beacons = new HashMap<>();

    public int remainingTeamSpace() {

        int totalSize = teamCount*playersPerTeam;
        int occupied = teamIDs.size();

        return totalSize-occupied;

    }
    public int remainingSpectatorSpace() {

        return MAX_SPECTATORS-spectators.size();

    }

    private HashMap<Team, JumpPoint.TeamSpawn> teamJumpPoints = new HashMap<>();

    public BattleMatch() {

        super(XenyriaSplatoon.getArenaProvider().getArenaWorld());
        room = XenyriaSpigotServerCore.getRoomManager().newRoom();

        setMatchController(new MatchControlInterface() {
            @Override
            public ArrayList<JumpPoint> getJumpPoints(Team team) {

                ArrayList<JumpPoint> points = new ArrayList<>();
                JumpPoint.TeamSpawn spawn = teamJumpPoints.getOrDefault(team, null);
                if(spawn != null) {

                    points.add(spawn);

                }

                for(SplatoonPlayer player : getPlayers(team)) {

                    JumpPoint.Player plr = jumpPoints.get(player);
                    if(plr != null && plr.isAvailable(team)) {

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

            @Override
            public void playerAdded(SplatoonPlayer player) {

                jumpPoints.put(player, new JumpPoint.Player(player));
                if(player instanceof SplatoonHumanPlayer && isLobbyPhase()) {

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;
                    humanPlayerAdded(player1);
                    player1.getXenyriaPlayer().getScoreboard().reset();

                }

                for(GameObject object : getGameObjects()) {

                    if(object instanceof BeaconObject) {

                        BeaconObject object1 = (BeaconObject) object;
                        if(object1.getOwner() == player) {

                            queueObjectRemoval(object1);

                        }

                    }

                }

                if(isLobbyPhase() && player instanceof SplatoonHumanPlayer) {

                    // Sanity check
                    jumpPoints.remove(player);
                    lobbyPlayerPool.remove(player);
                    teamIDs.remove(player);
                    spectators.remove(player);

                    lobbyPlayerPool.add((SplatoonHumanPlayer) player);

                }
                player.updateEquipment();
                updateChooseTeamInventory();

            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

                if(player instanceof SplatoonHumanPlayer) {

                    if(inIntro()) {

                        getIntroManager().handlePlayerQuit((SplatoonHumanPlayer) player);

                    } else if(inOutro()) {

                        getOutroManager().handlePlayerQuit((SplatoonHumanPlayer)player);

                    }

                    Room.RoomEntry entry = room.findEntry(player.getUUID());
                    if(entry != null) {

                        room.removeEntry(entry.getID());

                    }

                    for(EntityNPC npc : getNPCs()) {

                        npc.untrack(player);

                    }

                    if(teamIDs.containsKey(player)) {

                        int teamID = teamIDs.get(player);
                        Team team = getRegisteredTeams().get(teamID);
                        broadcast(team.getColor().prefix() + player.getName() + " §7hat das Spiel verlassen.");

                    } else {

                        broadcast("§8" + player.getName() + " §7hat das Spiel verlassen.");

                    }

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
                    if(isOwner(player1)) {

                        boolean found = false;
                        for(SplatoonHumanPlayer player2 : getHumanPlayers()) {

                            if(player2 != player1) {

                                changeOwner(player2);
                                found = true;
                                break;
                                //player2.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du bist nun der Raummeister da §e" + player1.getName() + " §7den Raum verlassen hat.");

                            }

                        }

                        if(!found) {

                            destroyMatch();

                        }

                    }

                }

                jumpPoints.remove(player);
                lobbyPlayerPool.remove(player);
                teamIDs.remove(player);
                spectators.remove(player);
                player.unlockSquidForm();
                musicIDs.remove(player);

                if(songPlayers.containsKey(player)) {

                    SongPlayer player1 = songPlayers.get(player);
                    player1.removePlayer(((SplatoonHumanPlayer)player).getPlayer());
                    songPlayers.remove(player);

                }

                if(matchTitle != null) {

                    if (player instanceof SplatoonHumanPlayer) {

                        SplatoonHumanPlayer humanPlayer = (SplatoonHumanPlayer) player;
                        if (matchTitle.getPlayers().contains(humanPlayer.getPlayer())) {

                            matchTitle.removePlayer(((SplatoonHumanPlayer) player).getPlayer());

                        }

                    }

                }

                if(playerLobbyInventories.containsKey(player)) {

                    playerLobbyInventories.remove(player);

                }
                updateChooseTeamInventory();

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

                if(!player.isSpectator()) {

                    if (!lobbyPhase) {

                        if (player instanceof SplatoonHumanPlayer) {

                            ((SplatoonHumanPlayer) player).getPlayer().getInventory().setItem(6, StaticItems.OPEN_JUMP_MENU);

                        }

                    } else {

                        if (player instanceof SplatoonHumanPlayer) {

                            SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
                            giveLobbyItems(player1.getPlayer());

                        }

                    }

                } else {

                    if(player instanceof SplatoonHumanPlayer) {

                        SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
                        player1.getPlayer().getInventory().setItem(4, StaticItems.SPECTATE);

                    }

                }

            }

            @Override
            public void handleSplat(SplatoonPlayer player, SplatoonPlayer shooter, SplatoonProjectile projectile) {

                if(shooter != null) {

                    shooter.sendMessage(" " + shooter.getTeam().getColor().prefix() + Characters.SMALL_X + " §8| §7" + player.getName() + " erledigt.");
                    for (SplatoonPlayer player1 : getAllPlayers()) {

                        if (player1 != player && player1 != shooter) {

                            player1.sendMessage(" §8" + Characters.ARROW_RIGHT_FROM_TOP + " " + shooter.getTeam().getColor().prefix() + shooter.getName() + " §7erledigte " + player.getTeam().getColor().prefix() + player.getName() + " §7mit §e" + projectile.getWeapon().getName());

                        }

                    }

                }

            }

            @Override
            public void teamChanged(SplatoonPlayer splatoonHumanPlayer, Team oldTeam, Team team) {

                teamIDs.remove(splatoonHumanPlayer);
                for(SplatoonHumanPlayer player : getHumanPlayers()) {

                    Scoreboard scoreboard = player.getPlayer().getScoreboard();
                    if(oldTeam != null) {

                        org.bukkit.scoreboard.Team team1 = scoreboard.getTeam("match-" + oldTeam.getColor().name());
                        if(team1 != null) {

                            team1.removeEntry(splatoonHumanPlayer.getUUID().toString());

                        }

                    }
                    if(team != null) {

                        org.bukkit.scoreboard.Team team1 = scoreboard.getTeam("match-" + team.getColor().name());
                        if(team1 != null) {

                            team1.addEntry(splatoonHumanPlayer.getUUID().toString());

                        }

                    }

                }

                if(team != null) {

                    teamIDs.put(splatoonHumanPlayer, getRegisteredTeams().indexOf(team));

                }

            }
        });

        switchMap(XenyriaSplatoon.getArenaRegistry().getArenaData(1));
        roomID = nextRoomID();

    }

    public void destroyMatch() {

        ArrayList<SplatoonPlayer> players = (ArrayList<SplatoonPlayer>) getAllPlayers().clone();
        for(SplatoonPlayer player : players) {

            removePlayer(player);

        }
        if(!getForceLoadedChunks().isEmpty()) {

            for(ChunkCoordIntPair pair : getForceLoadedChunks()) {

                ArenaBuilder.unloadChunk(pair);

            }

        }
        XenyriaSplatoon.getMatchManager().removeRoom(this);

    }

    private static int gRoomID = 1;
    public static int nextRoomID() { gRoomID++; return gRoomID-1;}

    public static final String ROOM_PREFIX = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Raum ";

    public void humanPlayerAdded(SplatoonHumanPlayer player) {

        if(inLobbyPhase()) {

            lobbyPlayerPool.add(player);
            player.getXenyriaPlayer().switchRoom(room);

            Inventory inventory = Bukkit.createInventory(null, 54, ROOM_PREFIX + "#" + getRoomID());
            for (int i = 0; i < 54; i++) {
                inventory.setItem(i, ItemBuilder.getUnclickablePane());
            }

            playerLobbyInventories.put(player, inventory);
            updatePlayerLobbyInventories();
            player.getPlayer().openInventory(playerLobbyInventories.get(player));

        } else {

            player.getPlayer().teleport(spectatorSpawnLocation);

        }

    }

    public boolean isLobbyPhase() { return lobbyPhase; }

    public void chooseTeam(SplatoonPlayer player, int teamID) {

        teamIDs.remove(player);
        spectators.remove(player);
        lobbyPlayerPool.remove(player);

        if(teamID == -1) {

            spectators.add((SplatoonHumanPlayer) player);

        } else if(teamID == -2) {

            lobbyPlayerPool.add((SplatoonHumanPlayer) player);

        } else {

            teamIDs.put(player, teamID);

        }

    }

    public void updateChooseTeamInventory() {

        int i = 0;
        for(Team team : getRegisteredTeams()) {

            ItemBuilder builder = new ItemBuilder(team.getColor().getWool()).setDisplayName(team.getColor().prefix() + "Team " + team.getColor().getName());
            int playerCount = 0;
            ArrayList<String> teamList = new ArrayList<>();

            int iterated = 0;
            for(Map.Entry<SplatoonPlayer, Integer> entry : teamIDs.entrySet()) {

                iterated++;
                teamList.add("§8- §6" + entry.getKey().getName());

            }

            for(AIPlayer player : aiPlayers) {

                iterated++;
                teamList.add("§8- §7[CPU] §6" + player.getName());

            }
            for(int y = iterated; y < playersPerTeam; y++) {

                teamList.add("§8- §7Leer");

            }

            builder.addLore("§e" + playerCount + " §8/ §e" + playersPerTeam + " Spieler");
            builder.addLore("");
            builder.addLore(teamList.toArray(new String[]{}));
            builder.addLore("");
            if((playerCount+1) <= playersPerTeam) {

                builder.addLore("§aKlicke, zum beitreten");

            } else {

                builder.addLore("§cDieses Team ist voll");

            }
            builder.setAmount((playerCount < 1) ? 1 : playerCount);
            builder.addToNBT("JoinTeam", i);
            builder.addToNBT("RoomID", roomID);
            chooseTeamInventory.setItem(i, builder.create());

            i++;

        }
        ItemBuilder spectator = new ItemBuilder(Material.ENDER_EYE);
        spectator.setDisplayName("§8Zuschauer");
        int spectators = getSpectators().size();
        spectator.addLore("§e" + spectators + " §8/ §e" + MAX_SPECTATORS + " Spieler");
        spectator.setAmount((spectators < 1) ? 1 : spectators);
        spectator.addLore("");

        int iter = 0;
        for(SplatoonHumanPlayer player : getSpectators()) {

            spectator.addLore("§8- §7" + player.getName());
            iter++;

        }
        for(int y = iter; y < MAX_SPECTATORS; y++) {

            spectator.addLore("§8- §7Leer");

        }

        spectator.addLore("");
        spectator.addToNBT("JoinTeam", -1);
        spectator.addToNBT("RoomID", roomID);

        if((spectators+1) <= MAX_SPECTATORS) {

            spectator.addLore("§aKlicke, zum beitreten");

        } else {

            spectator.addLore("§cDerzeit ist kein Zuschauerplatz frei");

        }
        chooseTeamInventory.setItem(6, spectator.create());

    }

    // Dient für folgendes:
    // Lobbyphase: false -> Abrufen der Spielerzahlen anhand der aiPlayers-Liste
    // Spielphase: true -> Abrufen der Spielerzahlen anhand der Spielerliste

    private ArenaProvider.ArenaGenerationTask task;
    public void start() {

        chooseTeamInventory.clear();
        for(int i = 0; i < 9; i++) {

            chooseTeamInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        chooseTeamInventory.setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§cNicht beitreten").addToNBT("BackToPrivateLobbies", true).create());

        broadcastDebugMessage("Farbkombination wird generiert...");
        colorCombination = ColorCombination.getColorCombinations(teamCount);

        for(int i = 0; i < teamCount; i++) {

            Team team = new Team(i, colorCombination.color(i));
            registerTeam(team);

            for(Map.Entry<SplatoonPlayer, Integer> entry : teamIDs.entrySet()) {

                SplatoonPlayer player = entry.getKey();
                player.setTeam(getRegisteredTeams().get(entry.getValue()));

            }

        }

        //        chooseTeamInventory.setItem(6, new ItemBuilder(Material.ENDER_EYE).setDisplayName("§8Zuschauer").addToNBT("EnterTeam", -1).create());

        matchStarted = true;
        task = XenyriaSplatoon.getArenaProvider().requestArena(selectedMapID, this);
        mapSchematicName = XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID).getMap().get(getMatchType());
        offset = task.getOffset();

        for(StoredPlaceholder placeholder : task.getSchematic().getStoredPlaceholders()) {

            if(placeholder.type == SchematicPlaceholder.Splatoon.SPECTATOR_SPAWN) {

                spectatorSpawnLocation = new Location(getWorld(), placeholder.x + .5, placeholder.y, placeholder.z + .5);
                spectatorSpawnLocation = spectatorSpawnLocation.add(offset);
                if (placeholder.getData().containsKey("yaw")) {

                    spectatorSpawnLocation.setYaw(Float.parseFloat(placeholder.getData().get("yaw")));

                }

            }

        }

        broadcastDebugMessage("§eArena #" + selectedMapID + " §7wird angefragt...");
        updateChooseTeamInventory();

    }
    public boolean inLobbyPhase() { return lobbyPhase; }

    public void reset() {

        super.reset();
        teamJumpPoints.clear();
        outroPhase = false;
        introFlag = false;
        ticksToOutroBegin = CONST_TICKS_TO_OUTRO_BEGIN;
        offset = null;
        matchStarted = false;
        matchStartPhase = false;
        introFlag = false;
        countdownPhase = false;

        waitLoadPhase = false;
        matchPreBeginTitle = false;
        gamePhase = false;

        ArrayList<EntityNPC> npcs = getNPCs();
        for(EntityNPC npc : npcs) {

            npc.remove();

        }

        teamIDs.clear();
        spectators.clear();
        lobbyPlayerPool.addAll(getHumanPlayers());

        lobbyPhase = true;
        countdownTicker = 0;
        countdownPhase = false;

        for(Map.Entry<SplatoonHumanPlayer, SongPlayer> entry : songPlayers.entrySet()) {

            entry.getValue().removePlayer(entry.getKey().getPlayer());

        }
        songPlayers.clear();

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.updateInventory();

            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                player.teleport(SplatoonLobby.getLobbySpawn());

                Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                    player.getPlayer().setFlying(false);
                    player.getPlayer().setAllowFlight(false);
                    player.getPlayer().setWalkSpeed(.2f);
                    player.getPlayer().setGameMode(GameMode.ADVENTURE);
                    player.getPlayer().setFlySpeed(0.1f);

                    openLobbyInventory(player);

                }, 10l);

            }, 10l);

        }

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            for(ChunkCoordIntPair pair : getForceLoadedChunks()) {

                ArenaBuilder.unloadChunk(pair);

            }
            getForceLoadedChunks().clear();


        }, 10l);
        updateChooseTeamInventory();
        updatePlayerLobbyInventories();

    }

    private Vector offset;
    public Vector getOffset() { return offset; }

    private ColorCombination colorCombination;
    private Location tempPos = null;

    private boolean lobbyPhase = true;
    private boolean skipIntro = true;
    private boolean matchStarted = false;

    private int countdownTicker = 0;

    public void tick() {

        if(lobbyPhase) {

            if(countdownPhase) {

                countdownTicker++;
                if (countdownTicker > 20) {

                    countdownTicker = 0;
                    startCountdown--;
                    for(SplatoonHumanPlayer player : getHumanPlayers()) {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "§e" + startCountdown + "...");
                        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                    }

                }

                if(startCountdown < 1) {

                    countdownPhase = false;
                    start();

                }
                return;

            }

            Iterator<Map.Entry<SplatoonHumanPlayer, Integer>> iterator = remainingUpdateTicks.entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<SplatoonHumanPlayer, Integer> entry = iterator.next();
                int newVal = entry.getValue();
                newVal--;

                if(newVal < 1 || !entry.getKey().isValid() || entry.getKey().getMatch() != this) {

                    iterator.remove();
                    if(entry.getKey().isValid() && entry.getKey().getMatch() == this) {

                        updateInventory(entry.getKey());

                    }

                } else {

                    entry.setValue(newVal);

                }

            }

            if(matchStarted) {

                if (task != null) {

                    if (task.isDone()) {

                        for(SplatoonHumanPlayer player : getHumanPlayers()) {

                            if(!player.isSpectator()) {

                                if (player.isSquid()) {

                                    player.leaveSquidForm();

                                }
                                player.lockSquidForm();

                            }

                        }

                        broadcastDebugMessage("Arena-Generation abgeschlossen - Erfolgreich? §e" + task.isSuccessful());
                        if (task.isSuccessful()) {

                            broadcastDebugMessage("Match-Initialisierungssequenz startet.");

                            broadcastDebugMessage("Wende .sbounds-Datei an.");
                            try {

                                ArenaBoundaryConfiguration configuration = ArenaBoundaryConfiguration.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + task.getArenaData().getMap().get(getMatchType()) + ".sbounds"));
                                System.out.println(task.getArenaData().getMap().get(getMatchType()) + ".sbounds");
                                broadcastDebugMessage("Färbflächen festlegen");
                                apply(configuration, task.getOffset());
                                int teamID = 0;
                                broadcastDebugMessage("Spawns werden eingefügt");
                                for (de.xenyria.splatoon.game.map.Map.TeamSpawn spawn : PlaceholderReader.getSpawns(task.getOffset(), task.getSchematic())) {

                                    if (teamID <= (getRegisteredTeams().size() - 1)) {

                                        Team team = getRegisteredTeams().get(teamID);
                                        getMap().getSpawns().add(spawn);
                                        getMap().pasteSpawn(getWorld(), spawn, getRegisteredTeams().get(teamID));
                                        JumpPoint.TeamSpawn point = new JumpPoint.TeamSpawn(team, team.getColor(), spawn.getPosition());
                                        teamJumpPoints.put(team, point);

                                        teamID++;


                                    }

                                }
                                HashMap<Integer, ArrayList<Vector>> inkRailJoints = new HashMap<>(), rideRailJoints = new HashMap<>();
                                for(StoredPlaceholder placeholder : task.getSchematic().getStoredPlaceholders()) {

                                    if(placeholder.type == SchematicPlaceholder.Splatoon.GUSHER) {

                                        double impulse = Double.parseDouble(placeholder.getData().get("impulse"));
                                        Gusher gusher = new Gusher(this, getWorld().getBlockAt(
                                                placeholder.x+(int)offset.getX(),
                                                placeholder.y+(int)offset.getY(),
                                                placeholder.z+(int)offset.getZ()
                                        ), BlockFace.UP, impulse);
                                        addGameObject(gusher);

                                    } else if(placeholder.type == SchematicPlaceholder.Splatoon.RIDE_RAIL_JOINT) {

                                        Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
                                        vec = vec.add(offset);

                                        int railID = Integer.parseInt(placeholder.getData().get("railid"));
                                        if(!rideRailJoints.containsKey(railID)) {

                                            ArrayList<Vector> vectors = new ArrayList<>();
                                            vectors.add(vec);
                                            rideRailJoints.put(railID, vectors);

                                        } else {

                                            rideRailJoints.get(railID).add(vec);

                                        }

                                    } else if(placeholder.type == SchematicPlaceholder.Splatoon.INK_RAIL_JOINT) {

                                        Vector vec = new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5);
                                        vec = vec.add(offset);

                                        int railID = Integer.parseInt(placeholder.getData().get("railid"));
                                        if(!inkRailJoints.containsKey(railID)) {

                                            ArrayList<Vector> vectors = new ArrayList<>();
                                            vectors.add(vec);
                                            inkRailJoints.put(railID, vectors);

                                        } else {

                                            inkRailJoints.get(railID).add(vec);

                                        }

                                    }

                                }
                                for(ArrayList<Vector> inkRailVectors : inkRailJoints.values()) {

                                    InkRail rail = new InkRail(this, inkRailVectors.toArray(new Vector[]{}));
                                    try {

                                        rail.interpolateVectors();
                                        addGameObject(rail);

                                    } catch (Exception e) {

                                        e.printStackTrace();

                                    }

                                }
                                for(ArrayList<Vector> rideRailVectors : rideRailJoints.values()) {

                                    RideRail rail = new RideRail(this, rideRailVectors.toArray(new Vector[]{}));
                                    try {

                                        rail.interpolateVectors();
                                        addGameObject(rail);

                                    } catch (Exception e) {

                                        e.printStackTrace();

                                    }

                                }

                                broadcastDebugMessage("Kamerafahrt wird erstellt");
                                for (StoredPlaceholder placeholder : task.getSchematic().getStoredPlaceholders()) {

                                    if (placeholder.type == SchematicPlaceholder.Splatoon.CAMERA_FOCUS) {

                                        getMap().getIntroductionCamera().setFocus(task.getOffset().clone().add(new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5)));

                                    } else if (placeholder.type == SchematicPlaceholder.Splatoon.CAMERA_BEGIN) {

                                        getMap().getIntroductionCamera().setStart(task.getOffset().clone().add(new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5)));

                                    } else if (placeholder.type == SchematicPlaceholder.Splatoon.CAMERA_END) {

                                        getMap().getIntroductionCamera().setEnd(task.getOffset().clone().add(new Vector(placeholder.x + .5, placeholder.y + .5, placeholder.z + .5)));

                                    }

                                }

                                tempPos = getMap().getIntroductionCamera().getStart().toLocation(XenyriaSplatoon.getArenaProvider().getArenaWorld());

                                task = null;
                                lobbyPhase = false;

                                for (Team team : getRegisteredTeams()) {

                                    int id = 0;
                                    for (SplatoonPlayer player : getPlayers(team)) {

                                        player.setSpawnPoint(getNextSpawnPoint(team, id));

                                        id++;
                                        if (id >= 3) {

                                            id = 3;

                                        }

                                    }

                                }

                                for (SplatoonPlayer player : getAllPlayers()) {

                                    if (!player.isSpectator() && player instanceof SplatoonHumanPlayer) {

                                        WeaponSetItem set = ((SplatoonHumanPlayer)player).getInventory().getEquippedSet();
                                        WeaponSet set1 = set.getSet();

                                        player.getEquipment().setPrimaryWeapon(set1.getPrimaryWeapon());
                                        player.getEquipment().setSecondaryWeapon(set1.getSecondary());
                                        player.getEquipment().setSpecialWeapon(set1.getSpecial());
                                        setUsedWeaponSet(player, set1);

                                    }

                                }


                                if (!aiPlayers.isEmpty()) {

                                    broadcastDebugMessage("KI-Gegner werden initialisiert");
                                    initializeAIManager();
                                    for (AIPlayer entry : aiPlayers) {

                                        Team team = getRegisteredTeams().get(entry.getTeam());

                                        Location location = getNextSpawnPoint(team);
                                        EntityNPC npc = new EntityNPC(entry.name, location, team, this);
                                        npc.setProperties(entry.difficulty.getProperties());
                                        npc.setSpawnPoint(location);
                                        npc.setVisibleInTab(true);
                                        addPlayer(npc);
                                        npc.disableTracker();
                                        npc.disableAI();

                                        ArrayList<WeaponSet> sets = WeaponSetRegistry.getSets(entry.weaponType.toPrimaryWeaponType());
                                        int rndmIndx = 0;
                                        WeaponSet set = null;
                                        if(sets.size() > 1) {

                                            rndmIndx = new Random().nextInt(sets.size()-1);
                                            set = sets.get(rndmIndx);

                                        } else if(sets.isEmpty()) {

                                            set = WeaponSetRegistry.getSet(1);

                                        }

                                        npc.getEquipment().setPrimaryWeapon(set.getPrimaryWeapon());
                                        npc.getEquipment().setSecondaryWeapon(set.getSecondary());
                                        npc.getEquipment().setSpecialWeapon(TentaMissles.ID);
                                        //npc.getEquipment().setSpecialWeapon(set.getSpecial());
                                        //npc.getEquipment().setSpecialWeapon(37);
                                        setUsedWeaponSet(npc, set);

                                        broadcastDebugMessage("KI-Gegner (EID: " + npc.getEntityID() + ") zum Match hinzugefügt.");

                                    }

                                }


                                waitLoadPhase = true;
                                tempPos = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                                        getMap().getIntroductionCamera().getStart().getX(), 16, getMap().getIntroductionCamera().getStart().getZ());

                                for (SplatoonHumanPlayer player : getHumanPlayers()) {

                                    player.getPlayer().teleport(tempPos);
                                    player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 65, 2, false, false, false));
                                    player.getPlayer().setGameMode(GameMode.SPECTATOR);
                                    player.getPlayer().setFlySpeed(0f);
                                    player.getPlayer().setAllowFlight(true);

                                }

                                if (!skipIntro) {

                                    initIntroManager();

                                }

                            } catch (Exception e) {

                                e.printStackTrace();
                                broadcastDebugMessage("Match-Init nicht erfolgreich.");
                                task = null;
                                lobbyPhase = true;

                            }

                        } else {

                            task = null;
                            lobbyPhase = true;

                        }

                    }

                }

            }

        } else {

            if(waitLoadPhase) {

                waitLoadTicks--;
                if(waitLoadTicks < 1 || skipIntro) {

                    waitLoadPhase = false;
                    introFlag = true;
                    if(!skipIntro) {

                        getIntroManager().initializeSequence();

                    }

                }

            } else if(introFlag) {

                if(skipIntro || (getIntroManager() != null && getIntroManager().isFinished())) {

                    matchStartPhase = true;
                    introFlag = false;
                    aiStartFlag = true;

                    for(SplatoonPlayer player : getAllPlayers()) {

                        if(player instanceof EntityNPC) {

                            ((EntityNPC)player).teleport(player.getSpawnPoint());
                            ((EntityNPC) player).enableTracker();

                        }

                    }

                    for(SplatoonHumanPlayer player : getHumanPlayers()) {

                        if(!player.isSpectator()) {

                            Player player1 = player.getPlayer();
                            player1.setGameMode(GameMode.ADVENTURE);
                            player1.setAllowFlight(false);
                            player.teleport(player.getSpawnPoint());
                            setupScoreboard(player1);

                        } else {

                            handleSpectatorJoin(player.getPlayer());

                        }

                    }

                } else {

                    if(!skipIntro) {

                        getIntroManager().tick();

                    }

                }

            } else if(matchStartPhase) {

                for(SplatoonHumanPlayer player : getHumanPlayers()) {

                    if(!player.isSpectator()) {

                        Player player1 = player.getPlayer();
                        if(player1.getLocation().distance(player.getSpawnPoint()) >= 0.05) {

                            player.teleport(player.getSpawnPoint());

                        }

                    }

                }

                matchStartTicks--;
                if(matchStartTicks < 20) {

                    if(!matchPreBeginTitle) {

                        for(SplatoonHumanPlayer player : getHumanPlayers()) {

                            if(!player.isSpectator()) {

                                Player player1 = player.getPlayer();
                                player1.sendTitle(new Title("", "§7Bereit?", 5, 10, 5));

                            }

                        }
                        matchPreBeginTitle = true;

                    }

                }

                if(matchStartTicks < 1) {

                    for(SplatoonHumanPlayer player : getHumanPlayers()) {

                        if(!player.isSpectator()) {

                            Player player1 = player.getPlayer();
                            player1.sendTitle(new Title(player.getTeam().getColor().prefix() + "Los!", "", 5, 15, 5));
                            player1.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.7f);

                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                player1.sendTitle(new Title("", "§7§o" + getMatchType().getMatchBeginText(), 5, 50, 5));

                            }, 25l);

                        }

                    }
                    for(SplatoonPlayer player : getAllPlayers()) {

                        if(!player.isHuman()) {

                            ((EntityNPC)player).enableAI();

                        }
                        player.unlockSquidForm();

                    }
                    matchStartPhase = false;

                    // DBG
                    XenyriaSplatoon.getXenyriaLogger().log("§eMatch #" + getRoomID() + " §7beginnt - Modus: §e" + getMatchType());
                    remainingGameTicks = getMatchType()==MatchType.TURF_WAR ? 180*20 : 300*20;

                    matchLengthTicks = remainingGameTicks;

                    // Musik
                    for(Map.Entry<SplatoonPlayer, Integer> songEntry : musicIDs.entrySet()) {

                        if(songEntry.getValue() != -1) {

                            int id = songEntry.getValue();
                            MusicTrack[] tracks = XenyriaSplatoon.getMusicManager().getTrackList(id);

                            SplatoonHumanPlayer player = (SplatoonHumanPlayer) songEntry.getKey();
                            player.getPlayer().sendMessage(" §e§l♪ §6§o§l" + tracks[0].getName());

                            Playlist playlist = null;
                            if(getMatchType() == MatchType.TURF_WAR) {

                                playlist = new Playlist(tracks[0].getSong(), tracks[1].getSong());

                            } else {

                                playlist = new Playlist(tracks[0].getSong());

                            }

                            SongPlayer player1 = new RadioSongPlayer(playlist);
                            player1.setRepeatMode(RepeatMode.ONE);
                            player1.setVolume((byte)100);
                            player1.setPlaying(true);
                            player1.addPlayer(player.getPlayer());
                            songPlayers.put(player, player1);


                        }

                    }

                    gamePhase = true;

                    for(SplatoonHumanPlayer player : getHumanPlayers()) {

                        matchTitle.addPlayer(player.getPlayer());

                    }

                }

            } else if(gamePhase) {

                for(SplatoonHumanPlayer player : getHumanPlayers()) {

                    if(!player.isSpectator()) {

                        updateValues(player.getPlayer());

                    }

                }

                if(remainingGameTicks > 0) {

                    tickSpawnShields();

                    remainingGameTicks--;
                    matchSecondTicker++;
                    timer.setSeconds((matchLengthTicks / 20) - (remainingGameTicks / 20));

                    if (matchSecondTicker >= 20) {

                        matchSecondTicker = 0;
                        updateBossBar();

                        int remainingSeconds = remainingGameTicks / 20;
                        if (remainingSeconds <= 10 && remainingSeconds >= 1) {

                            for (SplatoonHumanPlayer player : getHumanPlayers()) {

                                String prefix = "§7";
                                if(player.getTeam() != null) {

                                    prefix = player.getTeam().getColor().prefix();

                                }

                                player.getPlayer().sendTitle(new Title(prefix + "§o§l" + remainingSeconds));

                            }

                        }

                    }

                    if (remainingGameTicks < ((20 * 60) + 20)) {

                        if (!lastMinute) {

                            lastMinute = true;
                            for (SplatoonHumanPlayer player : getHumanPlayers()) {

                                //player.sendMessage(Chat.SYSTEM_PREFIX + "Noch §eeine Minute§7!");
                                player.getPlayer().sendTitle(new Title("", "§7Noch §eeine Minute §7verbleibt!", 5, 30, 5));
                                player.getPlayer().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 2f);
                                Bukkit.getConsoleSender().sendMessage("§cLast Minute called");

                            }

                            if (getMatchType() == MatchType.TURF_WAR) {

                                for (Map.Entry<SplatoonHumanPlayer, SongPlayer> entry : songPlayers.entrySet()) {

                                    RadioSongPlayer player1 = (RadioSongPlayer) entry.getValue();
                                    Playlist playlist = player1.getPlaylist();

                                    player1.setTick(playlist.get(0).getLength());
                                    player1.setRepeatMode(RepeatMode.NO);
                                    player1.setPlaylist(new Playlist(playlist.get(1)));
                                    player1.playSong(0);
                                    player1.setTick((short) 0);
                                    player1.setPlaying(true);

                                    MusicTrack track = XenyriaSplatoon.getMusicManager().getTrackList(musicIDs.get(entry.getKey()))[1];

                                    entry.getKey().sendMessage(" §e§l♪ §6§o§l" + track.getName());


                                }

                            }

                        }

                    }

                } else {

                    ticksToOutroBegin = 30;
                    outroPhase = true;
                    gamePhase = false;

                    for(SplatoonPlayer player : getAllPlayers()) {

                        if(!player.isSpectator()) {

                            if(player instanceof EntityNPC) {

                                EntityNPC npc = (EntityNPC)player;
                                npc.disableAI();
                                aiStartFlag = false;

                            } else {

                                SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;
                                player1.getPlayer().sendTitle(new Title("§7Ende!", "§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█§6█§0█", 2, 40, 10));
                                if(player1.isSquid()) {

                                    player1.leaveSquidForm();
                                    player1.lockSquidForm();

                                }
                                //player1.getEquipment().resetPrimaryWeapon();
                                //player1.getEquipment().resetSecondaryWeapon();
                                //player1.getEquipment().resetSpecialWeapon();
                                player1.getPlayer().getInventory().clear();
                                player1.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 2));

                            }

                        }

                    }

                }

                super.tick();

            } else if(outroPhase) {

                if(ticksToOutroBegin > 0) {

                    ticksToOutroBegin--;
                    if(ticksToOutroBegin < 1) {

                        for(SplatoonPlayer player : getAllPlayers()) {

                            if(!player.isSpectator()) {

                                try { player.getEquipment().getPrimaryWeapon().reset(); } catch (Exception e) { e.printStackTrace(); }
                                try { player.getEquipment().getSecondaryWeapon().reset(); } catch (Exception e) { e.printStackTrace(); }
                                try { player.getEquipment().getSpecialWeapon().reset(); } catch (Exception e) { e.printStackTrace(); }

                            } else {

                                if(player instanceof SplatoonHumanPlayer) {

                                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
                                    player1.leaveSpectatorMode();

                                }

                            }

                        }
                        clearAllObjects();

                        for(SplatoonHumanPlayer player : getHumanPlayers()) {

                            matchTitle.removePlayer(player.getPlayer());

                        }
                        matchTitle = null;

                        outroDelay = 5;
                        initOutroManager();
                        XenyriaSplatoon.getXenyriaLogger().log("§eMatch #" + getRoomID() + " §7endet.");

                    }

                } else {

                    if(outroDelay > 0) {

                        outroDelay--;
                        return;

                    }

                    getOutroManager().tick();

                }

            }

            // else {

            //    super.tick();

            //}

        }

    }

    private void handleSpectatorJoin(Player player) {

        player.setGameMode(GameMode.SURVIVAL);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 5, false, false, false));
        player.getInventory().clear();
        getMatchController().addGUIItems(SplatoonHumanPlayer.getPlayer(player));
        player.teleport(spectatorSpawnLocation.clone().add(offset));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(.2f);
        player.setWalkSpeed(.3f);

    }

    public Location spectatorSpawnLocation;

    private int outroDelay = 0;
    private boolean outroPhase = false;

    public static final int CONST_TICKS_TO_OUTRO_BEGIN = 30;

    private int ticksToOutroBegin = CONST_TICKS_TO_OUTRO_BEGIN;
    private int matchSecondTicker = 0;
    private Timer timer = new Timer(Timer.TimerElement.MINUTE, Timer.TimerElement.SECONDS);

    public void updateBossBar() {

        if(getTeamCount() == 2) {

            Team team1 = getRegisteredTeams().get(0);
            Team team2 = getRegisteredTeams().get(1);

            String teamStr1 = bossBarString(team1);
            String teamStr2 = bossBarString(team2);
            String timerStr = timer.toString();

            matchTitle.setTitle(teamStr1 + " §8§l( " + (lastMinute?"§e":"§f") + timerStr + " §8§l) " + teamStr2);

        }

    }

    private boolean bossBarAlternate = false;
    public String bossBarString(Team team) {

        String str = "";
        ArrayList<SplatoonPlayer> players = getPlayers(team);
        for(int i = 0; i < playersPerTeam; i++) {

            if(i <= (players.size()-1)) {

                SplatoonPlayer player = players.get(i);
                if(!player.isSplatted()) {

                    if(player.isSpecialReady()) {

                        bossBarAlternate=!bossBarAlternate;
                        if(bossBarAlternate) {

                            str += "§f❤ ";

                        } else {

                            str += team.getColor().prefix() + "❤ ";

                        }

                    } else {

                        str += team.getColor().prefix() + "❤ ";

                    }

                } else {

                    str+="§7" + Characters.BIG_X + " ";

                }

            } else {

                str+="§8" + Characters.SMALL_X + " ";

            }

        }
        return str.substring(0,str.length()-1);

    }
    private BossBar matchTitle = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);

    private boolean lastMinute = false;
    private int remainingGameTicks = 0;
    private int matchLengthTicks = 0;

    public Vector centeredTeamSpawnVector(Team team1) {

        int indx = getRegisteredTeams().indexOf(team1);
        de.xenyria.splatoon.game.map.Map.TeamSpawn spawn = getMap().getSpawns().get(indx);
        Location center = spawn.getPosition().clone().add(2, 1, 2);
        return center.toVector();

    }

    public void tickSpawnShields() {

        for(de.xenyria.splatoon.game.map.Map.TeamSpawn spawn : getMap().getSpawns()) {

            Team team = getRegisteredTeams().get(spawn.getTeamID());
            Location center = centeredTeamSpawnVector(getRegisteredTeams().get(spawn.getTeamID())).toLocation(getWorld());
            double spawnProtectionRadius = 5d;

            for(SplatoonPlayer player : getAllPlayers()) {

                if(!player.isSpectator() && !player.isSplatted()) {

                    if (!player.getTeam().equals(team)) {

                        if(player.getLocation().distance(center) <= spawnProtectionRadius) {

                            Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
                            if (VectorUtil.isValid(direction)) {

                                player.setVelocity(direction.multiply(.25));
                                player.onProjectileHit(new MapDamageProjectile(team.getColor(), DamageReason.SPAWN_BARRIER, player.getLocation(), this, 15d));
                                spawnBarrierParticles(center.toVector(), player.getLocation().toVector());

                            }

                        }

                    }

                }

            }

            for(SplatoonProjectile projectile : getProjectiles()) {

                if(projectile.getLocation() != null && projectile.getShooter() != null) {

                    if(!projectile.getShooter().getTeam().equals(team)) {

                        if(projectile.getLocation().toVector().distance(center.toVector()) <= spawnProtectionRadius) {

                            spawnBarrierParticles(center.toVector(), projectile.getLocation().toVector());
                            projectile.remove();

                        }

                    }

                }

            }

        }

    }

    public boolean inProgress() {

        return matchStarted;

    }
    public boolean lobbyPhase() {

        return !matchStarted;

    }

    public void spawnBarrierParticles(Vector center, Vector target) {

        Vector direction = target.clone().subtract(center.clone()).normalize();
        if(VectorUtil.isValid(direction)) {

            Location dummy = new Location(getWorld(),0,0,0);
            dummy.setDirection(direction);

            for(float offsetYaw = -10f; offsetYaw <= 10f; offsetYaw+=5f) {

                for(float offsetPitch = -10f; offsetPitch <= 10f; offsetPitch +=5f) {

                    Location clone = dummy.clone();
                    clone.setYaw(clone.getYaw() + offsetYaw);
                    clone.setPitch(clone.getPitch() + offsetPitch);

                    Vector direction1 = clone.getDirection();
                    Vector particleLoc = center.clone().add(direction1.clone().multiply(center.distance(target)));
                    getWorld().spawnParticle(Particle.END_ROD, particleLoc.toLocation(getWorld()), 0);

                }

            }

        }

    }

    private void setupScoreboard(Player player1) {

        XenyriaSpigotPlayer player = XenyriaSpigotPlayer.resolveByUUID(player1.getUniqueId()).getSpigotVariant();
        player.getScoreboard().reset();
        SplatoonHumanPlayer player2 = SplatoonHumanPlayer.getPlayer(player1);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            player.getScoreboard().setBoardName(player2.getColor().prefix() + "§lSplatoon §8" + Characters.SMALL_X + " §7Revierkampf");
            player.getScoreboard().setLine(10, "§0");
            player.getScoreboard().setLine(9, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Punkte");
            player.getScoreboard().setLine(8, "§0");
            player.getScoreboard().setLine(7, "§0");
            player.getScoreboard().setLine(6, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Spezialwaffe");
            player.getScoreboard().setLine(5, "§0");
            player.getScoreboard().setLine(4, "§0");
            player.getScoreboard().setLine(3, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Statistik");
            player.getScoreboard().setLine(2, "§0");
            player.getScoreboard().setLine(1, "§0");

        }, 2l);

    }

    public static int globalTicker = 0;

    private void updateValues(Player player1) {

        XenyriaSpigotPlayer player = XenyriaSpigotPlayer.resolveByUUID(player1.getUniqueId()).getSpigotVariant();
        SplatoonHumanPlayer player2 = SplatoonHumanPlayer.getPlayer(player1);
        player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_SCORE, player2.getTeam().getColor().prefix() + "§o§l" + player2.getScoreboardManager().getPointValue());

        // Spezialwaffen-Fortschritt
        int currentPoints = (int) player2.getSpecialPoints();
        float percentage = 0f;
        if(currentPoints >= player2.getRequiredSpecialPoints()) {

            percentage = 100f;

        } else {

            percentage = ((float)currentPoints / (float)player2.getRequiredSpecialPoints()) * 100f;

        }

        if(percentage == 100f) {

            if(globalTicker > 5) {

                player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_SPECIAL_WEAPON, "§f§o§lBereit!");

            } else {

                player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_SPECIAL_WEAPON, player2.getTeam().getColor().prefix() + "§o§lBereit!");

            }

        } else {

            int percentageVal = (int) Math.floor(percentage);
            player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_SPECIAL_WEAPON, player2.getTeam().getColor().prefix() + "§o§l" + percentageVal + "%");

        }

        player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_STATS, "§2§l" + Characters.SMALL_X + " §a" + player2.getStatistic().getSplats() + " §8/ §6§l+ §e" + player2.getStatistic().getAssists() + " §8/ §4§l" + Characters.BIG_X + " §c" + player2.getStatistic().getDeaths());

    }

    private int waitLoadTicks = CONST_MATCH_WAIT_LOAD_TICKS;
    private boolean waitLoadPhase = false;
    private boolean matchStartPhase = false;
    private boolean matchPreBeginTitle = false;
    private boolean gamePhase = false;
    private boolean setupCamera = false;

    public static final int CONST_MATCH_WAIT_LOAD_TICKS = 30;
    public static final int CONST_MATCH_START_TICKS = 40;
    private int matchStartTicks = CONST_MATCH_START_TICKS;

    private boolean aiStartFlag = false;
    public boolean getAIStartFlag() { return aiStartFlag; }

    private boolean introFlag = false;
    public boolean inIntro() {

        return introFlag || waitLoadPhase;

    }

    public boolean inOutro() {

        return outroPhase;

    }
    public static final ArrayList<BlockPosition> DEBUG = new ArrayList<>();

    public void apply(ArenaBoundaryConfiguration configuration, Vector offset) {

        for(ArenaBoundaryConfiguration.ArenaBoundaryBlock block : configuration.getPaintableSurfaces()) {

            BlockFlagManager.BlockFlag flag = getBlockFlagManager().getBlock(offset,
                    offset.getBlockX() + block.x,
                    offset.getBlockY() + block.y,
                    offset.getBlockZ() + block.z);
            flag.setPaintable(true);
            flag.setWall(block.wall);

        }

    }

    public void broadcastDebugMessage(String msg) {

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.getPlayer().sendMessage("§9[Match/DBG] §7" + msg);

        }

    }

    private HashMap<SplatoonHumanPlayer, Inventory> playerLobbyInventories = new HashMap<>();

    public static final int MAX_PLAYERS = 18;

    private boolean publicMatch=false;


    public boolean isOwner(SplatoonHumanPlayer player) {

        if(publicMatch) { return false; }
        return owner == player;

    }

    private int playersPerTeam;
    public int playersPerTeam() { return playersPerTeam; }
    public int playerCount(int teamID) {

        int i = 0;

        for(Integer integer : teamIDs.values()) {

            if(integer == teamID) { i++; }

        }

        return i;

    }

    public static final int MAX_SPECTATORS = 4;

    public void updatePlayerLobbyInventories() {

        for(SplatoonHumanPlayer player : playerLobbyInventories.keySet()) {

            updateInventory(player);

        }

    }

    private ArrayList<AIPlayer> getAIPlayersForLobbyTeam(int ii) {

        ArrayList<AIPlayer> aiPlayers = new ArrayList<>();
        for(AIPlayer entry : this.aiPlayers) {

            if(entry.getTeam() == ii) {

                aiPlayers.add(entry);

            }

        }
        return aiPlayers;

    }

    public ArrayList<SplatoonHumanPlayer> getPlayersForLobbyTeam(int teamID) {

        ArrayList<SplatoonHumanPlayer> list = new ArrayList<>();
        for(Map.Entry<SplatoonPlayer, Integer> entry : teamIDs.entrySet()) {

            if(entry.getValue() == teamID) {

                list.add((SplatoonHumanPlayer) entry.getKey());

            }

        }
        return list;

    }

    public static final int COUNT_DOWN_DEFAULT = 5;
    private int startCountdown = COUNT_DOWN_DEFAULT;
    private boolean countdownPhase = false;

    private HashMap<SplatoonPlayer, Integer> musicIDs = new HashMap<>();

    private ArrayList<SplatoonHumanPlayer> spectators = new ArrayList<>();

    public static void main(String[] args) {

        int slotID = 6;
        int slotOffset = 6;
        int y = slotID / 3;
        int x = slotID % 3;

        int xval = slotOffset+x;
        int yval = y*9;

    }

    private static HashMap<Integer, Material> teamIDtoMaterial = new HashMap<>();
    static  {

        teamIDtoMaterial.put(-1, Material.ENDER_EYE);
        teamIDtoMaterial.put(0, Material.DIAMOND_HOE);
        teamIDtoMaterial.put(1, Material.IRON_HOE);
        teamIDtoMaterial.put(2, Material.GOLDEN_HOE);
        teamIDtoMaterial.put(3, Material.STONE_HOE);
        teamIDtoMaterial.put(4, Material.WOODEN_HOE);

    }

    public void addAIPlayer(String name, int teamID, AIProperties.Difficulty difficulty, AIWeaponManager.AIPrimaryWeaponType type) {

        AIPlayer player = new AIPlayer(name, difficulty, type);
        player.team = teamID;
        aiPlayers.add(player);

    }

    public int getTeamID(SplatoonHumanPlayer player) {

        if(teamIDs.containsKey(player)) {

            return teamIDs.get(player);

        } else if(spectators.contains(player)) {

            return -1;

        } else {

            return -2;

        }

    }

    public boolean canJoinTeam(int teamID) {

        if(teamID >= 0) {

            return (getPlayersForLobbyTeam(teamID).size()+getAIPlayersForLobbyTeam(teamID).size()) < playersPerTeam();

        } else {

            switch (teamID) {

                case -1:
                    return spectators.size() < MAX_SPECTATORS;
                case -2:
                    return true;

            }

        }
        return false;

    }

    private HashMap<SplatoonHumanPlayer, Integer> remainingUpdateTicks = new HashMap<>();
    public void queuePlayerInventoryUpdate(SplatoonHumanPlayer player) {

        if(!remainingUpdateTicks.containsKey(player)) {

            remainingUpdateTicks.put(player, 3);

        }

    }


    public void giveLobbyItems(Player player) {

        for(int i = 0; i < 9; i++) {

            player.getInventory().setItem(i, null);

        }
        player.getInventory().setItem(4, new ItemBuilder(Material.PAPER).setDisplayName("§eMatchmenü öffnen").addLore("§7Öffnet das Raummenü.").addToNBT("OpenMatchScreen", true).create());
        player.getInventory().setItem(0, new ItemBuilder(Material.CHEST).setDisplayName("§cInventar").addLore("§7Öffnet dein Inventar.").addToNBT("OpenInventory", true).create());
        player.getInventory().setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§4Match verlassen").addLore("§7Entfernt dich aus dem Match.").addToNBT("QuitMatch", true).create());


    }

    public SplatoonHumanPlayer getPlayerFromUUID(UUID uuid) {

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            if(player.getUUID().equals(uuid)) { return player; }

        }
        return null;

    }

    public void managePlayer(SplatoonHumanPlayer player, UUID uuid) {

        SplatoonHumanPlayer player1 = getPlayerFromUUID(uuid);
        if(player1 != null) {

            Inventory inventory = Bukkit.createInventory(null, 27, PLAYER_MANAGE_TITLE);
            for (int i = 0; i < 27; i++) {

                inventory.setItem(i, ItemBuilder.getUnclickablePane());

            }
            inventory.setItem(13, new ItemBuilder(Material.PLAYER_HEAD).withTextureAndSignature(
                    player.getGameProfile()
            ).addToNBT("UUID", uuid.toString()).setDisplayName("§7" + player.getName()).create());

            inventory.setItem(11, new ItemBuilder(Material.HOPPER).setDisplayName("§cKicken").addLore("§7Entfernt den Spieler", "§7aus dem Raum.").addToNBT("KickPlayer", true).create());
            inventory.setItem(15, new ItemBuilder(Material.DIAMOND).addEnchantment(Enchantment.DURABILITY, 1).addAttributeHider().setDisplayName("§cKicken").addLore("§7Übergibt den Raum an", "§7diesen Spieler.").addToNBT("PromotePlayer", true).create());

            inventory.setItem(26, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Exit", true).create());
            player.getPlayer().openInventory(inventory);

        }

    }

    public void updateInventory(SplatoonHumanPlayer player11) {

        Inventory inventory = playerLobbyInventories.get(player11);
        int ii;
        int rowDistance = 0;

        if(teamCount == 2) { rowDistance=9; }

        for(ii = 0; ii < teamCount; ii++) {

            int slotID = (ii*9) + (rowDistance);
            for(int i = 1; i < 6; i++) {

                inventory.setItem(slotID+i, ItemBuilder.getUnclickablePane());

            }

            ItemBuilder builder = new ItemBuilder(teamIDtoMaterial.getOrDefault(ii, Material.BARRIER));
            int playerCount = playerCount(ii)+getAIPlayersForLobbyTeam(ii).size();

            boolean full = playerCount == playersPerTeam();

            builder.setDisplayName("§7§lTeam #" + (ii+1) + " §8(" + playerCount + "/" + playersPerTeam() + ")");
            builder.addLore("");
            if(!teamIDs.containsKey(player11)) {

                if(full) {

                    builder.addLore("§cDieses Team ist voll!");

                } else {

                    builder.addLore("§eKlicke §7zum Betreten des Teams!");

                }

            } else {

                if(teamIDs.get(player11) == ii) {

                    builder.addLore("§a" + Characters.OKAY + " §7Du bist in diesem Team!");

                } else {

                    if(!full) {

                        builder.addLore("§eKlicke §7zum Betreten des Teams!");

                    } else {

                        builder.addLore("§cDieses Team ist voll!");

                    }

                }

            }

            builder.addToNBT("EnterTeam", ii);
            inventory.setItem(slotID, builder.create());
            int offset = 1;
            for(int of = 0; of < playersPerTeam(); of++) {

                inventory.setItem(slotID + offset + of, null);

            }

            ArrayList<SplatoonHumanPlayer> players = getPlayersForLobbyTeam(ii);
            for(int i = 0; i < playersPerTeam(); i++) {

                if((players.size()-1) >= i) {

                    SplatoonHumanPlayer player = (SplatoonHumanPlayer) players.get(i);
                    WeaponSetItem item = player.getInventory().getEquippedSet();
                    WeaponSet set = item.getSet();
                    SplatoonPrimaryWeapon weapon = (SplatoonPrimaryWeapon) SplatoonWeaponRegistry.getDummy(set.getPrimaryWeapon());
                    ItemBuilder builder1 = new ItemBuilder(weapon.getRepresentiveMaterial()).setUnbreakable(true);
                    if(weapon.getResourcepackOption() != null) { builder1.setDurability(weapon.getResourcepackOption().getDamageValue()); }

                    if(isOwner(player11)) {

                        builder1.setDisplayName("§c" + Characters.STAR + " " + player.getName());

                    } else {

                        builder1.setDisplayName("§7" + player.getName());

                    }

                    builder1.addLore("");
                    builder1.addLore("§7Spielt das Waffenset:");
                    builder1.addLore("§e" + set.getName());
                    builder1.addLore("");
                    builder1.addLore("§7Besteht aus:");
                    builder1.addLore("§8- §e" + weapon.getName());
                    builder1.addLore("§8- §e" + SplatoonWeaponRegistry.getDummy(set.getSecondary()).getName());
                    builder1.addLore("§8- §e" + SplatoonWeaponRegistry.getDummy(set.getSpecial()).getName());
                    builder1.addLore("");

                    if(isOwner(player11)) {

                        builder1.addToNBT("ManagePlayer", player.getUUID().toString());
                        builder1.addLore("§eKlicke §7um Aktionen für", "§7diesen Spieler anzuzeigen.");

                    }

                    inventory.setItem(slotID+offset, builder1.create());
                    offset++;

                }

            }
            for(AIPlayer player : getAIPlayersForLobbyTeam(ii)) {

                ItemBuilder builder1 = new ItemBuilder(Material.REDSTONE).setDisplayName("§7Bot #" + (aiPlayers.indexOf(player)+1));
                builder1.addLore("§7KI-Spieler");
                builder1.addLore("§aDbg AIProperties");
                builder1.addLore("Aggressiveness: " + player.getProperties().getAggressiveness() + "%");
                builder1.addLore("Accuracy: " + player.getProperties().getAccuracy() + "%");
                builder1.addLore("WeaponHandling: " + player.getProperties().getWeaponHandling() + "%");
                builder1.addLore("");

                if(isOwner(player11)) {

                    builder1.addLore("§eKlicke §7um den Bot zu verwalten.");
                    builder1.addToNBT("ManageAI", aiPlayers.indexOf(player));

                }

                inventory.setItem(slotID+offset, builder1.create());
                offset++;

            }
            offset=playersPerTeam()+1;
            if(playerCount < playersPerTeam()) {

                if(isOwner(player11)) {

                    inventory.setItem(slotID + offset, new ItemBuilder(Material.CHEST).setDisplayName("§cKI-Spieler hinzufügen").addToNBT("AddAI", ii).create());

                } else {

                    inventory.setItem(slotID+offset, ItemBuilder.getUnclickablePane());

                }

            } else {

                inventory.setItem(slotID+offset, ItemBuilder.getUnclickablePane());

            }

        }
        ii++;
        int slot = ii*9;
        inventory.setItem(slot, new ItemBuilder(Material.ENDER_EYE).setDisplayName("§e" + spectators.size() + " Zuschauer").addLore("§eKlicke §7um als Zuschauer", "§7das Kampfgeschehen zu verfolgen.").addToNBT("EnterTeam", -1).create());
        for(int i = 0; i < MAX_SPECTATORS; i++) {

            if((spectators.size() - 1) >= i) {

                ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);

                SplatoonHumanPlayer player = (spectators.get(i));
                builder.withTextureAndSignature(player.getGameProfile());
                builder.setDisplayName("§8" + player.getName());
                inventory.setItem((slot + 1 + i), builder.create());

            } else {

                inventory.setItem((slot + 1 + i), null);

            }

        }

        // Bottom Bar
        ItemBuilder leaveMatch = new ItemBuilder(Material.BARRIER).setDisplayName("§cMatch verlassen").addToNBT("LeaveMatch", true);
        inventory.setItem(45, leaveMatch.create());

        // Musik
        ItemBuilder music = new ItemBuilder(Material.JUKEBOX);
        if(!musicIDs.containsKey(player11)) {

            music.setDisplayName("§bMusik gefällig?").addLore("§7Hier kannst du ein Musikstück", "§7deiner Wahl auswählen.", "", "§eDisclaimer §7Die Titel sind an", "§7das Original angelehnt.");
            music.addToNBT("ChooseMusic", true);

        } else {

            MusicTrack[] tracklist = XenyriaSplatoon.getMusicManager().getTrackList(musicIDs.get(player11));

            MusicTrack track = tracklist[0];
            music.setDisplayName("§e§o§l" + track.getName() + " & " + tracklist[1].getName());
            music.addLore("§eKlicke §7um eine andere Musik zu wählen.");
            music.addToNBT("ChooseMusic", true);

        }
        inventory.setItem(46, music.create());



        // Inventar
        ItemBuilder openInventory = new ItemBuilder(Material.CHEST);
        openInventory.setDisplayName("§cInventar öffnen");
        openInventory.addToNBT("ManageInventory", true);
        inventory.setItem(47, openInventory.create());

        // Match Properties
        if(isOwner(player11)) {

            ItemBuilder matchProperties = new ItemBuilder(Material.GLOWSTONE_DUST);
            matchProperties.setDisplayName("§eMatch-Einstellungen");
            matchProperties.addToNBT("MatchSettings", true);

            inventory.setItem(48, matchProperties.create());

        }

        // Map
        ItemBuilder currentMap = new ItemBuilder(Material.MAP);
        ArenaData data = XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID);
        currentMap.setDisplayName("§eArena: " + data.getArenaName());
        currentMap.addLore("§7Mapreihe: §e" + data.getCategory().getName());
        currentMap.addLore("§7Maximale Teams: §e" + data.getMaxTeams());
        currentMap.addLore("§7Maximale Spieler/Team: §e" + data.getMaxPlayersPerTeam());
        inventory.setItem(49, currentMap.create());

        // Countdown
        ItemBuilder countDown = new ItemBuilder(countdownPhase ? Material.RED_WOOL : Material.YELLOW_WOOL);
        if(isOwner(player11)){

            if(!countdownPhase) {

                countDown.setDisplayName("§eMatch starten?").create();
                countDown.addLore("§7Mit einem §eKlick §7startest", "§7du das Match.");
                countDown.addToNBT("StartGame", true);
                inventory.setItem(50, countDown.create());

            } else {

                countDown.setDisplayName("Countdown aktiv!");
                inventory.setItem(50, countDown.create());

            }

        }

        // Player Pool
        for(int i = 0; i < MAX_PLAYERS; i++) {

            int slotID = i;
            int slotOffset = 6;
            int y = slotID / 3;
            int x = slotID % 3;

            int xval = slotOffset+x;
            int yval = y*9;

            SplatoonHumanPlayer player = ((lobbyPlayerPool.size()-1)>=i) ? lobbyPlayerPool.get(i) : null;
            if(player != null) {

                ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
                GameProfile profile = player.getGameProfile();

                builder.withTextureAndSignature(profile);

                if(player == player11) {

                    if(isOwner(player)) {

                        builder.setDisplayName("§c" + Characters.STAR + " " + player.getName());

                    } else {

                        builder.setDisplayName("§a" + player.getName());

                    }

                }
                builder.addLore("§7Keinem Team zugeteilt.");
                inventory.setItem(xval+yval, builder.create());

            } else {

                inventory.setItem(xval+yval, new ItemStack(Material.AIR));

            }

        }

    }

    public ArrayList<SplatoonHumanPlayer> getPlayerLobbyPool() { return lobbyPlayerPool; }

    public static final String MATCH_OPTIONS = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cMatcheinstellungen";
    public static final String CHOOSE_MAP_CATEGORY = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWähle eine Arena-Kategorie";

    public static final String MUSIC_SELECTION_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWelche Musik?";
    public void showMusicSelection(SplatoonHumanPlayer player) {

        Inventory inventory = Bukkit.createInventory(null, 18, MUSIC_SELECTION_TITLE);

        for(int i = 0; i < 18; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }

        //Fuel
        inventory.setItem(inventory.getSize()-1, new ItemBuilder(Material.EMERALD).setDisplayName("§aAuswählen").addToNBT("SaveTheMelody", true).create());

        int i = 0;
        for(MusicTrack[] tracks : XenyriaSplatoon.getMusicManager().getTrackLists()) {

            ItemBuilder builder = new ItemBuilder(Material.JUKEBOX);
            builder.addAttributeHider();
            MusicTrack track1 = tracks[0];
            MusicTrack track2 = tracks[1];
            builder.setDisplayName("§e§o§l" + track1.getName() + " & " + track2.getName());
            builder.addLore("§eNotiz", "§7Das zweite Lied spielt im", "§7Revierkampf in der letzten Minute.");

            if(musicIDs.containsKey(player) && musicIDs.get(player) == i) {

                builder.addEnchantment(Enchantment.DURABILITY, 1);

            }

            builder.addToNBT("SelectTrack", i);
            inventory.setItem(i, builder.create());
            i++;

        }
        i++;
        inventory.setItem(i, new ItemBuilder(Material.BARRIER).setDisplayName("§cAuswahl zurücksetzen").addToNBT("SelectTrack", -1).create());

        player.getPlayer().openInventory(inventory);

    }

    public void showMapCategories(SplatoonHumanPlayer player) {

        Inventory inventory = Bukkit.createInventory(null, 9, CHOOSE_MAP_CATEGORY);
        for(int i = 0; i < 9; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }

        inventory.setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Exit", true).create());

        int i = 0;
        for(ArenaCategory category : ArenaCategory.values()) {

            if(category != ArenaCategory.INTERNAL) {

                inventory.setItem(i, category.getItemStack());
                i++;

            }

        }
        player.getPlayer().openInventory(inventory);

    }

    public void showMatchOptions(SplatoonHumanPlayer player) {

        Inventory inventory = Bukkit.createInventory(null, 45, MATCH_OPTIONS);
        for(int i = 0; i < 45; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        inventory.setItem(31, new ItemBuilder(Material.GOLDEN_SWORD).setDisplayName("§6Passwort festlegen").addLore("§7Fügt dem Raum ein Passwort hinzu.", "", "§7Wenn du nichts eingibst wird", "§7das Passwort entfernt.").addToNBT("UpdatePassword", "").create());

        ArenaData data = XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID);

        ItemBuilder mapItem = new ItemBuilder(Material.MAP);
        mapItem.setDisplayName("§eArena: " + data.getArenaName());
        mapItem.addLore("§7Max. Teams: §e" + data.getMaxTeams());
        mapItem.addLore("§7Max. Sp./Team: §e" + data.getMaxPlayersPerTeam());
        mapItem.addLore("");
        mapItem.addLore("§eKlicke §7um eine andere", "§7Arena zu wählen.").addToNBT("MapCategories", true).create();
        inventory.setItem(13, mapItem.create());

        ItemBuilder setTeamCount = new ItemBuilder(Material.WHITE_BANNER).setDisplayName("§eTeamanzahl festlegen").addLore("§7Legt fest, wieviele Teams", "§7zwischen wievielen Teams", "§7die Spieler entscheiden können.").addToNBT("SetTeamAmount", true);
        ItemBuilder setPlrTeamCount = new ItemBuilder(Material.BLACK_BANNER).setDisplayName("§eSpielerzahl festlegen").addLore("§7Legt fest wieviele", "§7Spieler in einem Team", "§7sein können.").addToNBT("SetPlayerCount", true);

        inventory.setItem(19, setTeamCount.create());
        inventory.setItem(25, setPlrTeamCount.create());

        inventory.setItem(44, new ItemBuilder(Material.BARRIER).setDisplayName("§cZur Matchlobby").addToNBT("Exit", true).create());
        player.getPlayer().openInventory(inventory);

    }

    public void openLobbyInventory(SplatoonHumanPlayer player) {

        player.getPlayer().openInventory(playerLobbyInventories.get(player));

    }

    public static final String BOT_MANAGE_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cBot verwalten";

    public void saveAIPlayer() {

        BattleMatch.AIPlayer player = null;
        int indx = aiEditorData.aiID;
        if(indx <= (aiPlayers.size()-1)) {

            AIPlayer player1 = aiPlayers.get(indx);
            player1.difficulty = aiEditorData.difficulty;
            player1.weaponType = aiEditorData.weaponType;
            updatePlayerLobbyInventories();

        }

    }

    public void removeAILobbyPlayer() {

        int indx = aiEditorData.aiID;
        if(indx <= (aiPlayers.size()-1)) {

            aiPlayers.remove(indx);

        }
        updatePlayerLobbyInventories();

    }

    public static final String CHOOSE_DIFFICULTY = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cBot-Schwierigkeitsgrad";
    public static final String CHOOSE_WEAPONTYPE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWaffentyp-Wahl";
    public void openDifficultyChooser(SplatoonHumanPlayer player, boolean returnToAIBuilder) {

        Inventory inventory = Bukkit.createInventory(null, 9, CHOOSE_DIFFICULTY);
        for(int i = 0; i < 9; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }

        int x = 0;
        for(AIProperties.Difficulty difficulty : AIProperties.Difficulty.values()) {

            ItemBuilder builder = new ItemBuilder(difficulty.getMaterial()).setDisplayName(difficulty.getName());
            builder.addToNBT("SetDifficulty", difficulty.name());
            builder.addAttributeHider();

            if(difficulty == aiEditorData.difficulty) {

                builder.addEnchantment(Enchantment.DURABILITY, 1);

            }

            inventory.setItem(x, builder.create());
            x++;

        }
        inventory.setItem(8, new ItemBuilder(Material.EMERALD).setDisplayName("§aSpeichern").addToNBT(returnToAIBuilder ? "BackToAIEditor" : "BackToAIBuilder", false).create());
        player.getPlayer().openInventory(inventory);

    }
    public void openWeaponTypeChooser(SplatoonHumanPlayer player, boolean returnToAIBuilder) {

        Inventory inventory = Bukkit.createInventory(null, 9, CHOOSE_WEAPONTYPE);
        for(int i = 0; i < 9; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }

        int x = 0;
        for(AIWeaponManager.AIPrimaryWeaponType type : AIWeaponManager.AIPrimaryWeaponType.values()) {

            ItemBuilder builder = type.createItem();
            builder.addToNBT("SetType", type.name());

            if(type == aiEditorData.weaponType) {

                builder.addEnchantment(Enchantment.DURABILITY, 1);
                builder.addAttributeHider();

            }

            inventory.setItem(x, builder.create());
            x++;

        }
        inventory.setItem(8, new ItemBuilder(Material.EMERALD).setDisplayName("§aSpeichern").addToNBT(returnToAIBuilder ? "BackToAIEditor" : "BackToAIBuilder", false).create());
        player.getPlayer().openInventory(inventory);

    }

    public AIPlayer getSelectedAIPlayer() { return aiPlayers.get(aiEditorData.aiID); }

    public void updateSelectedWeaponType(AIWeaponManager.AIPrimaryWeaponType type) {

        aiEditorData.weaponType = type;

    }

    public void updateSelectedDifficulty(AIProperties.Difficulty type) {

        aiEditorData.difficulty = type;

    }

    private String password;
    public void updatePassword(String s) {

        password = s;

    }
    public boolean hasPassword() { return password != null; }

    public void showAllMaps(SplatoonHumanPlayer player, ArenaCategory category) {

        ArrayList<ArenaData> arenaData = new ArrayList<>();
        for(ArenaData arenaData1 : XenyriaSplatoon.getArenaRegistry().allArenas()) {

            if(arenaData1.getMap().containsKey(getMatchType()) && arenaData1.getCategory() == category) {

                arenaData.add(arenaData1);

            }

        }

        int slots = (int) (Math.ceil((arenaData.size()+1) / 9d) * 9);
        Inventory inventory = Bukkit.createInventory(null, slots, CHOOSE_MAP);
        for(int i = 0; i < slots; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        int i = 0;
        for(ArenaData data : arenaData) {

            ItemBuilder builder = new ItemBuilder(data.getRepresentiveMaterial());
            builder.setDisplayName(data.getArenaName());
            builder.addLore("§7Max. Teams: §e" + data.getMaxTeams());
            builder.addLore("§7Max. Sp./Team: §e" + data.getMaxPlayersPerTeam());
            builder.addToNBT("SelectMap", data.getID());
            inventory.setItem(i, builder.create());

            i++;

        }
        inventory.setItem(slots-1, new ItemBuilder(Material.BARRIER).addToNBT("Back", true).setDisplayName("§cZurück").create());
        player.getPlayer().openInventory(inventory);

    }

    public void resetTeams() {

        aiPlayers.clear();
        for(Map.Entry<SplatoonPlayer, Integer> entry : teamIDs.entrySet()) {

            if(!lobbyPlayerPool.contains(entry.getKey())) {

                lobbyPlayerPool.add((SplatoonHumanPlayer) entry.getKey());

            }

        }
        teamIDs.clear();
        updatePlayerLobbyInventories();

    }

    public void switchMap(ArenaData arenaData) {

        selectedMapID = arenaData.getID();
        if(teamCount == 0 || teamCount > arenaData.getMaxTeams()) {

            teamCount = arenaData.getMaxTeams();

        }
        if(playersPerTeam() == 0 || playersPerTeam() > arenaData.getMaxPlayersPerTeam()) {

            playersPerTeam = arenaData.getMaxPlayersPerTeam();

        }

    }

    public boolean isTeamCountChangeable() {

        return XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID).getMaxTeams() > 2;

    }

    public boolean isPlayerCountChangeable() {

        return XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID).getMaxPlayersPerTeam() > 1;

    }

    public ArenaData getDataForSelectedArena() { return XenyriaSplatoon.getArenaRegistry().getArenaData(selectedMapID); }

    public void updateMaxTeams(int i) {

        if(i < teamCount) {

            resetTeams();

        }
        teamCount = i;
        updatePlayerLobbyInventories();

    }

    public void updateMaxPlayers(int i) {

        if(i < playersPerTeam) {

            resetTeams();

        }
        playersPerTeam = i;
        updatePlayerLobbyInventories();

    }

    public void setMusicID(SplatoonHumanPlayer player, int i) {

        if(i != -1) {

            musicIDs.put(player, i);

        } else { musicIDs.remove(player); }

    }

    public static final String ADD_AI_SCREEN = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cBot hinzufügen";
    public void showAIAddScreen(SplatoonHumanPlayer player, int id) {

        Inventory inventory = Bukkit.createInventory(null, 27, ADD_AI_SCREEN);
        for(int i = 0; i < 27; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }

        if(id != -1) {
            
            aiEditorData.teamID = id;
            
        }
        
        if(aiEditorData.difficulty == null) { aiEditorData.difficulty = AIProperties.Difficulty.EASY; }
        if(aiEditorData.weaponType == null) {aiEditorData.weaponType = AIWeaponManager.AIPrimaryWeaponType.SHOOTER; }

        AIProperties.Difficulty difficulty = aiEditorData.difficulty;
        AIWeaponManager.AIPrimaryWeaponType weaponType = aiEditorData.weaponType;

        inventory.setItem(11, new ItemBuilder(difficulty.getMaterial()).setDisplayName("§aSchwierigkeitsgrad").addLore(difficulty.getName(), "", "§eKlicke §7zum anpassen.").addToNBT("DifficultyChooser", true).create());
        inventory.setItem(15, weaponType.createItem().setDisplayName("§aWaffenklasse").addLore(weaponType.getName(), "", "§eKlicke §7zum anpassen.").addToNBT("WeaponTypeChooser", true).create());

        inventory.setItem(18, new ItemBuilder(Material.BARRIER).addToNBT("Exit", true).setDisplayName("§cZurück").create());
        inventory.setItem(26, new ItemBuilder(Material.EMERALD).addToNBT("AddAI", true).setDisplayName("§aHinzufügen").create());

        player.getPlayer().openInventory(inventory);

    }

    public void handleAIAdd(SplatoonHumanPlayer player) {

        int targetTeam = aiEditorData.teamID;

        int playersInTeam = getPlayersForLobbyTeam(targetTeam).size()+getAIPlayersForLobbyTeam(targetTeam).size()+1;
        if(playersInTeam <= (playersPerTeam())) {

            addAIPlayer("Spieler" + aiPlayers.size(), targetTeam, aiEditorData.difficulty, aiEditorData.weaponType);
            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Bot hinzugefügt!");
            updatePlayerLobbyInventories();
            openLobbyInventory(player);

        } else {

            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDer Bot kann nicht hinzugefügt werden.");
            player.getPlayer().sendMessage("§8-> §7Das Team ist bereits voll.");

        }

    }

    private HashMap<SplatoonHumanPlayer, SongPlayer> songPlayers = new HashMap<>();

    private int roomID;
    public int getRoomID() { return roomID; }

    public void kickPlayer(SplatoonHumanPlayer player1) {

        player1.leaveMatch();
        XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player1);
        XenyriaSplatoon.getLobbyManager().getLobby().teleportToFights(player1);
        player1.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du wurdest vom Raummeister aus dem §eRaum " + getRoomID() + " gekickt.");

    }

    private SplatoonHumanPlayer owner;
    public void changeOwner(SplatoonHumanPlayer player1) {

        owner = player1;
        player1.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du bist nun der §eRaummeister§7.");
        updatePlayerLobbyInventories();

    }


    public boolean isCountdownPhase() { return countdownPhase; }
    public void startCountDown() {

        countdownPhase = true;
        startCountdown = 2;

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.sendMessage(Chat.SYSTEM_PREFIX + "Der Countdown zum Matchstart beginnt!");
            player.getPlayer().closeInventory();

        }

    }

    public boolean canStart() {

        int i = 0;
        for(int teamID = 0; teamID < teamCount; teamID++) {

            if(getAIPlayersForLobbyTeam(teamID).size() > 0 || getPlayersForLobbyTeam(teamID).size() > 0) {

                i++;

            }

        }
        return i>1;

    }

    public void setMatchTicks(int i) {

        remainingGameTicks = i;

    }

    public ArrayList<SplatoonHumanPlayer> getSpectators() { return spectators; }

    public static final String SPECTATOR_MENU_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Wem zuschauen?";
    public Inventory createSpectatorMenu() {

        int rows = getRegisteredTeams().size();
        Inventory inventory = Bukkit.createInventory(null, rows*9, SPECTATOR_MENU_TITLE);
        for(int row = 0; row < rows; row++) {

            Team team = getRegisteredTeams().get(row);
            inventory.setItem(row*9, new ItemBuilder(
                    team.getColor().getWool()
            ).setDisplayName(team.getColor().prefix() + "Team " + team.getColor().getName()).create());

            int offset = 1;
            for(SplatoonPlayer player : getPlayers(team)) {

                WeaponSet set = getUsedWeaponSet(player);
                inventory.setItem((row*9)+offset,
                        new ItemBuilder(Material.PLAYER_HEAD).setDisplayName(player.coloredName()).
                                addLore("§7Spielertyp: §e" + ((player instanceof SplatoonHumanPlayer) ? "Mensch" : "KI")).
                                addLore("").
                                addLore("§8§l> §e7§lWaffenset").
                                addLore("§e" + SplatoonWeaponRegistry.getDummy(set.getPrimaryWeapon()).getName()).
                                addLore("§e" + SplatoonWeaponRegistry.getDummy(set.getSecondary()).getName()).
                                addLore("§e" + SplatoonWeaponRegistry.getDummy(set.getSpecial()).getName()).
                                addLore("").
                                addLore("§eKlicke §7zum teleportieren.").withTextureAndSignature(player.getGameProfile()).addToNBT("TeleportToPlayer", indexOfPlayer(player)).create());
                offset++;

            }

        }
        return inventory;

    }

    public void selectMap(int i) {

        selectedMapID = i;
        updatePlayerLobbyInventories();

    }

    public int getRemainingGameTicks() { return remainingGameTicks; }

    public static final String SPECTATE_MENU_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cZuschauerkamera";
    public void openSpectateMenu(Player player, SplatoonPlayer player1) {

        Inventory inventory = Bukkit.createInventory(null, 9, SPECTATE_MENU_TITLE);
        for(int i = 0; i < 9; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }
        inventory.setItem(0, new ItemBuilder(Material.PLAYER_HEAD).addToNBT("PlayerIndex", indexOfPlayer(player1)).setDisplayName(player1.coloredName()).withTextureAndSignature(player1.getGameProfile()).create());
        inventory.setItem(2, new ItemBuilder(Material.ENDER_EYE).setDisplayName("§eErste Person").addToNBT("CameraMode", SplatoonHumanPlayer.SpectatorCameraMode.FIRST_PERSON.name()).create());
        inventory.setItem(3, new ItemBuilder(Material.COMPASS).setDisplayName("§eRotierend").addToNBT("CameraMode", SplatoonHumanPlayer.SpectatorCameraMode.ROTATE.name()).create());
        inventory.setItem(4, new ItemBuilder(Material.STICK).setDisplayName("§eFixiert").addToNBT("CameraMode", SplatoonHumanPlayer.SpectatorCameraMode.FIXED.name()).create());
        inventory.setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§cNicht mehr zuschauen").addToNBT("QuitSpectator", true).create());
        player.openInventory(inventory);

    }

    public void cancel() {

        ArrayList<SplatoonPlayer> players = (ArrayList<SplatoonPlayer>) getAllPlayers().clone();
        for(SplatoonPlayer player : players) {

            player.leaveMatch();
            if(player instanceof SplatoonHumanPlayer) {

                ((SplatoonHumanPlayer) player).getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Das Match wurde abgebrochen. Du wirst zur Lobby teleportiert.");
                XenyriaSplatoon.getLobbyManager().addPlayerToLobby((SplatoonHumanPlayer) player);

            } else {

                EntityNPC npc = (EntityNPC) player;
                npc.remove();

            }

        }

    }

    private boolean allowRejoin = true;
    public boolean allowRejoining() { return allowRejoin; }

    public int totalPlayerCount() { return teamCount*playersPerTeam; }

    public int getPlayersPerTeamCount() { return playersPerTeam; }

    public static final String CHOOSE_TEAM_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cBitte wähle ein Team";
    private Inventory chooseTeamInventory = Bukkit.createInventory(null, 9, CHOOSE_TEAM_TITLE);
    public void openTeamChooseMenu(SplatoonHumanPlayer player) {

        player.getPlayer().openInventory(chooseTeamInventory);

    }

    public int combinedPlayerCount(Team team) {

        int teamID = getRegisteredTeams().indexOf(team);
        int i = 0;
        for(int x : teamIDs.values()) {

            if(x == teamID) { i++; }

        }
        for(AIPlayer player : aiPlayers) {

            if(player.team == teamID) {

                i++;

            }

        }
        return i;

    }

    public String getPassword() { return password; }


    public class AIEditorData {

        public int teamID;
        private int aiID;
        private AIProperties.Difficulty difficulty;
        private AIWeaponManager.AIPrimaryWeaponType weaponType;

    }

    private AIEditorData aiEditorData = new AIEditorData();

    public void showAIOptions(Player player, AIPlayer player1, boolean write) {

        int indx = aiPlayers.indexOf(player1);
        if(indx != -1) {

            if(write) {

                aiEditorData.aiID = indx;
                aiEditorData.difficulty = player1.difficulty;
                aiEditorData.weaponType = player1.weaponType;

            }

            Inventory inventory = Bukkit.createInventory(null, 27, BOT_MANAGE_TITLE);
            for (int i = 0; i < 27; i++) {

                inventory.setItem(i, ItemBuilder.getUnclickablePane());

            }
            ItemBuilder diff = new ItemBuilder(aiEditorData.difficulty.getMaterial());
            diff.setDisplayName("§aSchwierigkeitsgrad");
            diff.addLore(aiEditorData.difficulty.getName(), "", "§eKlicke §7zum ändern.");
            diff.addToNBT("DifficultyChooser", true);

            inventory.setItem(12, diff.create());
            ItemBuilder weaponType = aiEditorData.weaponType.createItem().addLore("§eKlicke §7um eine andere Klasse zu wählen.");
            weaponType.addToNBT("WeaponTypeChooser", true);
            inventory.setItem(14, weaponType.create());

            inventory.setItem(18, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Exit", true).create());
            inventory.setItem(19, new ItemBuilder(Material.HOPPER).setDisplayName("§4Löschen").addToNBT("Delete", true).create());
            inventory.setItem(26, new ItemBuilder(Material.EMERALD).setDisplayName("§aSpeichern").addToNBT("SaveAIPlayer", indx).create());
            player.openInventory(inventory);

        }

    }

    public class SplatoonPlayerInformation {

        private UUID uuid;
        private String name;
        private int weaponID;

        public SplatoonPlayerInformation(UUID uuid, String name, int weaponID) {

            this.uuid = uuid;
            this.name = name;
            this.weaponID = weaponID;

        }

    }

    //public ArrayList<>

    private ArrayList<SplatoonHumanPlayer> lobbyPlayerPool = new ArrayList<>();

    private HashMap<SplatoonPlayer, Integer> teamIDs = new HashMap<>();
    private ArrayList<AIPlayer> aiPlayers = new ArrayList<>();
    public ArrayList<AIPlayer> getLobbyAIPlayerEntries() { return aiPlayers; }

    public class AIPlayer {

        public AIProperties.Difficulty difficulty;
        public AIWeaponManager.AIPrimaryWeaponType weaponType;
        private String name;
        public String getName() { return name; }

        private AIProperties properties = new AIProperties(50, 75, 90);
        public AIProperties getProperties() { return properties; }

        public AIPlayer(String name, AIProperties.Difficulty difficulty, AIWeaponManager.AIPrimaryWeaponType type) {

            this.name = name;
            this.difficulty = difficulty;
            this.weaponType = type;

        }

        private int team;
        public int getTeam() { return team; }
    }

    private int selectedMapID = 1;
    public int getSelectedMapID() { return selectedMapID; }

    public ArrayList<Integer> availableArenas() {

        ArrayList<Integer> integers = new ArrayList<>();
        for(ArenaData data : XenyriaSplatoon.getArenaRegistry().allArenas()) {

            if(data.getMaxTeams() <= teamCount) {

                integers.add(data.getID());

            }

        }
        return integers;

    }

    private int teamCount = 2;
    public int getTeamCount() { return teamCount; }

}
