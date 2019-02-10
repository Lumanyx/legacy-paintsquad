package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SplatoonLobbyCommand extends SpigotCommand implements CommandExecutor {

    public SplatoonLobbyCommand() {
        super("slobby", "Teleportiert dich in die Splatoon-Lobby", "");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if(isPlayer(commandSender)) {

            Player player = (Player)commandSender;
            SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
            if(player1.getMatch() != null) {

                if(!player1.getMatch().equals(XenyriaSplatoon.getLobbyManager().getLobby())) {

                    XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player1);
                    player.sendMessage(Chat.SYSTEM_PREFIX + "Du bist nun in der Splatoon Lobby.");
                    player.sendMessage("§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Bedenke, dass das absichtliche Verlassen eines öffentlichen §7Kampfes §7bestraft §7werden §7kann.");

                } else {

                    player.sendMessage(Chat.SYSTEM_PREFIX + "Du bist bereits in der Splatoon Lobby.");

                }

            }

        } else {

            notAPlayer(commandSender);

        }

        return true;

    }

}
