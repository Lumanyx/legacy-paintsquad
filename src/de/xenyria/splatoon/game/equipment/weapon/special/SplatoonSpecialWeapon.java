package de.xenyria.splatoon.game.equipment.weapon.special;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class SplatoonSpecialWeapon extends SplatoonWeapon {

    private String description;
    public String getDescription() { return description; }

    public SplatoonSpecialWeapon(int id, String name, String desc, int requiredPoints) {

        super(id, name);
        this.description = desc;
        this.requiredPoints = requiredPoints;

    }

    private int requiredPoints;
    public int getRequiredPoints() { return requiredPoints; }

    public ItemStack asItemStack() {

        ItemBuilder builder = new ItemBuilder(getRepresentiveMaterial()).setDisplayName("§6§l" + getName());
        builder.addLore("§8§l> Waffentyp", "§6§lSpezial");
        builder.addLore("§0");
        builder.addLore(getDescription().split("\n"));
        builder.addLore("§0");
        builder.addToNBT("WeaponID", getID());
        builder.addToNBT("SpecialWeapon", true);

        return builder.create();

    }

    public abstract boolean isActive();

}
