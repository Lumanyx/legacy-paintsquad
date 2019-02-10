package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Characters;
import de.xenyria.servercore.spigot.commands.SpigotCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SplatoonCoinsCommand extends SpigotCommand implements CommandExecutor {

    public SplatoonCoinsCommand() {
        super("scoins", "Befehl zum Anpassen der Taler.", "xenyria.splatoon.admin");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender.hasPermission("xenyria.splatoon.admin")) {

            if(args.length == 0) {

                commandSender.sendMessage("§8" + Characters.FAT_ARROW_RIGHT + " §aSplatoon Talerverwaltungsbefehl");
                commandSender.sendMessage("§8- §e/scoins set <ID> <Anzahl>");
                commandSender.sendMessage("§8- §e/scoins add <ID> <Anzahl>");
                commandSender.sendMessage("§8- §e/scoins sub <ID> <Anzahl>");
                commandSender.sendMessage("§8- §e/scoins get <ID>");

            }

        } else {

            noPermissions(commandSender);

        }

        return true;

    }

}
