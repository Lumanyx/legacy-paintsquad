package de.xenyria.splatoon.game.gui;

import de.xenyria.api.spigot.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class StaticItems {

    public static final ItemStack OPEN_JUMP_MENU = new ItemBuilder(Material.FIREWORK_STAR).setDisplayName("§7§lSupersprung").addToNBT("NoClick", true).create();
    public static final ItemStack RESET_MAP = new ItemBuilder(Material.PAPER).setDisplayName("§7§lMap zurücksetzen").addToNBT("ResetMap", true).create();
    public static final ItemStack CHANGE_WEAPON = new ItemBuilder(Material.CHEST).setDisplayName("§7§lWaffenset wählen").addToNBT("ChangeWeapon", true).create();
    public static final ItemStack RETURN_TO_LOBBY = new ItemBuilder(Material.BARRIER).setDisplayName("§c§lZur Lobby").create();
    public static final ItemStack RETURN_TO_WEAPONSHOP = new ItemBuilder(Material.BARRIER).setDisplayName("§c§lZum Waffenshop").create();

}
