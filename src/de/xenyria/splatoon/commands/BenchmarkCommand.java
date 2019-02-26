package de.xenyria.splatoon.commands;

import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.splatoon.ai.entity.AIProperties;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.match.turfwar.TurfWarMatch;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BenchmarkCommand extends SpigotCommand implements CommandExecutor {

    public BenchmarkCommand() {
        super("benchmark", "Testbefehl", "xenyria.splatoon.admin");
    }

    public void broadcast(String str) { Bukkit.broadcastMessage("§4Benchmark §8> §7" + str); }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender.hasPermission("xenyria.splatoon.admin")) {

            int i = 35;
            broadcast("Benchmark wird gestartet. Generiere §c" + i + " Matches §7mit §e8 Spielern§7...");

            for(int y = 0; y < i; y++) {

                TurfWarMatch match = new TurfWarMatch();
                match.addAIPlayer("Spieler1", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
                match.addAIPlayer("Spieler2", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
                match.addAIPlayer("Spieler3", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
                match.addAIPlayer("Spieler4", 0, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
                match.addAIPlayer("Spieler5", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
                match.addAIPlayer("Spieler6", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.ROLLER);
                match.addAIPlayer("Spieler7", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.CHARGER);
                match.addAIPlayer("Spieler8", 1, AIProperties.Difficulty.HARD, AIWeaponManager.AIPrimaryWeaponType.SHOOTER);
                match.start();


                broadcast("§eMatch #" + y + " §7erstellt.");

            }

            new Thread(() -> {

                while (true) {

                    try {

                        Thread.sleep(2000);

                        Runtime runtime = Runtime.getRuntime();
                        long freeMem = (runtime.freeMemory() / 1024) / 1024;
                        long maxMem = (runtime.maxMemory() / 1024) / 1024;
                        long used = (runtime.totalMemory() / 1024) / 1024;

                        broadcast("§cMemStats | Total: " + used + ", Max: " + maxMem + ", Free: " + freeMem + " | NPCs: " + EntityNPC.getNPCs().size() + " / TPS: " + Bukkit.getTPS()[0]);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }).start();

        } else {

            noPermissions(commandSender);

        }

        return true;

    }

}
