package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.io.database.mysql.DatabaseQuery;
import de.xenyria.io.database.mysql.MySQLUtil;
import de.xenyria.io.database.mysql.variables.BasicVariable;
import de.xenyria.servercore.player.XenyriaPlayer;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;

public class SplatoonCoinsCommand extends SpigotCommand implements CommandExecutor {

    public SplatoonCoinsCommand() {
        super("scoins", "Befehl zum Anpassen der Taler.", "xenyria.splatoon.admin");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender.hasPermission("xenyria.splatoon.admin")) {

            if (args.length == 0) {

                commandSender.sendMessage("§8" + Characters.FAT_ARROW_RIGHT + " §aSplatoon Talerverwaltungsbefehl");
                commandSender.sendMessage("§8- §e/scoins set <ID> <Anzahl>");
                commandSender.sendMessage("§8- §e/scoins get <ID>");

            } else if (args.length == 2) {

                if(args[0].equalsIgnoreCase("get")) {

                    ArgumentCastResult result = castToInt(args[1]);
                    if(!result.isSuccess()) {

                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Die Spieler-ID muss eine Zahl sein!");
                        return true;

                    }

                    int userID = result.getInt();
                    try {

                        XenyriaPlayer player = XenyriaPlayer.resolveByID(result.getInt());
                        if(player != null) {

                            SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player.getSpigotVariant().resolve());
                            commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Taler für §e" + player.getLoggingName() + " §7liegen bei: §6" + player1.getUserData().getCoins());

                        } else {

                            Connection connection = null;
                            try {

                                connection = XenyriaSpigotServerCore.getDatabaseConnection().getConnection();
                                DatabaseQuery.StoredUserData data = DatabaseQuery.queryUserData(userID, connection, false);
                                if(data != null) {

                                    DatabaseQuery.PlayerVariables variables = new DatabaseQuery.PlayerVariables(data.getUserID());
                                    variables.loadFromMySQL(connection);

                                    if(variables.exists("splatoon.coins")) {

                                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Taler für §e" + data.getUsername() + " (#" + data.getUserID() + ") §7liegen bei: §6" + variables.getInt("splatoon.coins") + " Taler");

                                    } else {

                                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Taler für §e" + data.getUsername() + " (#" + data.getUserID() + ") §7liegen bei: §60 Taler");

                                    }

                                } else {

                                    commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Dieser Spieler ist nicht registriert.");

                                }

                            } catch (Exception e) {

                                throw e;

                            } finally {

                                MySQLUtil.close(connection);

                            }

                        }

                    } catch (Exception e) {

                        XenyriaSpigotServerCore.getXenyriaLogger().error("Fehler beim verarbeiten einer Talerabfrage.", e);

                    }

                }

            } else if (args.length == 3) {

                String subCommand = args[0];
                if(subCommand.equalsIgnoreCase("set")) {

                    ArgumentCastResult result = castToInt(args[1]);
                    ArgumentCastResult coinsResult = castToInt(args[2]);

                    if(!result.isSuccess()) {

                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Die Spieler-ID muss eine Zahl sein!");
                        return true;

                    }
                    if(!coinsResult.isSuccess()) {

                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Die Münzanzahl muss eine Zahl sein!");
                        return true;

                    }

                    try {

                        XenyriaPlayer player = XenyriaPlayer.resolveByID(result.getInt());
                        int newCoins = coinsResult.getInt();
                        if(player != null) {

                            SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player.getSpigotVariant().resolve());
                            player1.getUserData().updateCoins(Math.abs(newCoins));
                            commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Taler für §e" + player.getLoggingName() + " §7gesetzt - Neuer Wert: §6" + newCoins + " Taler");

                        } else {

                            Connection connection = null;
                            try {

                                connection = XenyriaSpigotServerCore.getDatabaseConnection().getConnection();
                                DatabaseQuery.StoredUserData data = DatabaseQuery.queryUserData(result.getInt(), connection, false);
                                if(data != null) {

                                    DatabaseQuery.PlayerVariables variables = new DatabaseQuery.PlayerVariables(data.getUserID());
                                    variables.loadFromMySQL(connection);
                                    BasicVariable variable = new BasicVariable();
                                    variable.setInt(newCoins);
                                    variables.set("splatoon.coins", variable);
                                    variables.saveToMySQL(connection);
                                    // TODO Transmission

                                    commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Taler für §e" + data.getUsername() + " (#" + data.getUserID() + ") §7gesetzt - Neuer Wert: §6" + newCoins + " Taler");

                                } else {

                                    commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Dieser Spieler ist nicht registriert.");

                                }

                            } catch (Exception e) {

                                throw e;

                            } finally {

                                MySQLUtil.close(connection);

                            }

                        }

                    } catch (Exception e) {

                        XenyriaSpigotServerCore.getXenyriaLogger().error("Fehler beim verarbeiten einer Talerbearbeitungsanfrage.", e);

                    }


                }

            } else {

                unknownSubCommand(commandSender);

            }

        } else {

            noPermissions(commandSender);

        }

        return true;

    }

}
