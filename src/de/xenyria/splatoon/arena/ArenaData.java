package de.xenyria.splatoon.arena;

import de.xenyria.splatoon.arena.placeholder.StoredTeamPlaceholder;
import de.xenyria.splatoon.game.map.PaintDefinition;
import org.bukkit.Material;

import java.util.ArrayList;

public class ArenaData {

    private int id;
    public int getID() { return id; }

    private String arenaName;
    public String getArenaName() { return arenaName; }

    private GamemodeSchematicMap map;
    public GamemodeSchematicMap getMap() { return map; }

    private ArenaCategory category;
    public ArenaCategory getCategory() { return category; }

    public ArenaData(int id, Material material, String arenaName, ArenaCategory category, GamemodeSchematicMap map) {

        this.id = id;
        this.material = material;
        this.category = category;
        this.arenaName = arenaName;
        this.map = map;

    }

    private Material material;

    private ArrayList<StoredTeamPlaceholder> placeholders = new ArrayList<>();
    public ArrayList<StoredTeamPlaceholder> getPlaceholders() { return placeholders; }

    private int maxTeams, maxPlayersPerTeam;
    public int getMaxTeams() { return maxTeams; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxTeams(int i) {

        this.maxTeams = i;

    }
    public void setMaxPlayersPerTeam(int i) {

        this.maxPlayersPerTeam = i;

    }

    private PaintDefinition paintDefinition = new PaintDefinition();
    public void addPaintableMaterial(Material material) {

        paintDefinition.getPaintableMaterials().add(material);

    }

    public boolean isPaintable(Material type) {

        return paintDefinition.canPaint(type);

    }

    public Material getRepresentiveMaterial() { return material; }

}
