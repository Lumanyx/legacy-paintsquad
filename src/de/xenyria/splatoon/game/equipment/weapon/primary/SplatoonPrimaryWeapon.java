package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.weapon.BrandedEquipment;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import org.bukkit.inventory.ItemStack;

public abstract class SplatoonPrimaryWeapon extends SplatoonWeapon implements BrandedEquipment {

    public SplatoonPrimaryWeapon(int id, String name) {
        super(id, name);
    }
    public abstract PrimaryWeaponType getPrimaryWeaponType();

    public ItemStack asItemStack() {

        ItemBuilder builder = new ItemBuilder(getRepresentiveMaterial()).setDisplayName(getBrand().getDisplayName() + getName());
        builder.addLore("§8§l> Waffentyp", getPrimaryWeaponType().getName());
        builder.addLore("§0");
        builder.addLore(getPrimaryWeaponType().getDescription().split("\n"));
        builder.addLore("§0");
        builder.addLore("§8§l> Marke");
        builder.addLore(getBrand().getDisplayName());
        builder.addToNBT("WeaponID", getID());
        builder.setUnbreakable(true);

        ItemStack stack = builder.create();
        if(getResourcepackOption() != null) {

            stack.setDurability(getResourcepackOption().getDamageValue());
            return stack;

        }

        return builder.create();

    }

}
