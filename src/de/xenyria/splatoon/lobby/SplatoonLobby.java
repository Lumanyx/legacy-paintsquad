package de.xenyria.splatoon.lobby;

import com.destroystokyo.paper.Title;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.camera.CinematicSequence;
import de.xenyria.servercore.spigot.display.DisplayDimensions;
import de.xenyria.servercore.spigot.display.MapDisplay;
import de.xenyria.servercore.spigot.display.animation.FadeAnimation;
import de.xenyria.servercore.spigot.display.image.ImageManager;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.util.ProgressBarUtil;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchControlInterface;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.match.scoreboard.ScoreboardSlotIDs;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.gear.GearItem;
import de.xenyria.splatoon.game.player.userdata.level.Level;
import de.xenyria.splatoon.game.player.userdata.level.LevelTree;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.lobby.npc.RecentPlayerNPC;
import de.xenyria.splatoon.lobby.npc.animation.IdleAnimation;
import de.xenyria.splatoon.lobby.npc.animation.TalkAnimation;
import de.xenyria.splatoon.lobby.npc.animation.WalkAnimation;
import de.xenyria.splatoon.lobby.shop.AbstractShop;
import de.xenyria.splatoon.lobby.shop.gear.GearShop;
import de.xenyria.splatoon.lobby.shop.gear.GearShopItem;
import de.xenyria.splatoon.lobby.shop.gear.GearShopkeeper;
import de.xenyria.splatoon.lobby.shop.item.ShopItem;
import de.xenyria.splatoon.lobby.shop.weapons.WeaponShop;
import de.xenyria.splatoon.lobby.shop.weapons.WeaponShopkeeper;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;

public class SplatoonLobby extends Match {

    private static Location lobbySpawn;
    public static Location getLobbySpawn() { return lobbySpawn; }

    public void tick() {

        for(GearShop shop : gearShops) {

            for(ShopItem item : shop.getSortiment().getItems()) {

                GearShopItem item1 = (GearShopItem) item;
                item1.tick();

                for(SplatoonHumanPlayer player : getHumanPlayers()) {

                    if(player.getLocation().toVector().distance(item.getItem().getLocation().toVector()) <= 5) {

                        item1.playerVisibilityCheck(player.getPlayer());

                    }

                }

            }

        }

        super.tick();

    }

    private ArrayList<GearShop> gearShops = new ArrayList<>();

    private Vector camera_1_introduce_begin = new Vector(-.5, 67.75, -7.5);
    private Vector camera_1_introduce_end = new Vector(-.5, 80, -7.5);
    private Vector camera_2_introduce_start = new Vector(-27.86, 80, 13.5);
    private Vector camera_2_introduce_end = new Vector(-18.5, 82, 15);
    private Vector camera_2_introduce_focus = new Vector(-25.5, 84.5, 29.5);
    private Vector camera_3_introduce_start = new Vector(-5.97, 74.25, 16.632);
    private Vector camera_3_introduce_end = new Vector(-8.187, 74.25, 24.92);
    private Vector camera_4_introduce_start = new Vector(-19.154, 68.74, -6.5);
    private Vector camera_4_introduce_end = new Vector(-19.154, 68.74, -10.75);
    private Vector camera_4_introduce_focus = new Vector(-24.5, 67.5, -8.5);
    private Vector camera_5_introduce_start = new Vector(1.5, 69.05, 52.5);
    private Vector camera_5_introduce_end = new Vector(-2.5, 69.05, 52.5);
    private Vector camera_5_introduce_focus = new Vector(-0.5, 70.5, 62.5);
    private Vector camera_6_introduce_start = new Vector(-3.5, 69.05, 23.5);
    private Vector camera_6_introduce_end = new Vector(3.5, 69.05, 23.5);
    private Vector camera_6_introduce_focus = new Vector(-1.5, 67.5, 14.5);
    private Vector camera_7_introduce_start = new Vector(-11.5, 67.75, 17.74);
    private Vector camera_7_introduce_end = new Vector(-15.946, 68, 10);

