package de.xenyria.splatoon.arena.placeholder;

import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;

public class StoredTeamPlaceholder {

    private Material material;
    public Material getMaterial() { return material; }

    private int teamID;
    public int getTeamID() { return teamID; }

    public StoredTeamPlaceholder(Material material, ReplacementType type, int teamID) {

        this.material = material;
        this.type = type;
        this.teamID = teamID;

    }

    private ReplacementType type;
    public enum ReplacementType {

        CARPET,
        STAINED_CLAY,
        WOOL;

    }

    public ArenaPlaceholder toPlaceholder(Match match) {

        return new ArenaPlaceholder() {
            @Override
            public Material getTriggeringMaterial() {

                return material;

            }

            @Override
            public Material getReplacement() {

                Team team = match.getRegisteredTeams().get(teamID);
                if(type == ReplacementType.CARPET) {

                    return team.getColor().getCarpet();

                } else if(type == ReplacementType.STAINED_CLAY) {

                    return team.getColor().getClay();

                } else if(type == ReplacementType.WOOL) {

                    return team.getColor().getWool();

                }
                return null;

            }

            public boolean addMetadata() { return false; }
            public Metadata getMetadata() { return null; }

        };

    }

}
