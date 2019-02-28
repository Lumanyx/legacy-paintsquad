package de.xenyria.splatoon.game.match;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.dialog.Dialog;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.turfwar.TurfWarMatch;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class MatchManager {

    private static ArrayList<BattleMatch> customBattles = new ArrayList<>(), publicBattles = new ArrayList<>();
    public static ArrayList<BattleMatch> getCustomBattles() { return customBattles; }
    public static ArrayList<BattleMatch> getPublicBattles() { return publicBattles; }

    public static String CUSTOM_BATTLES_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cEigene Kämpfe §8| §6Raumliste";

    private static Inventory inventory = Bukkit.createInventory(null, 54, CUSTOM_BATTLES_TITLE);

    public static final int MAX_PRIVATE_MATCHES = 45;

    public static BattleMatch getPrivateMatch(int roomID) {

        for(BattleMatch match1 : customBattles) {

            if(match1.getRoomID() == roomID) {

                return match1;

            }

        }
        return null;

    }

    public boolean newPrivateMatchPossible() {

        return customBattles.size() < MAX_PRIVATE_MATCHES;

    }

    public BattleMatch getRoom(int roomID) {

        for(BattleMatch match1 : customBattles) {

            if(match1.getRoomID() == roomID) {

                return match1;

            }

        }
        for(BattleMatch match1 : publicBattles) {

            if(match1.getRoomID() == roomID) {

                return match1;

            }

        }
        return null;

    }

    public boolean isRoomLimitExceeded() {

        return (customBattles.size()+1) > MAX_PRIVATE_MATCHES;

    }

    public BattleMatch createNewRoom(SplatoonHumanPlayer owner) {

        BattleMatch match1 = new TurfWarMatch();
        owner.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Neuer Raum erstellt! §8(§bRaum-ID: " + match1.getRoomID() + "§8)");
        match1.changeOwner(owner);
        owner.leaveMatch();
        owner.joinMatch(match1);
        customBattles.add(match1);

        return match1;

    }

    public Inventory getPrivateLobbyInventory() { return inventory; }

    public void removeRoom(BattleMatch battleMatch) {

        customBattles.remove(battleMatch);
        publicBattles.remove(battleMatch);
        XenyriaSplatoon.getXenyriaLogger().log("§bRaum #" + battleMatch.getRoomID() + " §7entfernt.");

    }

    public enum MatchStatus {

        OK(Material.GREEN_WOOL, "§aBetretbar!"),
        IN_PROGRESS(Material.ENDER_PEARL, "§eKampf aktiv - Zuschauerplatz frei"),
        SPECTATEABLE(Material.ENDER_EYE, "§eKampf inaktiv - Zuschauerplatz frei"),
        FULL(Material.RED_WOOL, "§cDieser Raum ist voll."),
        REJOINABLE(Material.YELLOW_WOOL, "§eKampf aktiv - Betretbar");

        private Material material;
        private String description;

        MatchStatus(Material material, String description) {

            this.material = material;
            this.description = description;

        }

    }

    public void openPasswordPrompt(SplatoonHumanPlayer player, int matchID) {

        BattleMatch match1 = getPrivateMatch(matchID);
        if(match1 != null) {

            player.getPlayer().closeInventory();
            Dialog dialog = new Dialog(player.getPlayer(), new Dialog.DialogHandler() {
                @Override
                public boolean validateInput(Player player, String s) {

                    return !match1.hasPassword() || s.equalsIgnoreCase(match1.getPassword());

                }

                @Override
                public void result(Player player1, String s) {

                    joinPrivateMatch(player, matchID);

                }

                @Override
                public void onQuit(Player player) {

                    player.getPlayer().openInventory(XenyriaSplatoon.getMatchManager().getPrivateRoomInventory());

                }

                @Override
                public boolean quitAfterFail() {
                    return true;
                }

                @Override
                public void onFail() {

                    player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Das Passwort ist ungültig oder falsch.");
                    player.getPlayer().openInventory(XenyriaSplatoon.getMatchManager().getPrivateRoomInventory());

                }
            }, "§6Gebe das", "§6Raumpasswort ein!");
            dialog.show();

        } else {

            player.sendMessage(Chat.SYSTEM_PREFIX + "Der gewählte Raum existiert nicht mehr.");

        }

    }

    private Inventory getPrivateRoomInventory() { return inventory; }

    public void joinPasswordProtectedMatch(SplatoonHumanPlayer player, int id) {

        BattleMatch match1 = getPrivateMatch(id);
        if(match1 != null) {

            if(match1.remainingTeamSpace() > 0) {

                openPasswordPrompt(player, id);

            } else if(match1.remainingSpectatorSpace() > 0) {

                openPasswordPrompt(player, id);

            } else {

                player.sendMessage(Chat.SYSTEM_PREFIX + "Du kannst diesem Raum nicht beitreten. Es sind keine Plätze mehr frei.");

            }

        } else {

            player.sendMessage(Chat.SYSTEM_PREFIX + "Der gewählte Raum ist nicht mehr verfügbar.");

        }

    }

    public void joinPrivateMatch(SplatoonHumanPlayer player, int id) {

        BattleMatch match1 = getPrivateMatch(id);
        if(match1 != null) {

            if(match1.remainingTeamSpace() > 0) {

                if(match1.inProgress()) {

                    if (match1.allowRejoining()) {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "Der Raummeister hat eingestellt, dass Spieler während der Runde beitreten können.");
                        player.sendMessage("§8§m--§r§8> §7Wähle daher bitte ein Team.");
                        match1.openTeamChooseMenu(player);

                    } else {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "Du trittst als Zuschauer bei!");
                        player.leaveMatch();
                        player.getPlayer().teleport(match1.spectatorSpawnLocation);
                        player.joinMatch(match1);
                        match1.chooseTeam(player, -1);

                    }

                } else {

                    player.leaveMatch();
                    player.joinMatch(match1);
                    // Player Pool
                    match1.chooseTeam(player, -2);

                }

            } else if(match1.remainingSpectatorSpace() > 0) {

                player.leaveMatch();
                player.joinMatch(match1);
                // Player Pool
                match1.chooseTeam(player, -1);

            } else {

                player.sendMessage(Chat.SYSTEM_PREFIX + "Du kannst diesem Raum nicht beitreten. Es sind keine Plätze mehr frei.");

            }

        } else {

            player.sendMessage(Chat.SYSTEM_PREFIX + "Der gewählte Raum ist nicht mehr verfügbar.");

        }

    }

    public void updatePrivateRoomInventory() {

        inventory.clear();
        int i = 0;
        for(BattleMatch match : customBattles) {

            Material material = null;
            MatchStatus status = null;
            if(match.inProgress()) {

                if(match.allowRejoining()) {

                    if(match.remainingTeamSpace() > 0) {

                        status = MatchStatus.REJOINABLE;

                    } else {

                        if(match.remainingSpectatorSpace() > 0) {

                            status = MatchStatus.SPECTATEABLE;

                        } else {

                            status = MatchStatus.FULL;

                        }

                    }

                } else {

                    if (match.remainingSpectatorSpace() > 0) {

                        status = MatchStatus.IN_PROGRESS;

                    } else {

                        status = MatchStatus.FULL;

                    }

                }

            } else {

                if(match.remainingTeamSpace() >= 1) {

                    status = MatchStatus.OK;

                } else if(match.remainingSpectatorSpace() >= 1) {

                    status = MatchStatus.SPECTATEABLE;

                } else {

                    status = MatchStatus.FULL;

                }

            }

            ItemBuilder builder = new ItemBuilder(status.material).setDisplayName(status.description).addAttributeHider();
            if(match.hasPassword()) {

                builder.addLore("", "§c-*- Passwort geschützt -*-", "");
                builder.addEnchantment(Enchantment.DURABILITY, 1);

            } else {

                builder.addLore("");

            }

            int players = match.getAllPlayers().size();
            builder.addLore("§7Spieler: §b" + players + "§8/§b" + (match.totalPlayerCount()));
            ArenaData data = match.getDataForSelectedArena();
            builder.addLore("§7Karte: §b" + data.getArenaName() + " §8(§6" + match.getTeamCount() + " Teams â " + match.getPlayersPerTeamCount() + " Spieler§8)");
            builder.addLore("§7Spielmodus: §6" + match.getMatchType().getTitle());
            builder.addLore("");
            builder.addToNBT("JoinMatch", match.getRoomID());
            inventory.setItem(i, builder.create());
            i++;

            //I/temBuilder builder = new ItemBuilder();

        }
        for(int y = 45; y < 54; y++) {

            inventory.setItem(y, ItemBuilder.getUnclickablePane());

        }
        if((customBattles.size()+1) <= MAX_PRIVATE_MATCHES) {

            inventory.setItem(53, StaticItems.CREATE_NEW_MATCH);

        } else {

            inventory.setItem(53, StaticItems.NO_NEW_ROOM_POSSIBLE);

        }

    }

    public MatchManager() {

        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

            updatePrivateRoomInventory();

        }, 10l, 10l);

    }

}
