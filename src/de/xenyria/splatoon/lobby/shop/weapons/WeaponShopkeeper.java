package de.xenyria.splatoon.lobby.shop.weapons;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WeaponShopkeeper extends AbstractShopkeeper {

    public WeaponShopkeeper(Location location) {
        super(location, "§2§lWaffenshop");
    }

    @Override
    public void onInteraction(Player player) {

        player.openInventory(XenyriaSplatoon.getLobbyManager().getLobby().getWeaponShop().getOverviewInventory());

    }
}
