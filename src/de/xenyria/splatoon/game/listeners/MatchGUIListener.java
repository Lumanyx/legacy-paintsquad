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
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.outro.OutroManager;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import static de.xenyria.splatoon.game.match.BattleMatch.MATCH_OPTIONS;

public class MatchGUIListener implements Listener {

    public MatchGUIListener() {

        SpigotListenerUtil.registerListener(this, XenyriaSplatoon.getPlugin());

    }

    @EventHandler
    public void close(InventoryCloseEvent event) {

        Inventory inventory = event.getInventory();
        String title = inventory.getTitle();
        if(title != null) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getPlayer());
            if(player != null) {

                Match match = player.getMatch();
                if(match instanceof BattleMatch) {

                    BattleMatch match1 = (BattleMatch) match;
                    if(match1.inOutro()) {

                        if(title.equalsIgnoreCase(OutroManager.BATTLE_STATISTIC)) {

                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                Player player1 = (Player) event.getPlayer();
                                if(player1.getOpenInventory() == null || player1.getOpenInventory().getTopInventory() == null) {

                                    player.getPlayer().openInventory(match1.getOutroManager().getResultScreen());

                                }

                            },1l);

                        } else if(title.equalsIgnoreCase(OutroManager.BACK_TO_LOBBY_TITLE)) {

                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                Player player1 = (Player) event.getPlayer();
                                if(player1.getOpenInventory() == null || player1.getOpenInventory().getTopInventory() == null) {

                                    match1.getOutroManager().doNotStay(player);

                                }

                            },1l);

                        }

                    }

                }

            }

        }

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
                title.equalsIgnoreCase(BattleMatch.CHOOSE_MAP) ||
                title.equalsIgnoreCase(BattleMatch.MUSIC_SELECTION_TITLE) ||
                title.equalsIgnoreCase(BattleMatch.ADD_AI_SCREEN) ||
                title.equalsIgnoreCase(BattleMatch.PLAYER_MANAGE_TITLE) ||
                title.equalsIgnoreCase(BattleMatch.SPECTATOR_MENU_TITLE) ||
                title.equalsIgnoreCase(OutroManager.BACK_TO_LOBBY_TITLE) ||
                title.equalsIgnoreCase(OutroManager.BATTLE_STATISTIC) ||
                title.equalsIgnoreCase(BattleMatch.SPECTATE_MENU_TITLE)) {

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
                                        if (match1.isOwner(player)) {

                                            if (aiPlayerIndx <= (match1.getLobbyAIPlayerEntries().size() - 1)) {

                                                BattleMatch.AIPlayer player1 = match1.getLobbyAIPlayerEntries().get(aiPlayerIndx);
                                                match1.showAIOptions(player.getPlayer(), player1, true);

                                            }

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDu kannst die Matcheinstellungen nicht anpassen da du nicht der Raummeister bist.");

                                        }

                                    } else if(ItemBuilder.hasValue(stack, "StartGame")) {

                                        if(match1.canStart()) {

                                            if(!match1.isCountdownPhase()) {

                                                match1.startCountDown();
                                                match1.updatePlayerLobbyInventories();

                                            }

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Es muss mindestens §eein Spieler §7in §ezwei verschiedenen Teams §7sein um das Match zu starten.");

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

                                    } else if (ItemBuilder.hasValue(stack, "ChooseMusic")) {

                                        match1.showMusicSelection(player);

                                    } else if (ItemBuilder.hasValue(stack, "MatchSettings")) {

                                        if (match1.isOwner(player)) {

                                            match1.showMatchOptions(player);

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "§cDu kannst die Matcheinstellungen nicht anpassen da du nicht der Raummeister bist.");

                                        }

                                    } else if(ItemBuilder.hasValue(stack, "AddAI")) {

                                        if(match1.isOwner(player)) {

                                            match1.showAIAddScreen(player, ItemBuilder.getIntValue(stack, "AddAI"));

                                        } else {

                                            match1.updateInventory(player);

                                        }

                                    } else if(ItemBuilder.hasValue(stack, "ManagePlayer")) {

                                        match1.managePlayer(player, UUID.fromString(ItemBuilder.getStringValue(stack, "ManagePlayer")));

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

                                    } else if(ItemBuilder.hasValue(stack, "MapCategories")) {

                                        match1.showMapCategories(player);

                                    } else if(ItemBuilder.hasValue(stack, "SetTeamAmount")) {

                                        if(match1.isTeamCountChangeable()) {

                                            ArenaData data = match1.getDataForSelectedArena();
                                            Dialog dialog = new Dialog(((Player) event.getWhoClicked()), new Dialog.DialogHandler() {
                                                @Override
                                                public boolean validateInput(Player player, String s) {

                                                    try {

                                                        int i = Integer.parseInt(s);
                                                        return i>=2&&i<=data.getMaxTeams();

                                                    } catch (Exception e) {

                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe eine Zahl zwischen §e2 §7und §e" + data.getMaxTeams() + " §7an.");
                                                        return false;

                                                    }

                                                }

                                                @Override
                                                public void result(Player player, String s) {

                                                    SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                                                    if(match1.isOwner(player1)) {

                                                        int i = Integer.parseInt(s);
                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Die Anzahl der Teams wurde auf §e" + i + " §7gesetzt.");
                                                        match1.updateMaxTeams(i);

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
                                            }, "§6Wieviele Teams?", "§8(Min. 2, Max. " + data.getMaxTeams() + ")");
                                            dialog.show();

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Die Teamanzahl kann nicht verändert werden da die derzeitige Arena nur zwei Teams unterstützt.");

                                        }

                                    } else if (ItemBuilder.hasValue(stack, "SetPlayerCount")) {

                                        if(match1.isPlayerCountChangeable()) {

                                            ArenaData data = match1.getDataForSelectedArena();
                                            Dialog dialog = new Dialog(((Player) event.getWhoClicked()), new Dialog.DialogHandler() {
                                                @Override
                                                public boolean validateInput(Player player, String s) {

                                                    try {

                                                        int i = Integer.parseInt(s);
                                                        return i >= 1 && i <= data.getMaxPlayersPerTeam();

                                                    } catch (Exception e) {

                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe eine Zahl zwischen §e1 §7und §e" + data.getMaxPlayersPerTeam() + " §7an.");
                                                        return false;

                                                    }

                                                }

                                                @Override
                                                public void result(Player player, String s) {

                                                    SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                                                    if (match1.isOwner(player1)) {

                                                        int i = Integer.parseInt(s);
                                                        player.sendMessage(Chat.SYSTEM_PREFIX + "Die Anzahl der Spieler pro Team wurde auf §e" + i + " §7gesetzt.");
                                                        match1.updateMaxPlayers(i);

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
                                            }, "§6Wieviele Spieler/Team?", "§8(Min. 1, Max. " + data.getMaxPlayersPerTeam() + ")");
                                            dialog.show();

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Die Spieleranzahl kann nicht verändert werden da die derzeitige Arena nur einen Spieler pro Team unterstützt.");

                                        }

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

                                        match1.openDifficultyChooser(player, false);

                                    } else if(ItemBuilder.hasValue(stack, "WeaponTypeChooser")) {

                                        match1.openWeaponTypeChooser(player, false);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_WEAPONTYPE)) {

                                    if(ItemBuilder.hasValue(stack, "BackToAIEditor")) {

                                        match1.showAIOptions(player.getPlayer(), match1.getSelectedAIPlayer(), false);

                                    } else if(ItemBuilder.hasValue(stack, "BackToAIBuilder")) {

                                        match1.showAIAddScreen(player, -1);

                                    } else if(ItemBuilder.hasValue(stack, "SetType")) {

                                        AIWeaponManager.AIPrimaryWeaponType type = AIWeaponManager.AIPrimaryWeaponType.valueOf(ItemBuilder.getStringValue(stack, "SetType"));
                                        match1.updateSelectedWeaponType(type);
                                        match1.openWeaponTypeChooser(player, ItemBuilder.getBooleanValue(inventory.getItem(8), "BackToAIEditor"));

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_DIFFICULTY)) {

                                    if(ItemBuilder.hasValue(stack, "BackToAIEditor")) {

                                        match1.showAIOptions(player.getPlayer(), match1.getSelectedAIPlayer(), false);

                                    } else if(ItemBuilder.hasValue(stack, "BackToAIBuilder")) {

                                        match1.showAIAddScreen(player, -1);

                                    } else if(ItemBuilder.hasValue(stack, "SetDifficulty")) {

                                        AIProperties.Difficulty type = AIProperties.Difficulty.valueOf(ItemBuilder.getStringValue(stack, "SetDifficulty"));
                                        match1.updateSelectedDifficulty(type);
                                        match1.openDifficultyChooser(player, ItemBuilder.getBooleanValue(inventory.getItem(8), "BackToAIEditor"));

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_MAP_CATEGORY)) {

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.showMatchOptions(player);

                                    } else if(ItemBuilder.hasValue(stack, "ArenaCategory")) {

                                        ArenaCategory category = ArenaCategory.valueOf(ItemBuilder.getStringValue(stack, "ArenaCategory"));
                                        match1.showAllMaps(player, category);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.CHOOSE_MAP)) {

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.showMapCategories(player);

                                    } else if(ItemBuilder.hasValue(stack, "SelectMap")) {

                                        int mapID = ItemBuilder.getIntValue(stack, "SelectMap");
                                        ArenaData arenaData = XenyriaSplatoon.getArenaRegistry().getArenaData(mapID);
                                        player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Arena geändert!");
                                        match1.switchMap(arenaData);
                                        if(match1.playersPerTeam() > arenaData.getMaxPlayersPerTeam() || match1.getTeamCount() > arenaData.getMaxTeams()) {

                                            player.getPlayer().sendMessage("§8-> §7Aufgrund der neuen Arena werden die Anzahl der §eTeams §7und §eSpieler-pro-Team §7zurückgesetzt.");
                                            match1.resetTeams();

                                        } else {

                                            match1.updatePlayerLobbyInventories();

                                        }
                                        match1.openLobbyInventory(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.MUSIC_SELECTION_TITLE)) {

                                    if(ItemBuilder.hasValue(stack, "SelectTrack")) {

                                        int i = ItemBuilder.getIntValue(stack, "SelectTrack");
                                        match1.setMusicID(player, i);
                                        match1.showMusicSelection(player);

                                    } else if(ItemBuilder.hasValue(stack, "SaveTheMelody")) {

                                        match1.updateInventory(player);
                                        match1.openLobbyInventory(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.ADD_AI_SCREEN)) {

                                    if(ItemBuilder.hasValue(stack, "DifficultyChooser")) {

                                        match1.openDifficultyChooser(player, true);

                                    } else if(ItemBuilder.hasValue(stack, "WeaponTypeChooser")) {

                                        match1.openWeaponTypeChooser(player, true);

                                    } else if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.openLobbyInventory(player);

                                    } else if(ItemBuilder.hasValue(stack, "AddAI")) {

                                        match1.handleAIAdd(player);

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.PLAYER_MANAGE_TITLE)) {

                                    ItemStack uuidStack = event.getInventory().getItem(13);
                                    UUID uuid = UUID.fromString(ItemBuilder.getStringValue(uuidStack, "UUID"));

                                    if(ItemBuilder.hasValue(stack, "Exit")) {

                                        match1.openLobbyInventory(player);

                                    } else if(ItemBuilder.hasValue(stack, "KickPlayer")) {

                                        SplatoonHumanPlayer player1 = match1.getPlayerFromUUID(uuid);
                                        if(player1 != null) {

                                            match1.kickPlayer(player1);

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Das Menü wurde geschlossen da der ausgewählte Spieler nicht mehr im Raum ist.");
                                            match1.openLobbyInventory(player);

                                        }

                                    } else if(ItemBuilder.hasValue(stack, "PromotePlayer")) {

                                        SplatoonHumanPlayer player1 = match1.getPlayerFromUUID(uuid);
                                        if(player1 != null) {

                                            match1.changeOwner(player1);

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Das Menü wurde geschlossen da der ausgewählte Spieler nicht mehr im Raum ist.");
                                            match1.openLobbyInventory(player);

                                        }

                                    }

                                }

                            } else {

                                if(inventory.getTitle().equalsIgnoreCase(BattleMatch.SPECTATOR_MENU_TITLE)) {

                                    if(ItemBuilder.hasValue(stack, "TeleportToPlayer")) {

                                        int id = ItemBuilder.getIntValue(stack, "TeleportToPlayer");
                                        SplatoonPlayer player1 = match1.getPlayerFromID(id);
                                        if(player1 != null) {

                                            player.getPlayer().closeInventory();
                                            match1.openSpectateMenu(player.getPlayer(), player1);

                                        } else {

                                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Der gewählte Spieler ist nicht mehr verfügbar.");
                                            player.getPlayer().openInventory(match1.createSpectatorMenu());

                                        }

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(OutroManager.BACK_TO_LOBBY_TITLE)) {

                                    if(match1.getOutroManager().processResultScreenEvents()) {

                                        if (ItemBuilder.hasValue(stack, "Leave")) {

                                            match1.getOutroManager().doNotStay(player);

                                        } else if (ItemBuilder.hasValue(stack, "Stay")) {

                                            if (!player.getMatch().getOutroManager().isStaying(player)) {

                                                player.getMatch().getOutroManager().addStayingPlayer(player);
                                                player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Okay! Nach Ablauf des Countdowns wird sich das Raummenü öffnen.");

                                            }

                                        } else if (ItemBuilder.hasValue(stack, "Stats")) {

                                            player.getPlayer().openInventory(player.getMatch().getOutroManager().getStatisticScreen());

                                        }

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(OutroManager.BATTLE_STATISTIC)) {

                                    if(match1.getOutroManager().processResultScreenEvents()) {

                                        if (ItemBuilder.hasValue(stack, "Back")) {

                                            player.getPlayer().openInventory(match1.getOutroManager().getResultScreen());

                                        }

                                    }

                                } else if(inventory.getTitle().equalsIgnoreCase(BattleMatch.SPECTATE_MENU_TITLE)) {

                                    if(ItemBuilder.hasValue(stack, "QuitSpectator")) {

                                        player.getPlayer().closeInventory();
                                        player.leaveSpectatorMode();

                                    } else if(ItemBuilder.hasValue(stack, "CameraMode")) {

                                        ItemStack playerHead = inventory.getItem(0);
                                        int playerIndex = ItemBuilder.getIntValue(playerHead, "PlayerIndex");
                                        SplatoonHumanPlayer.SpectatorCameraMode mode = SplatoonHumanPlayer.SpectatorCameraMode.valueOf(
                                                ItemBuilder.getStringValue(stack, "CameraMode")
                                        );
                                        SplatoonPlayer player1 = match1.getPlayerFromID(playerIndex);
                                        if(player1 != null) {

                                            player.getPlayer().closeInventory();
                                            player.spectatePlayer(player1, mode);

                                        } else {

                                            player.getPlayer().closeInventory();
                                            player.sendMessage(Chat.SYSTEM_PREFIX + "Der gewählte Spieler konnte nicht gefunden werden.");

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

}
