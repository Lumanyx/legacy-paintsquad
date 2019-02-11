package de.xenyria.splatoon.lobby.shop.gear;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GearShopkeeper extends AbstractShopkeeper {

    public GearShopkeeper(Location location, String name) {
        super(location, name);
    }

    private static final String INFORMATION_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWillkommen im Shop!";
    private static final Inventory informationInventory = Bukkit.createInventory(null, 27, INFORMATION_TITLE);

    private static final ItemStack EFFECT_DETAILS = new ItemBuilder(Material.POTION).setDisplayName("§cEffekte").addLore("§7Hier kannst du jederzeit", "§7nachschauen welche Fähigkeit", "§7welche Vor- und Nachteile bringt.").create();

    static {

        for(int i = 0; i < 27; i++) {

            informationInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        informationInventory.setItem(9, EFFECT_DETAILS);

    }

    @Override
    public void onInteraction(Player player) {

        player.openInventory(informationInventory);

    }

}
