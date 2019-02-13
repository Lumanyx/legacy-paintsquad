package de.xenyria.splatoon.commands;

import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.match.turfwar.TurfWarMatch;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MatchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        Player player = ((Player)commandSender);
        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
        TurfWarMatch match = new TurfWarMatch();
        match.chooseTeam(player1, 0);
        match.addAIPlayer("Player1", 0, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player2", 0, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player3",0, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player4", 1, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player5", 1, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player6", 1, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player7", 1, AIProperties.Difficulty.EASY, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        //match.start();
        player1.joinMatch(match);

        return true;

    }

}
