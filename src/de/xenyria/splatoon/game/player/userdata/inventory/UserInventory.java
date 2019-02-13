package de.xenyria.splatoon.game.player.userdata.inventory;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.gear.GearItem;
import de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserInventory {

    public static final String INVENTORY_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §aDein Inventar";

    private SplatoonHumanPlayer player;
    public UserInventory(SplatoonHumanPlayer player) {

        this.player = player;

    }

    public boolean hasSpace(ItemCategory category) {

        int max = category.getSlots();
        int current = items.getOrDefault(category, new ArrayList<>()).size();
        if((current+1) >= max) { return false; }
        return true;

    }

    public InventoryCoordinate coordinatesFromSlot(int slot) {

        int y = slot / 9;
        return new InventoryCoordinate(slot % 9, y);

    }

    private ItemCategory currentCategory = ItemCategory.WEAPONS, transitionCategory;
    private int currentPage, transitionPage;

    public boolean isOpen() {

        return !inventory.getViewers().isEmpty();

    }

    public boolean isAnimationActive() { return animationStarted; }

    public ItemCategory getCurrentCategory() { return currentCategory; }

    public void onItemClick(int itemID) {

        ArrayList<InventoryItem> invItems = items.getOrDefault(currentCategory, new ArrayList<>());
        for(InventoryItem item : invItems) {

            if(item.getLocalItemID() == itemID) {

                item.onClick();

            }

        }

    }

    public void equipSet(int localItemID) {

        for(InventoryItem item : items.getOrDefault(ItemCategory.WEAPONS, new ArrayList<>())) {

            if(item instanceof WeaponSetItem) {

                WeaponSetItem item1 = (WeaponSetItem)item;
                item1.setEquipped(localItemID == item.getLocalItemID());
                item1.buildItem();

            }

        }

        if(player.isValid() && player.getMatch() != null && player.getMatch() instanceof BattleMatch) {

            BattleMatch match = (BattleMatch) player.getMatch();
            if(match.isLobbyPhase()) {

                for(SplatoonHumanPlayer player : match.getPlayerLobbyPool()) {

                    match.queuePlayerInventoryUpdate(player);

                }

            }

        }

    }

    public boolean hasSetInInventory(int setID) {

        for(InventoryItem item : items.getOrDefault(ItemCategory.WEAPONS, new ArrayList<>())) {

            if(item instanceof WeaponSetItem) {

                WeaponSetItem item1 = (WeaponSetItem)item;
                if(item1.getSet().getSetID() == setID){

                    return true;

                }

            }

        }
        return false;

    }

    public int getLocalSetID(int setID) {

        for(InventoryItem item : items.getOrDefault(ItemCategory.WEAPONS, new ArrayList<>())) {

            if(item instanceof WeaponSetItem) {

                WeaponSetItem setItem = (WeaponSetItem)item;
                if(setItem.getSet().getSetID() == setID) {

                    return setItem.getLocalItemID();

                }

            }

        }
        return 1;

    }

    public InventoryItem getItemFromLocalID(int id) {

        for(Map.Entry<ItemCategory, ArrayList<InventoryItem>> entry : items.entrySet()) {

            for(InventoryItem item : entry.getValue()) {

                if(item.getLocalItemID() == id) {

                    return item;

                }

            }

        }

        return null;

    }

    public int itemCount(ItemCategory category) {

        if(items.containsKey(category)) {

            return items.get(category).size();

        }
        return 0;

    }

    public void removeItem(InventoryItem item) {

        ItemCategory category = item.getCategory();
        items.get(category).remove(item);

    }
    public void removeItem(int id) {

        InventoryItem item = getItemFromLocalID(id);
        if(item != null) { removeItem(item); }

    }

    public void equipItem(InventoryItem item) {

        for(InventoryItem item1 : items.getOrDefault(item.getCategory(), new ArrayList<>())) {

            if(item1.isEquipped()) {

                if(item1.getLocalItemID() != item.getLocalItemID()) {

                    item1.setEquipped(false);
                    item1.buildItem();

                } else { return; }

            } else {

                if(item1.getLocalItemID() == item.getLocalItemID()) {

                    item1.setEquipped(true);
                    item1.buildItem();

                }

            }

        }

        if(item instanceof GearItem) {

            GearItem item1 = (GearItem)item;
            Gear gear = item1.getGearInstance();
            switch (gear.getType()) {

                case HELMET: player.getEquipment().setHeadGear(gear); break;
                case CHESTPLATE: player.getEquipment().setBodyGear(gear); break;
                case BOOTS: player.getEquipment().setFootGear(gear); break;
            }

        }

    }

    public WeaponSetItem getEquippedSet() {

        for(InventoryItem item : items.get(ItemCategory.WEAPONS)) {

            if(item instanceof WeaponSetItem) {

                WeaponSetItem item1 = (WeaponSetItem) item;
                if(item1.isEquipped()) {

                    return item1;

                }

            }

        }

        return (WeaponSetItem) items.get(ItemCategory.WEAPONS).get(0);

    }


    public class InventoryCoordinate {

        public InventoryCoordinate(int x, int y) {

            this.x=x;this.y=y;

        }

        public final int x,y;

        @Override
        public int hashCode() { return new HashCodeBuilder().append(x).append(y).toHashCode(); }

        @Override
        public boolean equals(Object obj) { return x == ((InventoryCoordinate)obj).x && y == ((InventoryCoordinate)obj).y; }
    }

    private HashMap<InventoryCoordinate, ItemStack> itemGrid = new HashMap<>();

    public void open() {

        setCategoryBar();
        setItemsInstantly();
        player.getPlayer().openInventory(inventory);
        player.getPlayer().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);

    }

    public void setItemsInstantly() {

        ItemCategory targetCategory = currentCategory;
        if(transitionCategory != null) {

            targetCategory = transitionCategory;
            transitionCategory = null;

        }
        currentPage = transitionPage;
        ArrayList<ItemStack> toSet = getAllItemsForCategory(targetCategory, currentPage);
        transitionPage = 0;
        shifts = 0;
        itemGrid.clear();
        int i = 0;
        for(ItemStack stack : toSet) {

            if(stack != null) {

                inventory.setItem(i+9,stack);

            }
            i++;

        }

    }

    public void setCategoryBar() {

        int x = 1;
        for(int i = 0; i < 9; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());
            inventory.setItem(i+45, ItemBuilder.getUnclickablePane());

        }
        for(ItemCategory category : ItemCategory.values()) {

            ItemBuilder builder = category.getItemBuilder().addAttributeHider();
            builder.addLore("");
            int occupied = items.getOrDefault(category, new ArrayList<>()).size();
            builder.addLore("§8§l> §e" + occupied + "§6/§e" + category.getSlots() + " Slots besetzt");
            if(currentCategory == category) {

                builder.addEnchantment(Enchantment.DURABILITY, 1).addAttributeHider();

            }
            ItemStack stack = builder.create();
            inventory.setItem(x, stack);

            x++;

        }

        int categoryMaxSlots = currentCategory.getSlots();
        int currentMaxSlotCount = (currentPage * PAGE_SIZE) + PAGE_SIZE;
        if(currentMaxSlotCount < categoryMaxSlots) {

            inventory.setItem(52, new ItemBuilder(Material.PAPER).setDisplayName("§8§l> §e§lWeiter").addLore("§7Zu §eSeite " + (currentPage+1)).addToNBT("ToPage", currentPage+1).create());

        }
        if(currentPage > 0) {

            inventory.setItem(47, new ItemBuilder(Material.PAPER).setDisplayName("§8§l< §e§lZurück").addLore("§7Zu §eSeite " + (currentPage)).addToNBT("ToPage", currentPage-1).create());

        }

    }

    public void changeCategory(ItemCategory targetCategory, int page) {

        // Animation: Snapshot aller derzeit platzierten Items und die, die folgen werden
        int copyIndxBegin = 9;
        int copyIndxEnd = 9 + PAGE_SIZE;

        for(int x = copyIndxBegin; x < copyIndxEnd; x++) {

            ItemStack stack = inventory.getItem(x);
            if(stack != null && stack.getType() != Material.AIR) {

                InventoryCoordinate coordinate = coordinatesFromSlot(x);
                itemGrid.put(coordinate, stack);

            }

        }

        // Zielitems
        ArrayList<ItemStack> targetItems = getAllItemsForCategory(targetCategory, page);
        int newIndex = 0;
        for (ItemStack stack : targetItems) {

            if(stack != null && stack.getType() != Material.AIR) {

                int x = 10+(newIndex % 9);
                int y = newIndex / 9;
                y++;

                InventoryCoordinate coordinate = new InventoryCoordinate(x,y);
                itemGrid.put(coordinate, stack);

            }
            newIndex++;

        }
        animationStarted = true;
        transitionCategory = targetCategory;
        transitionPage = page;

    }

    public boolean animationDone() {

        return shifts > 10;

    }

    private boolean animationStarted = false;

    private int ticks;
    private int shifts = 0;
    public void tick() {

        if(isOpen()) {

            tickAnimation();

        } else {

            if(animationStarted) {

                animationStarted = false;
                currentCategory = transitionCategory;
                currentPage = transitionPage;
                itemGrid.clear();

            }

        }

    }

    private void tickAnimation() {

        if(animationStarted) {

            Iterator<Map.Entry<InventoryCoordinate, ItemStack>> iterator = itemGrid.entrySet().iterator();

            for (int i = 9; i < (9 + PAGE_SIZE); i++) {

                ItemStack stack = inventory.getItem(i);
                if (stack != null && stack.getType() != Material.AIR) {

                    inventory.setItem(i, new ItemStack(Material.AIR));

                }

            }

            while (iterator.hasNext()) {

                Map.Entry<InventoryCoordinate, ItemStack> entry = iterator.next();
                if (entry.getKey().x < shifts) {

                    iterator.remove();

                } else {

                    if (entry.getKey().x <= (shifts + 8)) {

                        int localX = (entry.getKey().x) - shifts;
                        int slot = localX + (entry.getKey().y * 9);
                        if(slot <= (9+PAGE_SIZE)) {

                            inventory.setItem(slot, entry.getValue());

                        }

                    }

                }

            }
            shifts++;

            if (animationDone()) {

                animationStarted = false;
                shifts = 0;
                currentPage = transitionPage;
                currentCategory = transitionCategory;
                transitionCategory = null;

                setItemsInstantly();
                setCategoryBar();

            }

        }

    }

    private HashMap<ItemCategory, ArrayList<InventoryItem>> items = new HashMap<>();
    private int lastItemID = 1;
    public int addItem(InventoryItem item) {

        int id = lastItemID;
        lastItemID++;

        if(!items.containsKey(item.getCategory())) {

            items.put(item.getCategory(), new ArrayList<>());

        }
        ArrayList<InventoryItem> itemList = items.get(item.getCategory());
        itemList.add(item);
        return id;

    }

    private static final int PAGE_SIZE = 36;
    public ArrayList<ItemStack> getAllItemsForCategory(ItemCategory category, int page) {

        ArrayList<InventoryItem> inventoryItems = items.getOrDefault(category, new ArrayList<>());
        ArrayList<ItemStack> stacks = new ArrayList<>();
        int minIndx = (page * PAGE_SIZE);
        int maxIndx = minIndx+PAGE_SIZE;
        int maxIndexList = (inventoryItems.size() - 1);
        for(int x = minIndx; x < maxIndx; x++) {

            if(x <= maxIndexList) {

                InventoryItem item = inventoryItems.get(x);
                stacks.add(item.asItemStack());

                //InventoryItem item = inventoryItems.get(x);
                //stacks.add(item.asItemStack());

            } else {

                if(x >= category.getSlots()) {

                    stacks.add(OCCUPIED);

                } else {

                    stacks.add(new ItemStack(Material.AIR));

                }

            }

        }

        return stacks;

    }

    private static ItemStack OCCUPIED = new ItemBuilder(Material.BARRIER).setDisplayName("§4" + Characters.BIG_X).create();

    private Inventory inventory = Bukkit.createInventory(null, 54, INVENTORY_TITLE);

}