    private Location footGearShopkeeperLocation,bodyGearShopkeeperLocation,headGearShopkeeperLocation, weaponShopkeeperLocation;
    private ArrayList<Vector> footGearItemLocations = new ArrayList<>(), bodyGearItemLocations = new ArrayList<>(), headGearItemLocations = new ArrayList<>();
    private Location triggerShootingRangeLocation, drinkShopkeeperLocation,snackShopkeeperLocation;
    private Location privateBattle,publicBattle;

    private Vector screen_begin = new Vector(-31, 81, 28);
    private BlockFace screen_normal = BlockFace.NORTH;
    private BlockFace screen_direction = BlockFace.WEST;
    public static final DisplayDimensions SCREEN_DIMENSIONS = new DisplayDimensions(13,7);

    public ArrayList<CinematicSequence> createIntroSequences(Player player) {

        ArrayList<CinematicSequence> sequences = new ArrayList<>();
        CinematicSequence intro1 = new CinematicSequence(camera_1_introduce_begin, camera_1_introduce_end, 0f, -15f, 6 * 60);
        intro1.getTimeline().addAction(40, () -> {

            player.sendTitle(new Title("", "§7Willkommen in der Lobby!", 10, 30, 10));

        });
        CinematicSequence introScreen = new CinematicSequence(camera_2_introduce_start, camera_2_introduce_end, camera_2_introduce_focus, 10 * 60);
        introScreen.getTimeline().addAction(18, () -> {

            player.sendMessage("");
            player.sendMessage(" §8§l> §c§lDer Neuigkeitenbildschirm");
            player.sendMessage(" §7Hier kannst du dich über die aktuellen Neuigkeiten zu");
            player.sendMessage(" §7neuen Inhalten, Waffen oder Veränderungen am Spielmodus");
            player.sendMessage(" §7informieren.");
            player.sendMessage("");

        });
        CinematicSequence storesScreen = new CinematicSequence(camera_3_introduce_start, camera_3_introduce_end, -80f, 8f, 10 * 60);
        storesScreen.getTimeline().addAction(18, () -> {

            player.sendMessage("");
            player.sendMessage(" §8§l> §6§lLäden");
            player.sendMessage(" §7Ab §eStufe 4 §7kannst du neue Waffen sowie Ausrüstungsteile");
            player.sendMessage(" §7wie §eHelme, Brustpanzer §7oder §eStiefel §7kaufen.");
            player.sendMessage("");
            player.sendMessage( " §7Das Angebot richtet sich nach deiner Stufe und ändert sich täglich.");
            player.sendMessage("");

        });
        CinematicSequence restaurantsScreen = new CinematicSequence(camera_7_introduce_start, camera_7_introduce_end, 72.5f, 0f, 10 * 60);
        restaurantsScreen.getTimeline().addAction(18, () -> {

            player.sendMessage("");
            player.sendMessage(" §8§l> §e§lRestaurant & Cafe");
            player.sendMessage(" §7Hier kannst du mit anderen Spielern abhängen und");
            player.sendMessage(" §7ab §eStufe 4 §7Getränke sowie Snacks für");
            player.sendMessage(" §7mehr Belohnungen in Kämpfen kaufen.");
            player.sendMessage("");

        });

        CinematicSequence chestScreen = new CinematicSequence(camera_4_introduce_start, camera_4_introduce_end, camera_4_introduce_focus, 10 * 60);
        chestScreen.getTimeline().addAction(18, () -> {

            player.sendMessage("");
            player.sendMessage(" §8§l> §a§lMysteriöse Kiste");
            player.sendMessage(" §7Hier kannst du mit etwas Glück neue Ausrüstung, Waffensets");
            player.sendMessage(" §7oder Verbesserungen für aktuelle Ausrüstung gewinnen.");
            player.sendMessage("");
            player.sendMessage( " §7§oSchlüssel kannst du durch das erledigen von Quests erhalten.");
            player.sendMessage("");

        });
        CinematicSequence fightScreen = new CinematicSequence(camera_5_introduce_start, camera_5_introduce_end, camera_5_introduce_focus, 10 * 60);
        fightScreen.getTimeline().addAction(18, () -> {

            player.sendMessage("");
            player.sendMessage(" §8§l> §b§lTintenturm");
            player.sendMessage(" §7Hier kannst du gegen andere Spieler antreten. Du kannst:");
            player.sendMessage(" §e- Eigene Räume erstellen und gegen Freunde kämpfen.");
            player.sendMessage(" §e- Dich mit anderen Spielern im Standard/Rangkampf messen.");
            player.sendMessage( " §7§oIn öffentlichen Kämpfen erhältst du mehr Erfahrung und Taler.");
            player.sendMessage("");

        });
        CinematicSequence endScreen = new CinematicSequence(camera_6_introduce_start, camera_6_introduce_end, camera_6_introduce_focus, 4 * 60);
        endScreen.getTimeline().addAction(10, () -> {

            player.sendMessage("");
            player.sendMessage(" §7Das war nun alles zur Lobby. Wir wünschen dir viel Spaß!");
            player.sendMessage("");

        });
        sequences.add(intro1); sequences.add(introScreen); sequences.add(storesScreen); sequences.add(restaurantsScreen); sequences.add(chestScreen); sequences.add(fightScreen); sequences.add(endScreen);
        return sequences;


    }

