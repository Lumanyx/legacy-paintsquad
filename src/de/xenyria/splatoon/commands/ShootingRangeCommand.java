package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.shootingrange.ShootingRange;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShootingRangeCommand extends SpigotCommand implements CommandExecutor {

    public ShootingRangeCommand() {
        super("sr", "Teleportiert dich zum Waffentestbereich.", "");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(isPlayer(commandSender)) {

            Player player = castToPlayer(commandSender);
            SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
            Match match = player1.getMatch();
            if(match instanceof SplatoonLobby) {

                XenyriaSplatoon.getShootingRangeManager().joinShootingRange(player1);

            } else if(match instanceof ShootingRange) {

                player.sendMessage(Chat.SYSTEM_PREFIX + "Du bist bereits in einer Waffentestumgebung!");

            } else {

                player.sendMessage(Chat.SYSTEM_PREFIX + "Du musst dich in der Lobby befinden um den Testbereich zu betreten.");

            }

        } else {

            notAPlayer(commandSender);

        }

        return true;

    }

}
