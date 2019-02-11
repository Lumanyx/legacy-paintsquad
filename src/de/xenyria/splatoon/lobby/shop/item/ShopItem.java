package de.xenyria.splatoon.lobby.shop.item;

import de.xenyria.splatoon.XenyriaSplatoon;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.EntityItem;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ShopItem {

    private static int lastItemID;
    public static int getNextItemID() {

        lastItemID++;
        return lastItemID;

    }

    private static ArrayList<ShopItem> items = new ArrayList<>();
    public static ArrayList<ShopItem> getItems() { return items; }

    private static ConcurrentHashMap<Integer, EntityItem> shopItemEntityIDs = new ConcurrentHashMap<Integer, EntityItem>();
    public static ConcurrentHashMap<Integer, EntityItem> getShopItemEntityIDs() { return shopItemEntityIDs; }

    private int shopItemID;
    public int getShopItemID() { return shopItemID; }

    public ShopItem(String name, String desc, String desc2, int price, Location spawnLocation) {

        shopItemID = getNextItemID();
        this.nameStr = name;
        this.price = price;
        item = (Item) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.DROPPED_ITEM);
        item.setVelocity(new Vector());
        item.setGravity(false);
        item.setCanMobPickup(false);
        item.setPickupDelay(Integer.MAX_VALUE);

        World world = spawnLocation.getWorld();
        Location priceTagLocation = spawnLocation.clone().subtract(0, .75, 0);
        priceTag = (ArmorStand) world.spawnEntity(priceTagLocation, EntityType.ARMOR_STAND);
        priceTag.setVisible(false);
        priceTag.setCustomNameVisible(false);
        priceTag.setCustomName("§e§l" + price + " Taler");
        //priceTag.setCanTick(false);
        priceTag.setCanMove(false);

        Location descTagLocation = priceTagLocation.clone().add(0, .25, 0);
        descTag = (ArmorStand) world.spawnEntity(descTagLocation, EntityType.ARMOR_STAND);
        descTag.setVisible(false);
        descTag.setCustomNameVisible(false);
        descTag.setCustomName(desc);
        descTag.setCanMove(false);
        //descTag.setCanTick(false);

        Location desc2TagLocation = descTagLocation.clone().add(0, .25, 0);
        desc2Tag = (ArmorStand) world.spawnEntity(desc2TagLocation, EntityType.ARMOR_STAND);
        desc2Tag.setVisible(false);
        desc2Tag.setCustomNameVisible(false);
        desc2Tag.setCustomName(desc2);
        desc2Tag.setCanMove(false);
        //desc2Tag.setCanTick(false);

        Location nameTagLocation = desc2TagLocation.clone().add(0, .25, 0);
        nameTag = (ArmorStand) world.spawnEntity(nameTagLocation, EntityType.ARMOR_STAND);
        nameTag.setVisible(false);
        nameTag.setCustomNameVisible(false);
        nameTag.setCustomName(nameStr);
        //nameTag.setCanTick(false);
        nameTag.setCanMove(false);

        nameTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        descTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        desc2Tag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        priceTag.setMetadata("ShopItemID", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), shopItemID));
        items.add(this);
        getShopItemEntityIDs().put(item.getEntityId(), ((EntityItem)((CraftItem)item).getHandle()));

    }

    public Item getItem() { return item; }

    public void setItem(ItemStack stack) {

        item.setItemStack(stack);

    }

    public ArrayList<EntityArmorStand> getTags() {

        ArrayList<EntityArmorStand> stands = new ArrayList<>();
        stands.add(((CraftArmorStand)nameTag).getHandle());
        stands.add(((CraftArmorStand)descTag).getHandle());
        stands.add(((CraftArmorStand)desc2Tag).getHandle());
        stands.add(((CraftArmorStand)priceTag).getHandle());
        return stands;

    }

    public void remove() {

        priceTag.remove();
        descTag.remove();
        nameTag.remove();
        item.remove();
        items.remove(this);
        getShopItemEntityIDs().remove(getItem().getEntityId());

    }

    public abstract void onRemove();
    public abstract void onInteract(Player player);

    private String nameStr;
    private int price;
    private Item item;
    private ArmorStand priceTag,descTag,desc2Tag,nameTag;

}
