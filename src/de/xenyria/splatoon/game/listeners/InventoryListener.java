package de.xenyria.splatoon.game.listeners;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.listener.SpigotListenerUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemCategory;
import de.xenyria.splatoon.game.player.userdata.inventory.UserInventory;
import de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.lobby.shop.weapons.WeaponShop;
import de.xenyria.splatoon.shootingrange.ShootingRange;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem.EQUIP_ASK_TITLE;

public class InventoryListener implements Listener {

    public InventoryListener() {

        SpigotListenerUtil.registerListener(this, XenyriaSplatoon.getPlugin());

    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {

        if(event.getInventory() != null && event.getInventory().getTitle() != null) {

            if(event.getInventory().getTitle().equalsIgnoreCase(UserInventory.INVENTORY_TITLE)) {

                event.setCancelled(true);

                ItemStack stack = event.getCurrentItem();
                if(stack != null) {

                    SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                    if(!player.getInventory().isAnimationActive()) {

                        if (ItemBuilder.hasValue(stack, "SelectCategory")) {

                            String targetCategory = ItemBuilder.getStringValue(stack, "SelectCategory");
                            ItemCategory category = ItemCategory.valueOf(targetCategory);

                            player.getInventory().changeCategory(category, 0);


                        } else if(ItemBuilder.hasValue(stack, "ToPage")) {

                            int targetPage = ItemBuilder.getIntValue(stack, "ToPage");
                            player.getInventory().changeCategory(player.getInventory().getCurrentCategory(), targetPage);

                        } else if(ItemBuilder.hasValue(stack, "ItemID")) {

                            int itemID = ItemBuilder.getIntValue(stack, "ItemID");
                            player.getInventory().onItemClick(itemID);

                        }

                    }

                }

            } else if(event.getInventory().getTitle().equalsIgnoreCase(EQUIP_ASK_TITLE)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                if(stack != null) {

                    ItemStack item = event.getInventory().getItem(13);

                    if(ItemBuilder.hasValue(stack, "Dismiss")) {

                        player.getInventory().open();

                    } else if(ItemBuilder.hasValue(stack, "EnterShootingRange")) {

                        if(player.getMatch() instanceof SplatoonLobby) {

                            XenyriaSplatoon.getShootingRangeManager().joinShootingRange(player, false);
                            int setID = ItemBuilder.getIntValue(item, "SetID");
                            WeaponSet set = WeaponSetRegistry.getSet(setID);
                            player.getEquipment().applySet(set);

                        }

                    } else if(ItemBuilder.hasValue(stack, "EquipSet")) {

                        int val = ItemBuilder.getIntValue(stack, "EquipSet");
                        player.getInventory().equipSet(val);
                        player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Waffenset für Kämpfe ausgerüstet!");
                        player.getInventory().open();

                    }

                }

            } else if(event.getInventory().getTitle().equalsIgnoreCase(WeaponShop.CATEGORY_OVERVIEW)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                if(stack != null) {

                    if(ItemBuilder.hasValue(stack, "ShowWeaponCategory")) {

                        PrimaryWeaponType type = PrimaryWeaponType.valueOf(ItemBuilder.getStringValue(stack, "ShowWeaponCategory"));
                        SplatoonLobby lobby = XenyriaSplatoon.getLobbyManager().getLobby();
                        WeaponShop shop = lobby.getWeaponShop();

                        shop.showAllWeapons(player.getPlayer(), type, player.getMatch() instanceof ShootingRange);

                    }

                }

            } else if(event.getInventory().getTitle().equalsIgnoreCase(WeaponShop.WEAPON_SETS)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                SplatoonLobby lobby = XenyriaSplatoon.getLobbyManager().getLobby();
                WeaponShop shop = lobby.getWeaponShop();

                if(stack.equals(WeaponShop.BACK_TO_OVERVIEW)) {

                    Inventory inventory = shop.getOverviewInventory();
                    event.getWhoClicked().openInventory(inventory);

                } else {

                    if(ItemBuilder.hasValue(stack, "WeaponSetForShootingRange")) {

                        int setID = ItemBuilder.getIntValue(stack, "WeaponSetForShootingRange");
                        WeaponSet set = WeaponSetRegistry.getSet(setID);
                        event.getWhoClicked().closeInventory();
                        player.getEquipment().applySet(set);

                    } else if(ItemBuilder.hasValue(stack, "ShowWeaponPurchaseScreen")) {

                        int setID = ItemBuilder.getIntValue(stack, "ShowWeaponPurchaseScreen");
                        SplatoonLobby lobby1 = XenyriaSplatoon.getLobbyManager().getLobby();
                        lobby1.getWeaponShop().showPurchaseScreen(player, setID);

                    }

                }

            } else if(event.getInventory().getTitle().equalsIgnoreCase(WeaponShop.PURCHASE_SET)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                SplatoonLobby lobby = XenyriaSplatoon.getLobbyManager().getLobby();
                WeaponShop shop = lobby.getWeaponShop();

                if(stack != null) {

                    if(ItemBuilder.hasValue(stack, "DismissCategory")) {

                        PrimaryWeaponType type = PrimaryWeaponType.valueOf(ItemBuilder.getStringValue(stack, "DismissCategory"));
                        shop.showAllWeapons(player.getPlayer(), type, false);

                    } else if(ItemBuilder.hasValue(stack, "EnterShootingRangeAbs")) {

                        int setID = ItemBuilder.getIntValue(stack, "EnterShootingRangeAbs");
                        if(!(player.getMatch() instanceof ShootingRange)) {

                            XenyriaSplatoon.getShootingRangeManager().joinShootingRange(player, true);
                            player.getEquipment().applySet(WeaponSetRegistry.getSet(setID));

                        }

                    } else if(ItemBuilder.hasValue(stack, "PurchaseSet")) {

                        int setID = ItemBuilder.getIntValue(stack, "PurchaseSet");
                        WeaponSet set = WeaponSetRegistry.getSet(setID);
                        boolean alreadyPurchased = player.getInventory().hasSetInInventory(setID);
                        if(!alreadyPurchased) {

                            if (player.getUserData().getCoins() >= set.getCost()) {

                                player.getUserData().subtractCoins(set.getCost());
                                player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du hast das §eWaffenset " + set.getName() + " §7für §6" + set.getCost() + " Taler §7gekauft!");
                                WeaponSetItem.fromSet(player, set, false);

                                Inventory inventory = Bukkit.createInventory(null, 36, WeaponShop.PURCHASED_EQUIP_NOW);
                                for(int i = 0; i < 36; i++) {

                                    inventory.setItem(i, ItemBuilder.getUnclickablePane());

                                }

                                inventory.setItem(29, new ItemBuilder(Material.BARRIER).setDisplayName("§cNein").addToNBT("Dismiss", true).create());
                                inventory.setItem(33, new ItemBuilder(Material.EMERALD).setDisplayName("§aJa").addToNBT("EquipSet", player.getInventory().getLocalSetID(set.getSetID())).create());

                            } else {

                                int remainingCoins = set.getCost()-player.getUserData().getCoins();
                                player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDir fehlen §e" + remainingCoins + " Taler §cfür dieses Waffenset!");

                            }

                        } else {

                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDu hast dieses Set bereits gekauft!");

                        }

                    }

                }

            } else if(event.getInventory().getTitle().equalsIgnoreCase(WeaponShop.PURCHASED_EQUIP_NOW)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());

                if(stack != null) {

                    if(ItemBuilder.hasValue(stack, "Dismiss")) {

                        event.getWhoClicked().openInventory(XenyriaSplatoon.getLobbyManager().getLobby().getWeaponShop().getOverviewInventory());

                    } else if(ItemBuilder.hasValue(stack, "EquipSet")) {

                        int setID = ItemBuilder.getIntValue(stack, "EquipSet");
                        player.getInventory().equipSet(setID);
                        player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Set ausgerüstet!");
                        event.getWhoClicked().openInventory(XenyriaSplatoon.getLobbyManager().getLobby().getWeaponShop().getOverviewInventory());

                    }

                }

            }

        }

    }

    @EventHandler
    public void inventoryDrag(InventoryDragEvent event) {

        if(event.getInventory() != null && event.getInventory().getTitle() != null) {

            if(event.getInventory().getTitle().equalsIgnoreCase(UserInventory.INVENTORY_TITLE) ||
            event.getInventory().getTitle().equalsIgnoreCase(EQUIP_ASK_TITLE)) {

                event.setCancelled(true);

            }

        }

    }

}
