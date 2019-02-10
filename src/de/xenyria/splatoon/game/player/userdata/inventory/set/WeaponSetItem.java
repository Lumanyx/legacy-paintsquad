package de.xenyria.splatoon.game.player.userdata.inventory.set;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.InventoryItem;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemCategory;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemRewriteRule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class WeaponSetItem extends InventoryItem {

    public WeaponSetItem(SplatoonHumanPlayer player, Material material, short durability, @Nullable ItemRewriteRule rewriteRule, WeaponSet set) {

        super(player, material, durability, rewriteRule);
        this.set = set;

    }
    private WeaponSet set;
    public WeaponSet getSet() { return set; }

    public static WeaponSetItem fromSet(SplatoonHumanPlayer player, WeaponSet set, boolean equipped) {

        SplatoonPrimaryWeapon primaryWeapon = (SplatoonPrimaryWeapon) SplatoonWeaponRegistry.getDummy(set.getPrimaryWeapon());

        Material material = primaryWeapon.getRepresentiveMaterial();
        short durability = 0;
        if(ResourcePackUtil.hasCustomResourcePack(player.getPlayer())) {

            if(primaryWeapon.getResourcepackOption() != null) {

                durability = primaryWeapon.getResourcepackOption().getDamageValue();

            }

        }

        WeaponSetItem setItem = new WeaponSetItem(player, material, durability, null, set);
        setItem.equipFlag = equipped;

        return setItem;

    }

    private boolean equipFlag;
    public boolean isEquipped() { return equipFlag; }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.WEAPONS;
    }

    public static String EQUIP_ASK_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cDieses Set ausrüsten?";

    @Override
    public void onClick() {

        Inventory inventory = Bukkit.createInventory(null, 45, EQUIP_ASK_TITLE);
        for(int i = 0; i < 45; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        ItemStack stack = asItemStack();
        inventory.setItem(13, stack);
        inventory.setItem(29, new ItemBuilder(Material.BARRIER).setDisplayName("§cNein").addLore("§7Zurück zum Inventar").addToNBT("Dismiss", true).create());
        inventory.setItem(31, new ItemBuilder(Material.YELLOW_TERRACOTTA).setDisplayName("§eTesten").addLore("§7Teleportiert dich zum Testbereich.").addToNBT("EnterShootingRange", getLocalItemID()).create());
        inventory.setItem(33, new ItemBuilder(Material.EMERALD).addToNBT("EquipSet", getLocalItemID()).setDisplayName("§aAnlegen").addLore("§7Rüstet das Waffenset aus.").create());
        getPlayer().getPlayer().openInventory(inventory);

    }

    private int inkedTurf = 0;

    @Override
    public void handleItemBuild(ItemBuilder builder) {

        SplatoonPrimaryWeapon primaryWeapon = (SplatoonPrimaryWeapon) SplatoonWeaponRegistry.getDummy(set.getPrimaryWeapon());
        SplatoonSecondaryWeapon secondaryWeapon = (SplatoonSecondaryWeapon) SplatoonWeaponRegistry.getDummy(set.getSecondary());
        SplatoonSpecialWeapon specialWeapon = (SplatoonSpecialWeapon) SplatoonWeaponRegistry.getDummy(set.getSpecial());

        builder.setDisplayName("§6§l" + primaryWeapon.getName());
        builder.addLore("");
        builder.addToNBT("SetID", set.getSetID());
        builder.addLore("§8§l> §e§lWaffenset");
        builder.addLore("§e" + primaryWeapon.getName());
        builder.addLore("§e" + secondaryWeapon.getName());
        builder.addLore("§e" + specialWeapon.getName());
        builder.addLore("");
        builder.addLore("§7Benötigte Punkte für", "§7die Spezialwaffe:", "§e§l" + specialWeapon.getRequiredPoints() + " Punkte");
        builder.addLore("");
        builder.addLore("§7Eingefärbte Fläche");
        builder.addLore("§e" + inkedTurf + " Punkte");
        builder.addLore("");
        builder.addAttributeHider();
        builder.setUnbreakable(true);

        if(equipFlag) {

            builder.addLore("§2" + Characters.OKAY + " §aAngelegt");

        } else {

            builder.addLore("§4" + Characters.BIG_X + " §cNicht angelegt");

        }

    }
}
