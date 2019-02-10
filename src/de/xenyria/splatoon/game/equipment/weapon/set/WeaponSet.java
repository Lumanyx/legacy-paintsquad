package de.xenyria.splatoon.game.equipment.weapon.set;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WeaponSet {

    private int setID,primary,secondary,special;

    private String name;
    public String getName() { return name; }

    private int cost;
    public int getCost() { return cost; }

    public WeaponSet(int setID, String name, int cost, int primary, int secondary, int special) {

        this.setID = setID;
        this.name = name;
        this.cost = cost;
        this.primary = primary;
        this.secondary = secondary;
        this.special = special;

    }

    public int getSetID() { return setID; }
    public int getPrimaryWeapon() { return primary; }
    public int getSecondary() { return secondary; }
    public int getSpecial() { return special; }

    public ItemStack itemStackForPlayer(Player player, boolean shootingRange) {

        SplatoonPrimaryWeapon weapon = (SplatoonPrimaryWeapon) SplatoonWeaponRegistry.getDummy(primary);
        SplatoonSecondaryWeapon secondary = (SplatoonSecondaryWeapon) SplatoonWeaponRegistry.getDummy(getSecondary());
        SplatoonSpecialWeapon special = (SplatoonSpecialWeapon) SplatoonWeaponRegistry.getDummy(getSpecial());
        ItemBuilder builder = new ItemBuilder(weapon.getRepresentiveMaterial());

        if(weapon.getResourcepackOption() != null) {

            if(ResourcePackUtil.hasCustomResourcePack(player)) {

                builder.setDurability(weapon.getResourcepackOption().getDamageValue());

            }

        }
        builder.setUnbreakable(true).addAttributeHider();
        if(shootingRange) {

            builder.addToNBT("WeaponSetForShootingRange",  getSetID());

        } else {

            builder.addToNBT("ShowWeaponPurchaseScreen", getSetID());

        }
        builder.setDisplayName("§6§o§l" + getName());
        builder.addLore("");
        builder.addLore("§8§l> §e§lWaffenset");
        builder.addLore("§e" + weapon.getName());
        builder.addLore("§e" + secondary.getName());
        builder.addLore("§e" + special.getName());
        builder.addLore("");
        builder.addLore("§7Benötigte Punkte für", "§7die Spezialwaffe:", "§e§l" + special.getRequiredPoints() + " Punkte");
        builder.addLore("");
        builder.addLore("§8§l> §e§lPreis");
        builder.addLore("§6§l" + getCost() + " Taler");
        builder.addLore("");
        return builder.create();

    }

    public PrimaryWeaponType getPrimaryWeaponType() {

        return ((SplatoonPrimaryWeapon)SplatoonWeaponRegistry.getDummy(primary)).getPrimaryWeaponType();

    }

}
