package de.xenyria.splatoon.commands;

import com.destroystokyo.paper.PaperCommand;
import com.destroystokyo.paper.PaperConfig;
import com.destroystokyo.paper.PaperWorldConfig;
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

        //for(int i = 0; i < 20; i++) {

        TurfWarMatch match = new TurfWarMatch();
        match.addAIPlayer("Player1", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player2", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
        match.addAIPlayer("Player3", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
        match.addAIPlayer("Player4", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player5", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Player6", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
        match.addAIPlayer("Player7", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
        match.addAIPlayer("Player8", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        player1.joinMatch(match);
        match.chooseTeam(player1, -1);
        match.start();

        /*}
        TurfWarMatch match = new TurfWarMatch();
        player1.joinMatch(match);
        match.changeOwner(player1);
        match.chooseTeam(player1, -1);
        match.start();*/

        return true;

    }

}