    public static final ItemStack OPEN_INVENTORY = new ItemBuilder(Material.CHEST).setDisplayName("§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cInventar").create();

    public Location drinkShopTeleport, snackShopTeleport, weaponShopTeleport, headGearTeleport, bodyGearTeleport, footGearTeleport, mysteryChestTeleport;
    public void createLocations() {

        World plazaWorld = plaza();
        lobbySpawn = new Location(plazaWorld, -0.5, 66, 14.5, 0, 0f);

        snackShopTeleport = new Location(plazaWorld, -30.5, 66, -.5, 45, 0);
        drinkShopTeleport = new Location(plazaWorld, -22.5, 66, 36.5, 45, 0);
        footGearTeleport = new Location(plazaWorld, 26.5, 68, 3.5, -40, 0);
        bodyGearTeleport = new Location(plazaWorld, 19.5, 66, 15.5, -45, 0);
        headGearTeleport = new Location(plazaWorld, 11, 66, 26, -72.5f, 0);
        weaponShopTeleport = new Location(plazaWorld, 18, 64, 33.5, 0f, 0f);
        fightTeleport = new Location(plazaWorld, -.5, 68, 55.5, 0, 0);

        footGearShopkeeperLocation = new Location(plazaWorld, 31.5, 68, 9.5, 135f, 0f);
        footGearItemLocations.add(new Vector(30.5, 68.5, 2.5));
        footGearItemLocations.add(new Vector(32.5, 68.5, 1.5));
        footGearItemLocations.add(new Vector(34.5, 68.5, 3.5));
        footGearItemLocations.add(new Vector(35.5, 68.5, 5.5));
        footGearItemLocations.add(new Vector(25.5, 68.5, 7.5));
        footGearItemLocations.add(new Vector(24.5, 68.5, 9.5));
        footGearItemLocations.add(new Vector(24.5, 68.5, 11.5));
        footGearItemLocations.add(new Vector(26.5, 68.5, 13.5));
        footGearItemLocations.add(new Vector(28.5, 68.5, 14.5));

        bodyGearShopkeeperLocation = new Location(plazaWorld, 28.5, 66, 28.5, -90f, 0f);
        bodyGearItemLocations.add(new Vector(24.5, 66.5, 14.5));
        bodyGearItemLocations.add(new Vector(26.5, 66.5, 16.5));
        bodyGearItemLocations.add(new Vector(28.5, 66.5, 17.5));
        bodyGearItemLocations.add(new Vector(30.5, 66.5, 19.5));
        bodyGearItemLocations.add(new Vector(32.5, 66.5, 21.5));
        bodyGearItemLocations.add(new Vector(33.5, 66.5, 23.5));
        bodyGearItemLocations.add(new Vector(33.5, 66.5, 25.5));
        bodyGearItemLocations.add(new Vector(33.5, 66.5, 27.5));
        bodyGearItemLocations.add(new Vector(33.5, 66.5, 29.5));

        headGearShopkeeperLocation = new Location(plazaWorld, 22.5, 66, 29.5, 107f, 0f);
        headGearItemLocations.add(new Vector(15.5, 66.5, 23.5));
        headGearItemLocations.add(new Vector(17.5, 66.5, 23.5));
        headGearItemLocations.add(new Vector(19.5, 66.5, 24.5));
        headGearItemLocations.add(new Vector(20.5, 66.5, 26.5));
        headGearItemLocations.add(new Vector(18.5, 66.5, 29.5));
        headGearItemLocations.add(new Vector(16.5, 66.5, 29.5));
        headGearItemLocations.add(new Vector(14.5, 66.5, 29.5));
        headGearItemLocations.add(new Vector(12.5, 66.5, 29.5));

        weaponShopkeeperLocation = new Location(plazaWorld, 17.5, 64, 37.3, 180f, 0f);
        triggerShootingRangeLocation = new Location(plazaWorld, 24.5, 64.5, 34);
        drinkShopkeeperLocation = new Location(plazaWorld, -27.5, 66, 40.3, 180f, 0f);
        snackShopkeeperLocation = new Location(plazaWorld, -37.6, 66, 7.36, -144f, 0f);
        publicBattle = new Location(plazaWorld, 1.5, 69, 60.5, 180f, 0);
        privateBattle = new Location(plazaWorld, -2.5, 69, 60.5, 180f, 0);

        weaponShopkeeper = new WeaponShopkeeper(weaponShopkeeperLocation);
        weaponShop = new WeaponShop(weaponShopkeeper);

        headShopkeeper = new GearShopkeeper(headGearShopkeeperLocation, "§dHutverkäufer");
        Location[] locations = new Location[headGearItemLocations.size()];
        int i = 0;
        for(Vector vector : headGearItemLocations) {

            locations[i] = new Location(plazaWorld, vector.getX(), vector.getY(), vector.getZ());
            i++;

        }

        headGearShop = new GearShop(headShopkeeper, AbstractShop.ShopType.HEAD_GEAR, locations);
        gearShops.add(headGearShop);

    }

