package de.xenyria.splatoon.game.player.userdata.inventory.gear;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearData;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.InventoryItem;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemCategory;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemRewriteRule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class GearItem extends InventoryItem {

    public static class StoredGearData {

        private SpecialEffect effect;

        public static StoredGearData fromBase(int i) {

            Gear gear = Gear.dummyInstance(i);
            StoredGearData data = new StoredGearData(gear.getDefaultEffect(), 0);
            return data;

        }

        public SpecialEffect getEffect() { return effect; }

        private SpecialEffect[] subEffects;
        public SpecialEffect[] getSubEffects() { return subEffects; }

        private int experience;
        public int getExperience() { return experience; }

        public StoredGearData(SpecialEffect primaryEffect, int experience, SpecialEffect... effects) {

            this.effect = primaryEffect;
            this.experience = experience;
            this.subEffects = effects;

        }

    }

    public GearItem(SplatoonHumanPlayer player, Material material, int gearID, StoredGearData data) {

        super(player, material, (short)0, null);
        this.originID = gearID;

        gearInstance = Gear.newInstance(gearID);
        gearInstance.assignToPlayer(player);
        gearInstance.addExistingData(data);
        gearType = gearInstance.getType();

    }

    private Gear gearInstance = null;
    public Gear getGearInstance() { return gearInstance; }

    public static GearItem createItem(SplatoonHumanPlayer player, int originID, StoredGearData data, boolean equipped) {

        Gear gear = Gear.dummyInstance(originID);

        GearItem item = new GearItem(player, gear.getMaterial(), originID, data);
        item.addToPlayerInventory();
        item.setEquipped(equipped);
        item.buildItem();
        return item;

    }

    private GearType gearType;
    private int originID;

    @Override
    public ItemCategory getCategory() {

        switch (gearType) {

            case HELMET: return ItemCategory.HELMETS;
            case CHESTPLATE: return ItemCategory.CHESTPLATES;
            case BOOTS: return ItemCategory.BOOTS;
            default: return null;

        }

    }

    public static final String EQUIP_ASK_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cAusrüstung anlegen?";

    @Override
    public void onClick() {

        Inventory inventory = Bukkit.createInventory(null, 45, EQUIP_ASK_TITLE);
        for(int i = 0; i < 45; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        ItemStack stack = asItemStack();
        inventory.setItem(13, stack);
        inventory.setItem(31, new ItemBuilder(Material.BARRIER).setDisplayName("§cNein").addLore("§7Zurück zum Inventar").addToNBT("Dismiss", true).create());
        inventory.setItem(29, new ItemBuilder(Material.HOPPER).setDisplayName("§4Wegwerfen").addLore("§7Entfernt den Gegenstand aus", "§7deinem Inventar.", "", "§4Kann nicht rückgängig gemacht werden!").addToNBT("Remove", getLocalItemID()).create());
        inventory.setItem(33, new ItemBuilder(Material.EMERALD).addToNBT("EquipGear", getLocalItemID()).setDisplayName("§aAnlegen").addLore("§7Rüstet den Gegenstand aus.").create());
        getPlayer().getPlayer().openInventory(inventory);

    }

    @Override
    public void handleItemBuild(ItemBuilder builder) {

        builder.setDisplayName(gearInstance.getName());
        gearInstance.handleItemBuilder(builder, null);
        builder.addAttributeHider();
        builder.setUnbreakable(true);
        builder.addLore("");
        if(isEquipped()) {

            builder.addEnchantment(Enchantment.DURABILITY, 1);
            builder.addLore("§2" + Characters.OKAY + " §aAngelegt");

        } else {

            builder.addLore("§4" + Characters.BIG_X + " §cNicht angelegt");

        }


    }
}
