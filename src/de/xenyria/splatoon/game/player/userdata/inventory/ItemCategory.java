package de.xenyria.splatoon.game.player.userdata.inventory;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ItemCategory {

    WEAPONS(Material.STONE_HOE, "§cWaffensets", "§7Hier findest du deine\n§7erworbenen Waffensets.", 36 * 3),
    HELMETS(Material.LEATHER_HELMET, "§6Helme", "§7Hier findest du deine\n§7erworbenen Helme.", 36),
    CHESTPLATES(Material.LEATHER_CHESTPLATE, "§eBrustpanzer", "§7Hier findest du deine\n§7erworbenen Brustpanzer.", 36),
    BOOTS(Material.LEATHER_BOOTS, "§aStiefel", "§7Hier findest du deine\n§7erworbenen Stiefel.", 36),
    CONSUMABLES(Material.POTION, "§bSnacks & Drinks", "§7Hier findest du deine\n§7erworbenen Snacks und Drinks.", 36),
    GADGETS(Material.HOPPER, "§3Gadgets", "§7Hier findest du deine\n§7erworbenen Gadgets für die Lobby.", 18);

    private Material material;
    private String name;
    private String description;

    private int slots;
    public int getSlots() { return slots; }

    public ItemBuilder getItemBuilder() {

        return new ItemBuilder(material).setDisplayName("§8" + Characters.ARROW_RIGHT_FROM_TOP + " " + name).addLore(description.split("\n")).addToNBT("SelectCategory", name());

    }

    ItemCategory(Material material, String name, String description, int slots) {

        this.slots = slots;
        this.material = material;
        this.name = name;
        this.description = description;

    }

}
