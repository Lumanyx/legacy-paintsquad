package de.xenyria.splatoon.arena.placeholder;

import de.xenyria.splatoon.game.color.Color;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;

public interface ArenaPlaceholder {

    Material getTriggeringMaterial();
    Material getReplacement();
    boolean addMetadata();
    Metadata getMetadata();

    public class Metadata {

        public String key;
        public FixedMetadataValue value;
        public Metadata(String key, FixedMetadataValue value) {

            this.key = key;
            this.value = value;

        }

    }

}
