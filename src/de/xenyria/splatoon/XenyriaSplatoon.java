package de.xenyria.splatoon;

import com.comphenix.protocol.ProtocolLibrary;
import com.destroystokyo.paper.PaperConfig;
import com.mojang.brigadier.Message;
import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.logger.XenyriaSpigotLogger;
import de.xenyria.servercore.spigot.util.WorldUtil;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.worker.PathfindingManager;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.ArenaRegistry;
import de.xenyria.splatoon.arena.builder.ArenaBuilder;
import de.xenyria.splatoon.commands.*;
import de.xenyria.splatoon.game.equipment.gear.registry.SplatoonGearRegistry;
import de.xenyria.splatoon.game.equipment.gear.registry.SplatoonGenericGearRegistry;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.listeners.*;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.DebugMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.level.Level;
import de.xenyria.splatoon.game.player.userdata.level.LevelTree;
import de.xenyria.splatoon.game.sound.MusicManager;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.lobby.PlazaLobbyManager;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import de.xenyria.splatoon.shootingrange.ShootingRangeManager;
import de.xenyria.splatoon.tutorial.TutorialManager;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;


public class XenyriaSplatoon extends JavaPlugin {

    public static byte[] RESOURCE_PACK_HASH = null;
    public static final String RESOURCE_PACK_PROVIDER = "http://xenyria.net:8080/splatoon";

    private static Plugin plugin;
    public static Plugin getPlugin() { return plugin; }

    private static XenyriaSpigotLogger logger = new XenyriaSpigotLogger('3', "Splatoon");
    public static XenyriaSpigotLogger getXenyriaLogger() { return logger; }

    public static void registerListener(Listener listener) {

        Bukkit.getPluginManager().registerEvents(listener, plugin);

    }

    private static SplatoonWeaponRegistry weaponRegistry;
    public static SplatoonWeaponRegistry getWeaponRegistry() { return weaponRegistry; }

    private static SplatoonGearRegistry gearRegistry;
    public static SplatoonGearRegistry getGearRegistry() { return gearRegistry; }

    private static TutorialManager tutorialManager;
    public static TutorialManager getTutorialManager() { return tutorialManager; }

    private static ArenaRegistry arenaRegistry;
    public static ArenaRegistry getArenaRegistry() { return arenaRegistry; }

    private static LevelTree levelTree;
    public static LevelTree getLevelTree() { return levelTree; }

    private static ArenaBuilder arenaBuilder = new ArenaBuilder();
    public static ArenaBuilder getArenaBuilder() { return arenaBuilder; }

    private static ArenaProvider arenaProvider;
    public static ArenaProvider getArenaProvider() { return arenaProvider; }

    private static PlazaLobbyManager lobbyManager;
    public static PlazaLobbyManager getLobbyManager() { return lobbyManager; }

    private static SplatoonGenericGearRegistry genericGearRegistry;
    public static SplatoonGenericGearRegistry getGenericGearRegistry() { return genericGearRegistry; }

    private static WeaponSetRegistry weaponSetRegistry;
    public static WeaponSetRegistry getWeaponSetRegistry() { return weaponSetRegistry; }

    private static ShootingRangeManager shootingRangeManager;
    public static ShootingRangeManager getShootingRangeManager() { return shootingRangeManager; }

    private static MusicManager musicManager;
    public static MusicManager getMusicManager() { return musicManager; }

    public static void initLobbyManager() {

        lobbyManager = new PlazaLobbyManager();

    }

