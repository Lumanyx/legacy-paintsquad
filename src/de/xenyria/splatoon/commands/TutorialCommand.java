package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.tutorial.TutorialMatch;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender instanceof Player) {

            Player player = ((Player)commandSender);
            TutorialMatch match = XenyriaSplatoon.getTutorialManager().getFreeCluster();
            SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
            if(player1.getMatch() != null) {

                player1.getMatch().removePlayer(player1);

            }

            player1.joinMatch(match);
            player.sendMessage(Chat.SYSTEM_PREFIX + "Du trittst dem Tutorial bei.");
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () ->
            {

                //player.teleport(new Location(Bukkit.getWorld("sp_tutorial"), 135.5, 85, 186.5));
                //player.setGameMode(GameMode.CREATIVE);

            }, 20l);

        } else {

            commandSender.sendMessage(Chat.MUST_BE_PLAYER);

        }

        return true;

    }
}
