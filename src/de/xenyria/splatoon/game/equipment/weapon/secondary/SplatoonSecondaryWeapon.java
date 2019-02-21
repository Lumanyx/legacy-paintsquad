package de.xenyria.splatoon.game.equipment.weapon.secondary;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.BrandedEquipment;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class SplatoonSecondaryWeapon extends SplatoonWeapon implements BrandedEquipment {

    public SplatoonSecondaryWeapon(int id, String name) {
        super(id, name);
    }

    public abstract SecondaryWeaponType getSecondaryWeaponType();

    public ItemStack asItemStack() {

        ItemBuilder builder = new ItemBuilder(getRepresentiveMaterial()).setDisplayName(getBrand().getDisplayName() + getName());
        builder.addLore("§8§l> Waffentyp", getSecondaryWeaponType().getName());
        builder.addLore("§0");
        builder.addLore(getSecondaryWeaponType().getDescription().split("\n"));
        builder.addLore("§0");
        builder.addLore("§8§l> Marke");
        builder.addLore(getBrand().getDisplayName());
        builder.addToNBT("WeaponID", getID());
        builder.addToNBT("SecondaryWeapon", true);

        return builder.create();

    }

}