    private GearShop headGearShop,bodyGearShop,footGearShop;
    private GearShopkeeper headShopkeeper,bodyShopkeeper,footShopkeeper;

    private WeaponShopkeeper weaponShopkeeper;
    private WeaponShop weaponShop;
    public WeaponShop getWeaponShop() { return weaponShop; }


    public World plaza() { return Bukkit.getWorld("sp_lobby"); }

    private Location talk1_1 = new Location(plaza(), 8.5, 66, -8.36, 16.5f, 0f);
    private Location talk1_2 = new Location(plaza(), 7.5, 66, -6.36, -158.5f, 0f);

    private Location sit_talk2_1 = new Location(plaza(), -24.5, 67.5, 33.5, 90f, 0f);
    private Location sit_talk2_2 = new Location(plaza(), -24.5, 67.5, 34.5, 90f, 0f);
    private Location sit_talk2_3 = new Location(plaza(), -29.5, 67.5, 33.5, -90f, 0f);

    private Location idle_3 = new Location(plaza(), -4.5, 66, 33.5, -164.5f, 0f);
    private Location idle_4 = new Location(plaza(), -20.1, 74, 22.1, 28.6f, -51.7f);
    private Location idle_5 = new Location(plaza(), -17.37, 74, 23.5, 49.6f, -44.4f);
    private Location idle_6 = new Location(plaza(), -32.5, 78.5, 11.5, -90f, 10f);

