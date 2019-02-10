package de.xenyria.splatoon.arena;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ArenaCategory {

    INTERNAL("INTERNAL", "", Material.AIR),
    REPLICA("§aReplik", "§7Nachbauten aus dem Originalspiel.\n§7Unterstützt werden Spiele bis\n§e• 4 vs. 4", Material.PAPER);

    private String name;
    public String getName() {

        return name;

    }

    private Material material;
    private String description;

    ArenaCategory(String name, String description, Material material) {

        this.name = name;
        this.material = material;
        this.description = description;

    }

    public ItemStack getItemStack() {

        return new ItemBuilder(material).setDisplayName("§8" + Characters.ARROW_RIGHT_FROM_TOP + " " + name).addLore(description.split("\n")).addToNBT("ArenaCategory", name()).create();

    }

}
