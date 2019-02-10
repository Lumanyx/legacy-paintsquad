package de.xenyria.splatoon.game.util;

import de.xenyria.splatoon.game.color.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class ArmorUtil {

    public static ItemStack getHelmet(Color color) {

        ItemStack stack = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color.getBukkitColor());
        stack.setItemMeta(meta);
        return stack;

    }

    public static ItemStack getChestplate(Color color) {

        ItemStack stack = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color.getBukkitColor());
        stack.setItemMeta(meta);
        return stack;

    }

    public static ItemStack getBoots(Color color) {

        ItemStack stack = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color.getBukkitColor());
        stack.setItemMeta(meta);
        return stack;

    }

}
