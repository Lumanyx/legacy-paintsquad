package de.xenyria.splatoon.game.match;

import com.destroystokyo.paper.Title;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.schematics.internal.placeholder.Placeholder;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.color.ColorCombination;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.PlaceholderReader;
import de.xenyria.splatoon.game.match.scoreboard.ScoreboardSlotIDs;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.MapDamageProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.VectorUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public abstract class BattleMatch extends Match {

    public BattleMatch() {

        super(XenyriaSplatoon.getArenaProvider().getArenaWorld());


    }

    public void chooseTeam(SplatoonPlayer player, int teamID) {

        teamIDs.put(player, teamID);

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

        task = XenyriaSplatoon.getArenaProvider().requestArena(selectedMapID, this);
        broadcastDebugMessage("§eArena #" + selectedMapID + " §7wird angefragt...");

    }

    private ColorCombination colorCombination;
    private Location tempPos = null;

    private boolean lobbyPhase = true;
    private boolean skipIntro = true;

    public void tick() {

        if(lobbyPhase) {

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

                            for(Team team : getRegisteredTeams()) {

                                int id = 0;
                                for (SplatoonPlayer player : getPlayers(team)) {

                                    player.setSpawnPoint(getNextSpawnPoint(team, id));

                                    id++;
                                    if(id >= 3) {

                                        id = 3;

                                    }

                                }

                            }

                            for(SplatoonPlayer player : getAllPlayers()) {

                                if(!player.isSpectator()) {

                                    player.getEquipment().setPrimaryWeapon(1);
                                    player.getEquipment().setSecondaryWeapon(2);
                                    player.getEquipment().setSpecialWeapon(3);

                                }

                            }


                            if(!aiPlayers.isEmpty()) {

                                broadcastDebugMessage("KI-Gegner werden initialisiert");
                                for(Map.Entry<AIPlayer, Integer> entry : aiPlayers.entrySet()) {

                                    Team team = getRegisteredTeams().get(entry.getValue());

                                    Location location = getNextSpawnPoint(team);
                                    EntityNPC npc = new EntityNPC(location, team, this);
                                    npc.setSpawnPoint(location);
                                    addPlayer(npc);
                                    npc.disableTracker();
                                    npc.disableAI();
                                    npc.getEquipment().setPrimaryWeapon(entry.getKey().primaryWeapon);
                                    npc.getEquipment().setSecondaryWeapon(entry.getKey().secondaryWeapon);
                                    npc.getEquipment().setSpecialWeapon(entry.getKey().specialWeapon);
                                    broadcastDebugMessage("KI-Gegner (EID: " + npc.getEntityID() + ") zum Match hinzugefügt.");

                                }

                                getAIController().initSpots(getAIController().gatherNodesBySpawns());

                            }


                            waitLoadPhase = true;
                            tempPos = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                                    getMap().getIntroductionCamera().getStart().getX(), 16, getMap().getIntroductionCamera().getStart().getZ());

                            for(SplatoonHumanPlayer player : getHumanPlayers()) {

                                player.getPlayer().teleport(tempPos);
                                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 65, 2, false, false, false));
                                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                                player.getPlayer().setFlySpeed(0f);
                                player.getPlayer().setAllowFlight(true);

                            }

                            if(!skipIntro) {

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

    private Inventory roomInventory = Bukkit.createInventory(null, 54, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §a§o§lSplatoon");
    private static HashMap<Integer, Material> teamIDtoMaterial = new HashMap<>();
    static  {

        teamIDtoMaterial.put(-1, Material.ENDER_EYE);
        teamIDtoMaterial.put(0, Material.DIAMOND_HOE);
        teamIDtoMaterial.put(1, Material.IRON_HOE);
        teamIDtoMaterial.put(2, Material.GOLDEN_HOE);
        teamIDtoMaterial.put(3, Material.STONE_HOE);
        teamIDtoMaterial.put(4, Material.WOODEN_HOE);

    }

    public void addAIPlayer(String name, int teamID, int w1, int w2, int w3) {

        AIPlayer player = new AIPlayer(name, w1, w2, w3);
        aiPlayers.put(player, teamID);

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

    public void updateInventory() {

        for(int i = 0; i < 54; i++) {

            roomInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        for(int team = 0; team < teamCount; team++) {

            /*ItemStack teamStack = new ItemBuilder(teamIDtoMaterial.getOrDefault(team, Material.WOODEN_HOE))
                    .setDisplayName("§8§l> §f§lTeam §7§l#" + (team + 1)).addLore(
                            "§8"
                    );
            roomInventory.setItem((team * 9) + 9, );*/

        }

    }

    private HashMap<SplatoonPlayer, Integer> teamIDs = new HashMap<>();
    private HashMap<AIPlayer, Integer> aiPlayers = new HashMap<>();

    public class AIPlayer {

        private String name;
        public String getName() { return name; }

        private AIProperties properties = new AIProperties(50, 75, 90);
        public AIProperties getProperties() { return properties; }

        private int primaryWeapon, secondaryWeapon, specialWeapon;
        public AIPlayer(String name, int primaryWeapon, int secondaryWeapon, int specialWeapon) {

            this.name = name;
            this.primaryWeapon = primaryWeapon;
            this.secondaryWeapon = secondaryWeapon;
            this.specialWeapon = specialWeapon;

        }

    }

    private static final int SPECTATOR_TEAM_ID = -1;

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
