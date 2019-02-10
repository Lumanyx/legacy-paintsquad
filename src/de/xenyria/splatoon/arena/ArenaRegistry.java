package de.xenyria.splatoon.arena;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.placeholder.StoredTeamPlaceholder;
import de.xenyria.splatoon.game.match.MatchType;
import org.bukkit.Material;

import java.util.HashMap;

public class ArenaRegistry {

    private HashMap<Integer, ArenaData> arenas = new HashMap<>();

    public ArenaRegistry() {

        GamemodeSchematicMap shootingRangeMap = new GamemodeSchematicMap();
        shootingRangeMap.put(MatchType.TURF_WAR, "shootingrange");

        ArenaData shootingRangeData = new ArenaData(-1, "SP_SHOOTINGRANGE", ArenaCategory.INTERNAL, shootingRangeMap);
        shootingRangeData.addPaintableMaterial(Material.SMOOTH_QUARTZ);
        shootingRangeData.addPaintableMaterial(Material.GRAY_CONCRETE);
        shootingRangeData.addPaintableMaterial(Material.WHITE_CONCRETE);
        shootingRangeData.addPaintableMaterial(Material.QUARTZ_PILLAR);
        shootingRangeData.addPaintableMaterial(Material.WHITE_TERRACOTTA);
        shootingRangeData.addPaintableMaterial(Material.COARSE_DIRT);
        shootingRangeData.addPaintableMaterial(Material.GRASS_BLOCK);
        arenas.put(-1, shootingRangeData);

        GamemodeSchematicMap map = new GamemodeSchematicMap();
        map.put(MatchType.TURF_WAR, "tutorial_map");

        ArenaData tutorialData = new ArenaData(0, "SP_TUTORIAL", ArenaCategory.INTERNAL, map);
        tutorialData.setMaxPlayersPerTeam(1);
        tutorialData.setMaxTeams(1);
        tutorialData.addPaintableMaterial(Material.STONE_BRICKS);
        tutorialData.addPaintableMaterial(Material.GRASS_BLOCK);
        tutorialData.addPaintableMaterial(Material.GRAY_CONCRETE);
        tutorialData.addPaintableMaterial(Material.CYAN_TERRACOTTA);
        tutorialData.addPaintableMaterial(Material.CHISELED_QUARTZ_BLOCK);
        tutorialData.addPaintableMaterial(Material.OAK_LOG);
        tutorialData.addPaintableMaterial(Material.OAK_LEAVES);
        tutorialData.addPaintableMaterial(Material.DIRT);
        tutorialData.addPaintableMaterial(Material.COARSE_DIRT);
        tutorialData.addPaintableMaterial(Material.BLUE_WOOL);
        tutorialData.addPaintableMaterial(Material.ORANGE_WOOL);
        arenas.put(0, tutorialData);

        GamemodeSchematicMap tw_uu = new GamemodeSchematicMap();
        tw_uu.put(MatchType.TURF_WAR, "turfwar_urchinunderpass");

        ArenaData urchinUnderpass = new ArenaData(1, "Dekabahnstation", ArenaCategory.REPLICA, tw_uu);
        urchinUnderpass.setMaxTeams(2);
        urchinUnderpass.setMaxPlayersPerTeam(4);
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.BLUE_WOOL, StoredTeamPlaceholder.ReplacementType.WOOL, 0));
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.ORANGE_WOOL, StoredTeamPlaceholder.ReplacementType.WOOL, 1));
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.BLUE_CARPET, StoredTeamPlaceholder.ReplacementType.CARPET, 0));
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.BLUE_TERRACOTTA, StoredTeamPlaceholder.ReplacementType.STAINED_CLAY, 0));
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.ORANGE_CARPET, StoredTeamPlaceholder.ReplacementType.CARPET, 1));
        urchinUnderpass.getPlaceholders().add(new StoredTeamPlaceholder(Material.ORANGE_TERRACOTTA, StoredTeamPlaceholder.ReplacementType.STAINED_CLAY, 1));
        urchinUnderpass.addPaintableMaterial(Material.SMOOTH_QUARTZ);
        urchinUnderpass.addPaintableMaterial(Material.GRAY_CONCRETE);
        urchinUnderpass.addPaintableMaterial(Material.LIGHT_GRAY_TERRACOTTA);
        urchinUnderpass.addPaintableMaterial(Material.YELLOW_CONCRETE);
        urchinUnderpass.addPaintableMaterial(Material.SMOOTH_STONE);
        urchinUnderpass.addPaintableMaterial(Material.CHISELED_QUARTZ_BLOCK);
        urchinUnderpass.addPaintableMaterial(Material.STONE_BRICKS);
        urchinUnderpass.addPaintableMaterial(Material.GRASS_BLOCK);
        urchinUnderpass.addPaintableMaterial(Material.OAK_PLANKS);
        urchinUnderpass.addPaintableMaterial(Material.DIRT);
        urchinUnderpass.addPaintableMaterial(Material.COARSE_DIRT);

        arenas.put(1, urchinUnderpass);
        XenyriaSplatoon.getXenyriaLogger().log("§e" + arenas.size() + " Arenen §7wurden registriert.");

    }

    public ArenaData getArenaData(int id) {

        return arenas.get(id);

    }

    public Iterable<? extends ArenaData> allArenas() {
        return arenas.values();
    }
}