    public void onEnable() {


        /*XenyriaSpigotAPI.HANDLE_TAB_LIST = false;
        XenyriaSpigotAPI.HANDLE_PLAYER_INFO = false;
        XenyriaSpigotAPI.disablePlayerInfo();*/
        XenyriaSpigotServerCore.setTOSOnly();

        plugin = this;

        try {

            // Bereinigen der Welt-Daten
            WorldUtil.purifyWorld("sp_tutorial");
            WorldUtil.purifyWorld("sp_sr");
            WorldUtil.purifyWorld("sp_lobby");
            WorldUtil.purifyWorld("sp_arena");
            WorldUtil.purgeWorld("sp_sr");
            WorldUtil.purgeWorld("sp_tutorial");
            WorldUtil.purgeWorld("sp_arena");

            File resourcepack = new File(getPlugin().getDataFolder() + File.separator + "resourcepack.zip");
            byte[] buf = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            FileInputStream stream = new FileInputStream(resourcepack);
            byte[] buffer = new byte[8192];
            int n = 0;
            while (n != -1) {
                n = stream.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            RESOURCE_PACK_HASH = digest.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < RESOURCE_PACK_HASH.length; i++) {
                sb.append(Integer.toString((RESOURCE_PACK_HASH[i] & 0xff) + 0x100, 16).substring(1));
            }
            String fileHash = sb.toString();
            digest.reset();
            stream.close();

            getXenyriaLogger().log("Resourcepack-Hash festgelegt: §e" + fileHash);

        } catch (Exception e) {

            e.printStackTrace();
            Bukkit.shutdown();

        }

        getCommand("splatoon").setExecutor(new SplatoonCommand());
        getCommand("suicide").setExecutor(new SuicideCommand());
        getCommand("weapon").setExecutor(new WeaponCommand());
        getCommand("tutorial").setExecutor(new TutorialCommand());
        getCommand("aidbg").setExecutor(new AIDebugCommand());
        getCommand("pe").setExecutor(new ProjectileExaminerCommand());
        getCommand("arena").setExecutor(new ArenaCommand());
        getCommand("match").setExecutor(new MatchCommand());
        getCommand("dsv").setExecutor(new DetermineShootVectorCommand());
        getCommand("slobby").setExecutor(new SplatoonLobbyCommand());
        getCommand("slobby").setAliases(Arrays.asList("sl"));
        getCommand("shootingrange").setExecutor(new ShootingRangeCommand());
        getCommand("shootingrange").setAliases(Arrays.asList("sr"));
        getCommand("scoins").setExecutor(new SplatoonCoinsCommand());
        getCommand("benchmark").setExecutor(new BenchmarkCommand());

        new PlayerEventHandler();
        new InitializeListener();
        new InventoryListener();
        new MatchGUIListener();

        arenaProvider = new ArenaProvider();
        weaponRegistry = new SplatoonWeaponRegistry();
        gearRegistry = new SplatoonGearRegistry();
        levelTree = new LevelTree();
        arenaRegistry = new ArenaRegistry();
        genericGearRegistry = new SplatoonGenericGearRegistry();
        weaponSetRegistry = new WeaponSetRegistry();
        musicManager = new MusicManager();
        //match.registerTeam(Team.DEBUG_TEAM_3);
        //match.registerTeam(Team.DEBUG_TEAM_4);
        /*match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.SMOOTH_STONE);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.GRAY_CONCRETE);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.TERRACOTTA);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.DIRT);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.COARSE_DIRT);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.COAL_BLOCK);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.YELLOW_TERRACOTTA);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.LIGHT_GRAY_CONCRETE);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.CYAN_CONCRETE);
        match.getMap().getPaintDefinition().getPaintableMaterials().add(Material.QUARTZ_BLOCK);*/

        ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolListener());

        new Thread(() -> {

            while (true) {

                try {

                    Thread.sleep(1);

                    EntityNPC.equipmentAsyncTick();
                    for(SplatoonHumanPlayer player : SplatoonHumanPlayer.getHumanPlayers()) {

                        player.getEquipment().asyncTick();

                    }

                } catch (Exception e) {

                    e.printStackTrace();

                }

            }

        }).start();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            BattleMatch.globalTicker++;
            if(BattleMatch.globalTicker > 10) {

                BattleMatch.globalTicker = 0;

            }

            EntityNPC.tickNPCs();
            EntityNPC.equipmentSyncTick();
            AbstractShopkeeper.tickShopKeepers();

            for(SplatoonHumanPlayer player : SplatoonHumanPlayer.getHumanPlayers()) {

                player.tick();
                if(player.getEquipment() != null) {

                    player.getEquipment().syncTick();

                }

            }

            Match.tickMatches();

        }, 1l, 1l);


        try {

            tutorialManager = new TutorialManager();
            shootingRangeManager = new ShootingRangeManager();

        } catch (Exception e) {

            e.printStackTrace();
            Bukkit.shutdown();

        }

        new PathfindingManager();

    }

}