    private Location walk_7_1 = new Location(plaza(), -25.5, 66, -3.5, 0f, 0f);
    private Location walk_7_2 = new Location(plaza(), -25.5, 66, 5.5, 0f, 0f);
    private Location walk_7_3 = new Location(plaza(), -19.5, 66, 5.5, 0f, 0f);

    private Location walk_8_1 = new Location(plaza(), -21.5, 66, 27.5, 0f, 0f);
    private Location walk_8_2 = new Location(plaza(), -28.5, 66, 25.5, 0f, 0f);
    private Location walk_8_3 = new Location(plaza(), -24.5, 66, 23.5, 0f, 0f);

    private Location walk_9_1 = new Location(plaza(), 6.5, 66, 15.5f);
    private Location walk_9_2 = new Location(plaza(), 2.5, 66, 17.5f);
    private Location walk_9_3 = new Location(plaza(), 5.5, 66, 18.5f);

    private Location walk_10_1 = new Location(plaza(), -5.5, 66, 25.5);
    private Location walk_10_2 = new Location(plaza(), -8.5, 66, 24.5);
    private Location walk_10_3 = new Location(plaza(), -5.5, 66, 22.5);

    private Location idle_11 = new Location(plaza(), 3.5, 66, 26.5, 180f, 0f);
    private Location idle_12 = new Location(plaza(), 9.5, 66, 10.5, 90f, 0f);
    private Location sit_idle_13 = new Location(plaza(), 10.5, 67.5, 14.5, 90f, 0f);
    private Location talk_3_1 = new Location(plaza(), 3.5, 68, 52.5, -135f, 0f);
    private Location talk_3_2 = new Location(plaza(), 5.5, 68, 50.5, 45f, 0f);
    private Location talk_4_1 = new Location(plaza(), -7.5, 68, 47.5, -72f, 0f);
    private Location talk_4_2 = new Location(plaza(), -5.1, 68, 48.68, 118, 0f);
    private Location talk_5_1 = new Location(plaza(), -14.5, 66, -8.5, -83.1f, 0f);
    private Location talk_5_2 = new Location(plaza(), -12.2, 66, -8.058, 97.5f, 0f);
    private Location idle_13 = new Location(plaza(), -26.5, 66, 17.5, -116.8f, 0f);

    private static ArrayList<RecentPlayerNPC> npcs = new ArrayList<>();
    public static int minRecentPlayerCount() { return npcs.size() + 3; }

