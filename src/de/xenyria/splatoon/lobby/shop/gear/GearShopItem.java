package de.xenyria.splatoon.lobby.shop.gear;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.boots.LegacyFootGear;
import de.xenyria.splatoon.game.equipment.gear.chest.LegacyBodyGear;
import de.xenyria.splatoon.game.equipment.gear.head.LegacyHeadGear;
import de.xenyria.splatoon.lobby.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GearShopItem extends ShopItem {

    private static final String BUY_EQUIPMENT_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + "§6Ausrüstung kaufen?";
    public GearShopItem(Gear gear, String name, String desc, int price, Location spawnLocation) {

        super(name, desc, price, spawnLocation);

        buyInventory = Bukkit.createInventory(null, 36, BUY_EQUIPMENT_TITLE);
        for(int i = 0; i < 36; i++) {

            buyInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        dummy = gear;
        buyInventory.setItem(13, gear.asItemStack(null));
        boolean generic = gear instanceof LegacyHeadGear || gear instanceof LegacyBodyGear || gear instanceof LegacyFootGear;
        buyInventory.setItem(29, new ItemBuilder(Material.BARRIER).addToNBT("ExitInventory", true).setDisplayName("§cLieber nicht").create());
        buyInventory.setItem(33, new ItemBuilder(Material.EMERALD).addToNBT("PurchaseItem", dummy.getOriginID()).addToNBT("Generic", generic).setDisplayName("§aKaufen").create());

    }
    private Gear dummy;

    private Inventory buyInventory;

    @Override
    public void onRemove() {

        if(buyInventory != null) {

            buyInventory = null;

        }

    }

    public static GearShopItem fromGear(Gear gear, Location location) {

        String name = colorPrefix(gear.getMaxSubAbilities()) + gear.getName();
        String desc = getDescription(gear);

        return new GearShopItem(gear, name, desc, gear.getPrice(), location);

    }

    private static String colorPrefix(int abilityCount) {

        switch (abilityCount) {

            case 1: return "§8";
            case 2: return "§e";
            case 3: return "§6§l";

        }
        return "";
    }
    private static String getDescription(Gear gear) {

        String desc = "§e";

        for(int i = 0; i < gear.getMaxSubAbilities(); i++) {

            desc+=Characters.STAR;

        }

        desc+=" §8| §r" + gear.getMainAbility().getShortName() + " §8| §r" + gear.getBrand().getDisplayName();
        return desc;

    }

    @Override
    public void onInteract(Player player) {

    }
}
