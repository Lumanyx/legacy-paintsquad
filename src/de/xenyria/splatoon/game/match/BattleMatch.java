package de.xenyria.splatoon.game.match;

import com.destroystokyo.paper.Title;
import com.mojang.authlib.GameProfile;
import com.mysql.fabric.xmlrpc.base.Data;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.filter.Filter;
import de.xenyria.schematics.internal.placeholder.Placeholder;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
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
import de.xenyria.splatoon.arena.ArenaRegistry;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.color.ColorCombination;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.PlaceholderReader;
import de.xenyria.splatoon.game.match.scoreboard.ScoreboardSlotIDs;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.MapDamageProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.ItemMilkBucket;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public abstract class BattleMatch extends Match {

    public static final String CHOOSE_MAP = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWähle eine Arena";
    private SpigotRoom room;

    public BattleMatch() {

        super(XenyriaSplatoon.getArenaProvider().getArenaWorld());
        room = XenyriaSpigotServerCore.getRoomManager().newRoom();

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
                if(player instanceof SplatoonHumanPlayer && isLobbyPhase()) {

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;
                    humanPlayerAdded(player1);

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

            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

                jumpPoints.remove(player);
                lobbyPlayerPool.remove(player);
                teamIDs.remove(player);
                spectators.remove(player);

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

                if(!lobbyPhase) {

                    if (player instanceof SplatoonHumanPlayer) {

                        ((SplatoonHumanPlayer) player).getPlayer().getInventory().setItem(6, StaticItems.OPEN_JUMP_MENU);

                    }

                } else {

                    if(player instanceof SplatoonHumanPlayer) {

                        SplatoonHumanPlayer player1 = (SplatoonHumanPlayer) player;
                        giveLobbyItems(player1.getPlayer());

                    }

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
        });

    }

    public static final String ROOM_PREFIX = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Raum ";

    public void humanPlayerAdded(SplatoonHumanPlayer player) {

        lobbyPlayerPool.add(player);
        player.getXenyriaPlayer().switchRoom(room);

        Inventory inventory = Bukkit.createInventory(null, 54, ROOM_PREFIX + "#?");
        for(int i = 0; i < 54; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }

        playerLobbyInventories.put(player, inventory);
        updatePlayerLobbyInventories();
        player.getPlayer().openInventory(playerLobbyInventories.get(player));

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

    private ArenaProvider.ArenaGenerationTask task;
    public void start() {

        broadcastDebugMessage("Farbkombination wird generiert...");
        colorCombination = ColorCombination.getColorCombinations(teamCount);

        for(int i = 0; i < teamCount; i++) {

            Team team = new Team(colorCombination.color(i));
            registerTeam(team);

            for(Map.Entry<SplatoonPlayer, Integer> entry : teamIDs.entrySet()) {

                SplatoonPlayer player = entry.getKey();
                player.setTeam(getRegisteredTeams().get(entry.getValue()));

            }

        }

        matchStarted = true;
        task = XenyriaSplatoon.getArenaProvider().requestArena(selectedMapID, this);
        broadcastDebugMessage("§eArena #" + selectedMapID + " §7wird angefragt...");

    }

    private ColorCombination colorCombination;
    private Location tempPos = null;

    private boolean lobbyPhase = true;
    private boolean skipIntro = true;
    private boolean matchStarted = false;

    public void tick() {

        if(lobbyPhase) {

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

                        broadcastDebugMessage("Arena-Generation abgeschlossen - Erfolgreich? §e" + task.isSuccessful());
                        if (task.isSuccessful()) {

                            broadcastDebugMessage("Match-Initialisierungssequenz startet.");

                            broadcastDebugMessage("Wende .sbounds-Datei an.");
                            try {

                                ArenaBoundaryConfiguration configuration = ArenaBoundaryConfiguration.fromFile(new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + task.getArenaData().getMap().get(getMatchType()) + ".sbounds"));
                                broadcastDebugMessage("Färbflächen festlegen");
                                apply(configuration, task.getOffset());
                                int teamID = 0;
                                broadcastDebugMessage("Spawns werden eingefügt");
                                for (de.xenyria.splatoon.game.map.Map.TeamSpawn spawn : PlaceholderReader.getSpawns(task.getOffset(), task.getSchematic())) {

                                    if (teamID <= (getRegisteredTeams().size() - 1)) {

                                        getMap().getSpawns().add(spawn);
                                        getMap().pasteSpawn(getWorld(), spawn, getRegisteredTeams().get(teamID));
                                        teamID++;

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

                                    if (!player.isSpectator()) {

                                        player.getEquipment().setPrimaryWeapon(1);
                                        player.getEquipment().setSecondaryWeapon(2);
                                        player.getEquipment().setSpecialWeapon(3);

                                    }

                                }


                                if (!aiPlayers.isEmpty()) {

                                    broadcastDebugMessage("KI-Gegner werden initialisiert");
                                    for (AIPlayer entry : aiPlayers) {

                                        Team team = getRegisteredTeams().get(entry.getTeam());

                                        Location location = getNextSpawnPoint(team);
                                        EntityNPC npc = new EntityNPC(location, team, this);
                                        npc.setSpawnPoint(location);
                                        addPlayer(npc);
                                        npc.disableTracker();
                                        npc.disableAI();
                                        /*npc.getEquipment().setPrimaryWeapon(entry.primaryWeapon);
                                        npc.getEquipment().setSecondaryWeapon(entry.secondaryWeapon);
                                        npc.getEquipment().setSpecialWeapon(entry.specialWeapon);
                                        */broadcastDebugMessage("KI-Gegner (EID: " + npc.getEntityID() + ") zum Match hinzugefügt.");

                                    }

                                    getAIController().initSpots(getAIController().gatherNodesBySpawns());

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

                        }

                    }
                    for(SplatoonPlayer player : getAllPlayers()) {

                        if(!player.isHuman()) {

                            ((EntityNPC)player).enableAI();

                        }

                    }
                    matchStartPhase = false;
                    gamePhase = true;

                }

            } else if(gamePhase) {

                for(SplatoonHumanPlayer player : getHumanPlayers()) {

                    updateValues(player.getPlayer());

                }

                tickSpawnShields();
                super.tick();

            }

            // else {

            //    super.tick();

            //}

        }

    }

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

                if(!player.isSpectator()) {

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

        player.getScoreboard().setBoardName(player2.getColor().prefix() + "§lSplatoon §8" + Characters.SMALL_X + " §7Revierkampf");
        player.getScoreboard().setLine(10, "");
        player.getScoreboard().setLine(9, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Punkte");
        player.getScoreboard().setLine(8, "");
        player.getScoreboard().setLine(7, "");
        player.getScoreboard().setLine(6, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Spezialwaffe");
        player.getScoreboard().setLine(5, "");
        player.getScoreboard().setLine(4, "");
        player.getScoreboard().setLine(3, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Statistik");
        player.getScoreboard().setLine(2, "");
        player.getScoreboard().setLine(1, "");

    }

    public static int globalTicker = 0;

    private void updateValues(Player player1) {

        XenyriaSpigotPlayer player = XenyriaSpigotPlayer.resolveByUUID(player1.getUniqueId()).getSpigotVariant();
        SplatoonHumanPlayer player2 = SplatoonHumanPlayer.getPlayer(player1);
        player.getScoreboard().setLine(ScoreboardSlotIDs.TURFWAR_SCORE, player2.getTeam().getColor().prefix() + "§o§l" + player2.getScoreboardManager().getPointValue());

        // Spezialwaffen-Fortschritt
        int currentPoints = player2.getSpecialPoints();
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

    private int waitLoadTicks = 60;
    private boolean waitLoadPhase = false;
    private boolean matchStartPhase = false;
    private boolean matchPreBeginTitle = false;
    private boolean gamePhase = false;
    private boolean setupCamera = false;
    private int matchStartTicks = 40;

    private boolean introFlag = false;
    public boolean inIntro() {

        return introFlag;

    }

    public boolean inOutro() {

        return false;

    }


    public void apply(ArenaBoundaryConfiguration configuration, Vector offset) {

        for(ArenaBoundaryConfiguration.ArenaBoundaryBlock block : configuration.getPaintableSurfaces()) {

            Vector realPos = offset.clone().add(new Vector(block.x, block.y, block.z));
            Block block1 = getWorld().getBlockAt(realPos.getBlockX(), realPos.getBlockY(), realPos.getBlockZ());
            block1.setMetadata("Paintable", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), true));
            block1.setMetadata("Wall", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), block.wall));

        }

    }

    public void broadcastDebugMessage(String msg) {

        for(SplatoonHumanPlayer player : getHumanPlayers()) {

            player.getPlayer().sendMessage("§9[Match/DBG] §7" + msg);

        }

    }

    private HashMap<SplatoonHumanPlayer, Inventory> playerLobbyInventories = new HashMap<>();

    public static final int MAX_PLAYERS = 18;

    public boolean isOwner(SplatoonHumanPlayer player) {

        return true;

    }

    public int playersPerTeam() { return 4; }
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

        System.out.println(
                xval+yval
        );

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
        player.getInventory().setItem(5, new ItemBuilder(Material.PAPER).setDisplayName("§eMatchmenü öffnen").addLore("§7Öffnet das Raummenü.").create());

    }

    public void updateInventory(SplatoonHumanPlayer player11) {

        Inventory inventory = playerLobbyInventories.get(player11);
        int ii;
        int rowDistance = 0;

        if(teamCount == 2) { rowDistance=9; }

        for(ii = 0; ii < teamCount; ii++) {

            int slotID = (ii*9) + (rowDistance);

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

                    inventory.setItem(slotID + offset, new ItemBuilder(Material.CHEST).setDisplayName("§cKI-Spieler hinzufügen").create());

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
                builder.setDisplayName("§7" + player.getPlayer());
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

    public void showMapCategories(SplatoonHumanPlayer player) {

        Inventory inventory = Bukkit.createInventory(null, 9, CHOOSE_MAP_CATEGORY);
        for(int i = 0; i < 9; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }

        inventory.setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Exit", true).create());

        int i = 0;
        for(ArenaCategory category : ArenaCategory.values()) {

            if(category != ArenaCategory.INTERNAL) {



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
        if((aiPlayers.size()-1) <= indx) {

            aiPlayers.remove(indx);

        }
        updatePlayerLobbyInventories();

    }

    public static final String CHOOSE_DIFFICULTY = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cBot-Schwierigkeitsgrad";
    public static final String CHOOSE_WEAPONTYPE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWaffentyp-Wahl";
    public void openDifficultyChooser(SplatoonHumanPlayer player) {

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
        inventory.setItem(8, new ItemBuilder(Material.EMERALD).setDisplayName("§aSpeichern").addToNBT("BackToAIEditor", false).create());
        player.getPlayer().openInventory(inventory);

    }
    public void openWeaponTypeChooser(SplatoonHumanPlayer player) {

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
        inventory.setItem(8, new ItemBuilder(Material.EMERALD).setDisplayName("§aSpeichern").addToNBT("BackToAIEditor", false).create());
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

            ItemBuilder builder = new ItemBuilder(data.getRepresentiveMaterial())
            i++;

        }
        inventory.setItem(slots-1, new ItemBuilder(Material.BARRIER).addToNBT("Back", true).setDisplayName("§cZurück").create());

    }

    public class AIEditorData {

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
            diff.setDisplayName("§7Schwierigkeitsgrad");
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
