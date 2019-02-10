package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SuicideCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender instanceof Player) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(((Player)commandSender));
            player.splat(player.getTeam().getColor(), null, null);

        } else {

            commandSender.sendMessage(Chat.MUST_BE_PLAYER);

        }

        return true;

    }

}
