package de.xenyria.splatoon.game.gui;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.match.MatchManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class StaticItems {

    public static final ItemStack OPEN_JUMP_MENU = new ItemBuilder(Material.FIREWORK_STAR).setDisplayName("§7§lSupersprung").addToNBT("NoClick", true).create();
    public static final ItemStack RESET_MAP = new ItemBuilder(Material.PAPER).setDisplayName("§7§lMap zurücksetzen").addToNBT("ResetMap", true).create();
    public static final ItemStack CHANGE_WEAPON = new ItemBuilder(Material.CHEST).setDisplayName("§7§lWaffenset wählen").addToNBT("ChangeWeapon", true).create();
    public static final ItemStack RETURN_TO_LOBBY = new ItemBuilder(Material.BARRIER).setDisplayName("§c§lZur Lobby").create();
    public static final ItemStack RETURN_TO_WEAPONSHOP = new ItemBuilder(Material.BARRIER).setDisplayName("§c§lZum Waffenshop").create();
    public static final ItemStack SPECTATE = new ItemBuilder(Material.ENDER_EYE).setDisplayName("§7§lZu Spieler teleportieren").addToNBT("OpenTeleportMenu", true).create();
    public static final ItemStack CREATE_NEW_MATCH = new ItemBuilder(Material.DIAMOND_SWORD).setDisplayName("§cNeuen Raum erstellen").addToNBT("CreateNewRoom", true).create();
    public static final ItemStack NO_NEW_ROOM_POSSIBLE = new ItemBuilder(Material.BARRIER).setDisplayName("§cRaumlimit erreicht!").addLore("§7Du kannst keinen neuen Raum erstellen", "§7da bereits §e" + MatchManager.MAX_PRIVATE_MATCHES + " Räume §7erstellt wurden.").create();

}
