package de.xenyria.splatoon.arena.placeholder;

import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.builder.ArenaBuilder;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;

public interface ArenaPlaceholder {

    Material getTriggeringMaterial();
    Material getReplacement();
    boolean handleFlagData();
    void addFlags(ArenaProvider.ArenaGenerationTask.FlagData flag);

    public class Metadata {

        public String key;
        public FixedMetadataValue value;
        public Metadata(String key, FixedMetadataValue value) {

            this.key = key;
            this.value = value;

        }

    }

}