    public SplatoonLobby(World world) {

        super(world);
        createLocations();

        npcs.add(new RecentPlayerNPC(talk1_1, new TalkAnimation()));
        npcs.add(new RecentPlayerNPC(talk1_2, new TalkAnimation()));
        npcs.add(new RecentPlayerNPC(sit_talk2_1, new TalkAnimation(), true));
        npcs.add(new RecentPlayerNPC(sit_talk2_2, new TalkAnimation(), true));
        npcs.add(new RecentPlayerNPC(sit_talk2_3, new TalkAnimation(), true));
        npcs.add(new RecentPlayerNPC(idle_3, new IdleAnimation(), false));
        npcs.add(new RecentPlayerNPC(idle_4, new IdleAnimation(), false));
        npcs.add(new RecentPlayerNPC(idle_5, new IdleAnimation(), false));
        npcs.add(new RecentPlayerNPC(idle_6, new IdleAnimation(), true));
        npcs.add(new RecentPlayerNPC(walk_7_1, new WalkAnimation(walk_7_1, walk_7_2, walk_7_3), false));
        npcs.add(new RecentPlayerNPC(walk_8_1, new WalkAnimation(walk_8_1, walk_8_2, walk_8_3), false));
        npcs.add(new RecentPlayerNPC(walk_9_1, new WalkAnimation(walk_9_1, walk_9_2, walk_9_3), false));
        npcs.add(new RecentPlayerNPC(walk_10_1, new WalkAnimation(walk_10_1, walk_10_2, walk_10_3), false));
        npcs.add(new RecentPlayerNPC(idle_11, new IdleAnimation(), false));
        npcs.add(new RecentPlayerNPC(idle_12, new IdleAnimation(), false));
        npcs.add(new RecentPlayerNPC(sit_idle_13, new IdleAnimation(), true));
        npcs.add(new RecentPlayerNPC(talk_3_1, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(talk_3_2, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(talk_4_1, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(talk_4_2, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(talk_5_1, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(talk_5_2, new TalkAnimation(), false));
        npcs.add(new RecentPlayerNPC(idle_13, new IdleAnimation(), false));

        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

            for(RecentPlayerNPC npc : npcs) {

                npc.tick();

            }

        }, 1l, 1l);

        display = new MapDisplay(SCREEN_DIMENSIONS, screen_direction, screen_normal);
        display.setImageChangeTicks(600);
        display.addImages(ImageManager.getImage("testfire"));
        display.addImages(ImageManager.getImage("testfire_alt"));
        display.setDisplayAnimation(new FadeAnimation());
        display.build(world, screen_begin);
        display.nextImage();

        setMatchController(new MatchControlInterface() {
            @Override
            public ArrayList<JumpPoint> getJumpPoints(Team team) {
                return new ArrayList<>();
            }

            @Override
            public void playerAdded(SplatoonPlayer player) {

                player.setSpawnPoint(lobbySpawn);
                player.teleport(lobbySpawn);

                if(player instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer humanPlayer = (SplatoonHumanPlayer) player;
                    XenyriaSpigotPlayer player1 = ((SplatoonHumanPlayer)player).getXenyriaPlayer();
                    player1.getScoreboard().reset();
                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                        player1.getScoreboard().setBoardName("§7§o§lSplatoon §8" + Characters.SMALL_X + " §7Lobby");
                        player1.getScoreboard().setLine(13, "§0");
                        player1.getScoreboard().setLine(12, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7§lSpieler online");
                        player1.getScoreboard().setLine(10, "§0");
                        player1.getScoreboard().setLine(9, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7§lRang");
                        player1.getScoreboard().setLine(7, "§0");
                        player1.getScoreboard().setLine(6, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7§lTaler");
                        player1.getScoreboard().setLine(4, "§0");
                        player1.getScoreboard().setLine(3, "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7§lStufe");
                        updateScoreboard(((SplatoonHumanPlayer) player).getPlayer());

                    }, 1l);


                    ((SplatoonHumanPlayer)player).getPlayer().setGameMode(GameMode.ADVENTURE);
                    ((SplatoonHumanPlayer)player).resetSpawnedRecentPlayers();
                    ((SplatoonHumanPlayer)player).refillRecentPlayerPool();
                    ((SplatoonHumanPlayer)player).updateInventory();

                    SplatoonHumanPlayer player2 = (SplatoonHumanPlayer)player;
                    Scoreboard scoreboard = player2.getPlayer().getScoreboard();

                    if(scoreboard.getTeam("lobby-team-npc") == null) {

                        org.bukkit.scoreboard.Team team = scoreboard.registerNewTeam("lobby-team-npc");
                        team.setPrefix("§8[NPC] ");
                        team.setColor(ChatColor.YELLOW);

                    }
                    for(Level level : XenyriaSplatoon.getLevelTree().allLevels()) {

                        if(scoreboard.getTeam("lobby-team-" + level.getID()) == null) {

                            org.bukkit.scoreboard.Team team = scoreboard.registerNewTeam("lobby-team-" + level.getID());
                            team.setPrefix("§eLv. " + level.getID() + " ");
                            team.setColor(ChatColor.GRAY);



                        }
                        org.bukkit.scoreboard.Team team = scoreboard.getTeam("lobby-team-" + level.getID());
                        if(level.getID() == player2.getUserData().currentLevel()) {

                            team.addEntry(player2.getName());

                        }

                    }

                    for(SplatoonHumanPlayer humanPlayer1 : getHumanPlayers()) {

                        if(humanPlayer1 != humanPlayer) {

                            org.bukkit.scoreboard.Team team = scoreboard.getTeam("lobby-team-" + humanPlayer.getUserData().currentLevel());
                            if(team != null) {

                                team.addEntry(humanPlayer.getName());

                            } else {

                                team = scoreboard.registerNewTeam("lobby-team-" + humanPlayer.getUserData().currentLevel());
                                team.addEntry(humanPlayer.getName());

                            }

                        }

                    }

                }


            }

            @Override
            public void playerRemoved(SplatoonPlayer player) {

                if(player instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player1 = (SplatoonHumanPlayer)player;
                    player1.resetSpawnedRecentPlayers();

                    Scoreboard scoreboard1 = player1.getPlayer().getScoreboard();
                    for(Level level : XenyriaSplatoon.getLevelTree().allLevels()) {

                        org.bukkit.scoreboard.Team team = scoreboard1.getTeam("lobby-team-" + level.getID());
                        if(team != null) {

                            team.unregister();

                        }

                    }
                    if(scoreboard1.getTeam("lobby-team-npc") != null) {

                        scoreboard1.getTeam("lobby-team-npc").unregister();

                    }

                    for(SplatoonHumanPlayer otherPlayer : getHumanPlayers()) {

                        if(otherPlayer != player1) {

                            Scoreboard scoreboard = otherPlayer.getPlayer().getScoreboard();
                            org.bukkit.scoreboard.Team team = scoreboard.getTeam("lobby-team-" + player1.getUserData().currentLevel());
                            if(team != null) { team.removeEntry(player1.getName()); }

                        }

                    }

                }

            }

            @Override
            public void objectAdded(GameObject object) {

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
                    player1.getPlayer().getInventory().setItem(2, SplatoonHumanPlayer.TRANSFORM_TO_SQUID);
                    player1.getPlayer().getInventory().setItem(4, OPEN_INVENTORY);

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
    private MapDisplay display;

    public void updateScoreboard(Player player) {

        XenyriaSpigotPlayer player1 = XenyriaSpigotPlayer.resolveByUUID(player.getUniqueId()).getSpigotVariant();
        SplatoonHumanPlayer humanPlayer = SplatoonHumanPlayer.getPlayer(player);
        int level = humanPlayer.getUserData().currentLevel();
        int target = humanPlayer.getUserData().targetLevel().getID();
        int playerAmount = Bukkit.getOnlinePlayers().size();
        player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_PLAYERS, "§e§l" + playerAmount + "");
        player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_COINS, "§e§l" + humanPlayer.getUserData().getCoins() + "");
        player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_RANK, "§6§lC- §e100 Punkte §7bis §6§lRang C");

        if(level == target) {

            double percentage = humanPlayer.getUserData().currentLevelPercentage();
            if(percentage == 100D) {

                player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_LEVEL, "§eMaximal Stufe §6(Lv. " + target + ") §eerreicht!");

            } else {

                player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_LEVEL, "§eLv. " + humanPlayer.getUserData().currentLevel() + " " + ProgressBarUtil.generateProgressBar(humanPlayer.getUserData().currentLevelPercentage(), 10, "§6", "§8"));

            }

        } else {

            player1.getScoreboard().setLine(ScoreboardSlotIDs.LOBBY_LEVEL, "§eLv. " + humanPlayer.getUserData().currentLevel() + " " + ProgressBarUtil.generateProgressBar(humanPlayer.getUserData().currentLevelPercentage(), 10, "§6", "§8") + " §eLv. " + target);

            player1.getScoreboard().setLine(1, "§0");

        }

    }

    @Override
    public MatchType getMatchType() {
        return MatchType.TUTORIAL;
    }

    @Override
    public void removeBeacon(BeaconObject object) {

    }

    public ShopItem getShopItem(int id) {

        for(GearShop shop : gearShops) {

            for(ShopItem item : shop.getSortiment().getItems()) {

                if(item.getShopItemID() == id) { return item; }

            }

        }
        return null;

    }

    private Location fightTeleport;
    public void teleportToFights(SplatoonHumanPlayer player1) {

        player1.getPlayer().teleport(fightTeleport);

    }

}
