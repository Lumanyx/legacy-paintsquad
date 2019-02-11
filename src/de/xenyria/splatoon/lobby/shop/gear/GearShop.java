package de.xenyria.splatoon.lobby.shop.gear;

import de.xenyria.splatoon.game.player.userdata.inventory.gear.GearItem;
import de.xenyria.splatoon.lobby.shop.AbstractShop;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import org.bukkit.Location;

public class GearShop extends AbstractShop {

    public GearShop(AbstractShopkeeper shopkeeper, ShopType type, Location... locations) {

        super(shopkeeper, type, locations);
        initializeSortiment();
        getSortiment().generateSortiment();

    }
}
