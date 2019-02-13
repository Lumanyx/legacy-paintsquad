package de.xenyria.splatoon.game.listeners;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.dialog.Dialog;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.listener.SpigotListenerUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.arena.ArenaCategory;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static de.xenyria.splatoon.game.match.BattleMatch.MATCH_OPTIONS;

public class MatchGUIListener implements Listener {

    public MatchGUIListener() {

        SpigotListenerUtil.registerListener(this, XenyriaSplatoon.getPlugin());

    }


    @EventHandler
    public void click(InventoryClickEvent event) {

        Inventory inventory = event.getInventory();
        String title = inventory.getTitle();
        if(title != null) {

            if(title.startsWith(BattleMatch.ROOM_PREFIX) ||
                title.equalsIgnoreCase(MATCH_OPTIONS) ||
                title.equalsIgnoreCase(BattleMatch.BOT_MANAGE_TITLE) ||
                title.equalsIgnoreCase(BattleMatch.CHOOSE_DIFFICULTY) ||
                title.equalsIgnoreCase(BattleMatch.CHOOSE_WEAPONTYPE) ||
                title.equalsIgnoreCase(BattleMatch.CHOOSE_MAP_CATEGORY) ||
                title.equalsIgnoreCase(BattleMatch.CHOOSE_MAP)) {

                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();

                if (stack != null && stack.getType() != Material.AIR) {

                    SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
                    if (player != null) {

                        Match match = player.getMatch();
                        if (match instanceof BattleMatch) {

                            BattleMatch match1 = (BattleMatch) match;
                            if (match1.isLobbyPhase()) {

                                if(title.startsWith(BattleMatch.ROOM_PREFIX)) {

                                    if(ItemBuilder.hasValue(stack, "ManageAI")) {

                                        int aiPlayerIndx = ItemBuilder.getIntValue(stack, "ManageAI");
                                        if(match1.isOwner(player)) {

                                            if(aiPlayerIndx <= (match1.getLobbyAIPlayerEntries().size()-1)) {

                                                BattleMatch.AIPlayer player1 = match1.getLobbyAIPlayerEntries().get(aiPlayerIndx);
                                                match1.showAIOptions(player.getPlayer(), player1, true);

                                            }

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDu kannst die Matcheinstellungen nicht anpassen da du nicht der Raummeister bist.");

                                        }

                                    } else if (ItemBuilder.hasValue(stack, "EnterTeam")) {

                                        int teamID = ItemBuilder.getIntValue(stack, "EnterTeam");
                                        int currentTeamID = match1.getTeamID(player);
                                        if (teamID != currentTeamID) {

                                            if (match1.canJoinTeam(teamID)) {

                                                match1.chooseTeam(player, teamID);
                                                if (teamID != -1) {

                                                    player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du hast das §eTeam " + teamID + " §7betreten.");

                                                } else {

                                                    player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du bist nun ein Zuschauer!");

                                                }
                                                player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                                                match1.queuePlayerInventoryUpdate(player);

                                            } else {

                                                player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Dieses Team ist bereits voll!");

                                            }

                                        }

                                    } else if (ItemBuilder.hasValue(stack, "LeaveMatch")) {

                                        player.leaveMatch();
                                        player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du wirst nun zurück in die Lobby teleportiert.");
                                        XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player);

                                    } else if (ItemBuilder.hasValue(stack, "ManageInventory")) {

                                        player.getInventory().open();

                                    } else if (ItemBuilder.hasValue(stack, "MatchSettings")) {

                                        if (match1.isOwner(player)) {

                                            match1.showMatchOptions(player);

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDu kannst die Matcheinstellungen nicht anpassen da du nicht der Raummeister bist.");

                                        }

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(MATCH_OPTIONS)) {

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.openLobbyInventory(player);

                                    } else if(ItemBuilder.hasValue(stack, "UpdatePassword")) {

                                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player.getPlayer());
                                        Dialog dialog = new Dialog((Player)event.getWhoClicked(), new Dialog.DialogHandler() {
                                            @Override
                                            public boolean validateInput(Player player, String s) {

                                                if(s.isEmpty()) { return true; }
                                                if(s.length() >= 3 && s.length() <= 16) {

                                                    if(!Chat.containsIllegalCharacters(s)) {

                                                        return true;

                                                    } else {

                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Das Passwort enthält ein ungültiges Zeichen!");

                                                    }

                                                } else {

                                                    player.sendMessage(Chat.SYSTEM_PREFIX + "Das Passwort sollte zwischen §e3 und 16 Zeichen §7lang sein.");

                                                }
                                                return false;

                                            }

                                            @Override
                                            public void result(Player player, String s) {

                                                if(match1.isOwner(player1)) {

                                                    if (s.isEmpty()) {

                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Das Passwort wurde entfernt!");

                                                    } else {

                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Neues Passwort festgelegt!");

                                                    }
                                                    match1.updatePassword(s);

                                                }
                                                match1.openLobbyInventory(player1);

                                            }

                                            @Override
                                            public void onQuit(Player player) {

                                            }

                                            @Override
                                            public boolean quitAfterFail() {
                                                return false;
                                            }

                                            @Override
                                            public void onFail() {

                                            }
                                        }, "§6Wähle ein", "§6Raumpasswort!");
                                        dialog.show();

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.BOT_MANAGE_TITLE)) {

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.openLobbyInventory(player);

                                    } else if(ItemBuilder.hasValue(stack, "SaveAIPlayer")) {

                                        match1.saveAIPlayer();
                                        match1.openLobbyInventory(player);
                                        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Bot gespeichert!");

                                    } else if(ItemBuilder.hasValue(stack, "Delete")) {

                                        match1.removeAILobbyPlayer();
                                        match1.openLobbyInventory(player);
                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Bot gelöscht!");

                                    } else if(ItemBuilder.hasValue(stack, "DifficultyChooser")) {

                                        match1.openDifficultyChooser(player);

                                    } else if(ItemBuilder.hasValue(stack, "WeaponTypeChooser")) {

                                        match1.openWeaponTypeChooser(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_WEAPONTYPE)) {

                                    if(ItemBuilder.hasValue(stack, "BackToAIEditor")) {

                                        match1.showAIOptions(player.getPlayer(), match1.getSelectedAIPlayer(), false);

                                    } else if(ItemBuilder.hasValue(stack, "SetType")) {

                                        AIWeaponManager.AIPrimaryWeaponType type = AIWeaponManager.AIPrimaryWeaponType.valueOf(ItemBuilder.getStringValue(stack, "SetType"));
                                        match1.updateSelectedWeaponType(type);
                                        match1.openWeaponTypeChooser(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_DIFFICULTY)) {

                                    if(ItemBuilder.hasValue(stack, "BackToAIEditor")) {

                                        match1.showAIOptions(player.getPlayer(), match1.getSelectedAIPlayer(), false);

                                    } else if(ItemBuilder.hasValue(stack, "SetDifficulty")) {

                                        AIProperties.Difficulty type = AIProperties.Difficulty.valueOf(ItemBuilder.getStringValue(stack, "SetDifficulty"));
                                        match1.updateSelectedDifficulty(type);
                                        match1.openDifficultyChooser(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_MAP_CATEGORY)) {

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.showMatchOptions(player);

                                    } else if(ItemBuilder.hasValue(stack, "ShowMaps")) {

                                        ArenaCategory category = ArenaCategory.valueOf(ItemBuilder.getStringValue(stack, "ShowMaps"));
                                        match1.showAllMaps(player, category);

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

    }

}
