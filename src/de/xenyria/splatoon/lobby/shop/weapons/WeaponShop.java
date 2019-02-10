package de.xenyria.splatoon.lobby.shop.weapons;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import de.xenyria.splatoon.lobby.shop.AbstractShop;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WeaponShop extends AbstractShop {

    public WeaponShop(AbstractShopkeeper shopkeeper, Location... locations) {

        super(shopkeeper, ShopType.WEAPONS, locations);

        int x = 0;
        for(PrimaryWeaponType type : PrimaryWeaponType.values()) {

            if (type != PrimaryWeaponType.MODE_EXCLUSIVE) {

                ItemBuilder builder = new ItemBuilder(type.getRepresentiveMaterial()).addAttributeHider();

                builder.setDisplayName("§2§l" + type.getName());

                if(type.getDurability() != 0) {

                    builder.setDurability(type.getDurability());
                    builder.setUnbreakable(true);

                }
                builder.addLore("");
                builder.addLore(type.getDescription().split("\n"));
                builder.addLore("");
                builder.addToNBT("ShowWeaponCategory", type.name());

                categoryOverview.setItem(x, builder.create());
                x++;

            }

        }

    }

    public static final String CATEGORY_OVERVIEW = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §2§lWelche Waffenart?";
    public static final String WEAPON_SETS = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §2§lWelches Set?";
    public static final ItemStack BACK_TO_OVERVIEW = new ItemBuilder(Material.BARRIER).setDisplayName("§8§l> §c§lZur Waffenartauswahl").create();

    private Inventory categoryOverview = Bukkit.createInventory(null, 9, CATEGORY_OVERVIEW);

    public void showAllWeapons(Player player, PrimaryWeaponType category, boolean shootingRange) {

        Inventory inventory = Bukkit.createInventory(null, 54, WEAPON_SETS);
        int x = 0;
        for(WeaponSet set : WeaponSetRegistry.getSets(category)) {

            ItemStack stack = set.itemStackForPlayer(player, shootingRange);
            inventory.setItem(x, stack);
            x++;

        }

        for(int i = 45; i < 54 ; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        inventory.setItem(45, BACK_TO_OVERVIEW);
        player.openInventory(inventory);
    }

    public Inventory getOverviewInventory() {
        return categoryOverview;
    }

    public static final String PURCHASE_SET = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cSet kaufen?";
    public static final String PURCHASED_EQUIP_NOW = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cSet gekauft - Ausrüsten?";

    public void showPurchaseScreen(SplatoonHumanPlayer player, int setID) {

        boolean alreadyPurchased = player.getInventory().hasSetInInventory(setID);

        WeaponSet set = WeaponSetRegistry.getSet(setID);

        Inventory inventory = Bukkit.createInventory(null, 36);
        for(int i = 0; i < 36; i++) { inventory.setItem(i, ItemBuilder.getUnclickablePane()); }
        inventory.setItem(14, set.itemStackForPlayer(player.getPlayer(), false));
        inventory.setItem(29, new ItemBuilder(Material.BARRIER).setDisplayName("§cNein").addLore("§7Zurück zum Shop").addToNBT("DismissCategory", set.getPrimaryWeaponType().name()).create());
        inventory.setItem(31, new ItemBuilder(Material.YELLOW_TERRACOTTA).setDisplayName("§eTesten").addLore("§7Teleportiert dich zum Testbereich.").addToNBT("EnterShootingRangeAbs", set.getSetID()).create());

        int setID1 = set.getSetID();
        if(!alreadyPurchased) {

            inventory.setItem(33, new ItemBuilder(Material.EMERALD).addToNBT("PurchaseSet", setID1).setDisplayName("§aKaufen").addLore("§7Fügt das Waffenset zu\n§7deinem Inventar hinzu.").create());

        } else {

            inventory.setItem(33, new ItemBuilder(Material.LIME_TERRACOTTA).setDisplayName("§aAnlegen").addLore("§7§o§lBereits gekauft!", "", "§7Legt das Waffenset an.").addToNBT("EquipSet", player.getInventory().getLocalSetID(set.getSetID())).create());

        }
        player.getPlayer().openInventory(inventory);


    }

}
