package de.xenyria.splatoon.lobby.shop.item;

import de.xenyria.splatoon.XenyriaSplatoon;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public abstract class ShopItem {

    private static int lastItemID;
    public static int getNextItemID() {

        lastItemID++;
        return lastItemID;

    }

    private static ArrayList<ShopItem> items = new ArrayList<>();
    public static ArrayList<ShopItem> getItems() { return items; }

    private int shopItemID;
    public int getShopItemID() { return shopItemID; }

    public ShopItem(String name, String desc, int price, Location spawnLocation) {

        shopItemID = getNextItemID();
        this.nameStr = name;
        this.price = price;
        item = (Item) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.DROPPED_ITEM);
        item.setVelocity(new Vector());
        item.setGravity(false);
        item.setCanMobPickup(false);
        item.setPickupDelay(Integer.MAX_VALUE);

        World world = spawnLocation.getWorld();
        Location priceTagLocation = spawnLocation.clone();
        priceTagLocation = priceTagLocation.add(0, .75, 0);
        priceTag = (ArmorStand) world.spawnEntity(priceTagLocation, EntityType.ARMOR_STAND);
        priceTag.setVisible(false);
        priceTag.setCustomNameVisible(true);
        priceTag.setCustomName("§e§l" + price + " Taler");
        priceTag.setCanTick(false);
        priceTag.setCanMove(false);

        Location descTagLocation = priceTagLocation.clone().add(0, .25, 0);
        descTag = (ArmorStand) world.spawnEntity(descTagLocation, EntityType.ARMOR_STAND);
        descTag.setVisible(false);
        descTag.setCustomNameVisible(true);
        descTag.setCustomName(desc);
        descTag.setCanMove(false);
        descTag.setCanTick(false);

        Location nameTagLocation = descTagLocation.clone().add(0, .25, 0);
        nameTag = (ArmorStand) world.spawnEntity(nameTagLocation, EntityType.ARMOR_STAND);
        nameTag.setVisible(false);
        nameTag.setCustomNameVisible(true);
        nameTag.setCustomName(nameStr);
        nameTag.setCanTick(false);
        nameTag.setCanMove(false);

        nameTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        descTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        priceTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        items.add(this);

    }

    public void remove() {

        priceTag.remove();
        descTag.remove();
        nameTag.remove();
        item.remove();
        items.remove(this);

    }

    public abstract void onRemove();
    public abstract void onInteract(Player player);

    private String nameStr;
    private int price;
    private Item item;
    private ArmorStand priceTag,descTag,nameTag;

}
