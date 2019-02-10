package de.xenyria.splatoon.commands;

import de.xenyria.servercore.spigot.commands.SpigotCommand;
import de.xenyria.splatoon.game.equipment.weapon.WeaponPositionOffset;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DetermineShootVectorCommand extends SpigotCommand implements CommandExecutor {

    public DetermineShootVectorCommand() {
        super("dsv", "Befehl zum Testen von Vektoren für Waffen.", "xenyria.splatoon.admin");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (isPlayer(commandSender)) {

            Player player = castToPlayer(commandSender);
            if(player.hasPermission("xenyria.splatoon.admin")) {

                if(args.length == 4) {

                    ArgumentCastResult result1 = castToDouble(args[0]);
                    ArgumentCastResult result2 = castToDouble(args[1]);
                    ArgumentCastResult result3 = castToDouble(args[2]);
                    ArgumentCastResult result4 = castToDouble(args[3]);

                    if(result1.isSuccess() && result2.isSuccess() && result3.isSuccess() && result4.isSuccess()) {

                        double depth = result1.getDouble();
                        double x_offset = result2.getDouble();
                        double y_offset = result3.getDouble();
                        double z_offset = result4.getDouble();

                        WeaponPositionOffset offset = new WeaponPositionOffset(false, depth, x_offset, y_offset, z_offset);
                        Vector vector = player.getEyeLocation().toVector();
                        vector = vector.add(offset.getOffset(player.getLocation().getYaw(), player.getLocation().getPitch()));

                        player.spawnParticle(Particle.END_ROD, vector.getX(), vector.getY(), vector.getZ(), 0);

                    }

                } else {

                    unknownSubCommand(player);

                }

            } else {

                noPermissions(commandSender);

            }

        } else {

            notAPlayer(commandSender);

        }

        return true;
    }
}
