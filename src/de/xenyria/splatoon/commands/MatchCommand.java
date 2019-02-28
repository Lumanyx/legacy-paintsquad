package de.xenyria.splatoon.commands;

import com.destroystokyo.paper.PaperCommand;
import com.destroystokyo.paper.PaperConfig;
import com.destroystokyo.paper.PaperWorldConfig;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.turfwar.TurfWarMatch;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MatchCommand extends SpigotCommand implements CommandExecutor {

    public MatchCommand() {
        super("match", "Erlaubt die Verwaltung eines Matches", "xenyria.splatoon.match");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender.hasPermission("xenyria.splatoon.match")) {

            if(args.length == 0) {

                commandSender.sendMessage("§8" + Characters.ARROW_RIGHT_FROM_TOP + " §aXenyriaSplatoon §8| §7Matchverwaltung");
                commandSender.sendMessage("§b/match cancel §8| §7Bricht das Match ab, in dem du dich befindest.");
                commandSender.sendMessage("§b/match createprivate §8| §7Erstellt einen leeren Privatraum.");

            } else if(args.length == 1) {

                String arg = args[0];
                if(arg.equalsIgnoreCase("cancel")) {

                    if (isPlayer(commandSender)) {

                        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(castToPlayer(commandSender));
                        Match match = player.getMatch();
                        if (match instanceof BattleMatch) {

                            BattleMatch match1 = (BattleMatch) match;
                            match1.cancel();
                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Match abgebrochen.");

                        } else {

                            player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du kannst das aktuelle Match nicht abbrechen da es sich um kein Kampfmatch handelt.");

                        }

                    } else {

                        unknownSubCommand(commandSender);

                    }

                } else if(arg.equalsIgnoreCase("createprivate")) {

                    if (isPlayer(commandSender)) {

                        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(castToPlayer(commandSender));
                        //BattleMatch match1 = XenyriaSplatoon.getMatchManager()

                    } else {

                        unknownSubCommand(commandSender);

                    }


                } else {

                    unknownSubCommand(commandSender);

                }

            }

        } else {

            noPermissions(commandSender);

        }

        Player player = ((Player)commandSender);
        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

        //for(int i = 0; i < 20; i++) {


        /*match.addAIPlayer("Spieler1", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Spieler2", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
        match.addAIPlayer("Spieler3", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
        match.addAIPlayer("Spieler4", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Spieler5", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
        match.addAIPlayer("Spieler6", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
        match.addAIPlayer("Spieler7", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
        match.addAIPlayer("Spieler8", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);*/

        TurfWarMatch match = new TurfWarMatch();
        match.selectMap(1);
        player1.joinMatch(match);

        match.chooseTeam(player1, 0);
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
