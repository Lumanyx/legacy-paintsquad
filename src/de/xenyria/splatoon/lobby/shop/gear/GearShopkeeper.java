package de.xenyria.splatoon.lobby.shop.gear;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
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

    public static final String INFORMATION_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cWillkommen im Shop!";
    public static final Inventory informationInventory = Bukkit.createInventory(null, 9, INFORMATION_TITLE);

    public static final ItemStack EFFECT_DETAILS = new ItemBuilder(Material.POTION).setDisplayName("§cEffekte").addLore("§7Hier kannst du jederzeit", "§7nachschauen welche Fähigkeit", "§7welche Vor- und Nachteile bringt.").create();

    static {

        for(int i = 0; i < 9; i++) {

            informationInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        informationInventory.setItem(0, EFFECT_DETAILS);
        informationInventory.setItem(8, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Dismiss", true).create());

    }

    public static final String ABILITIES = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cFähigkeiten";

    public static void openAbilityInfo(Player player, boolean openedInShopInventory) {

        Inventory inventory = Bukkit.createInventory(null, 27, ABILITIES);
        int i = 0;
        for(SpecialEffect effect : SpecialEffect.values()) {

            inventory.setItem(i, effect.getBuilder().setDisplayName(effect.getName()).addLore(effect.getDescription().split("\n")).create());
            i++;

        }

        for(int x = 18; x < 27; x++) {

            inventory.setItem(x, ItemBuilder.getUnclickablePane());

        }
        if(openedInShopInventory) {

            inventory.setItem(26, new ItemBuilder(Material.BARRIER).setDisplayName("§cZurück").addToNBT("Back", true).create());

        }
        player.openInventory(inventory);

    }

    @Override
    public void onInteraction(Player player) {

        player.openInventory(informationInventory);

    }

    public Location getLocation() { return getVillager().getLocation(); }

}
