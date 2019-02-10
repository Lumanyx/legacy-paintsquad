package de.xenyria.splatoon.game.player.userdata.inventory;

import de.xenyria.api.spigot.ItemBuilder;
import org.bukkit.Material;

public class ItemRewriteRule {

    private Material material;
    private short durablity;
    public ItemRewriteRule(Material rewriteMaterial, short durability) {

        this.material = rewriteMaterial;
        this.durablity = durability;

    }

    public void addTags(ItemBuilder builder) {

        builder.addToNBT("mat_rwr", material.name()).addToNBT("mat_dmg", durablity);

    }

}
