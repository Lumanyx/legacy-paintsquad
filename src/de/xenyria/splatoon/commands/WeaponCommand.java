package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class WeaponCommand implements CommandExecutor {

    private enum WeaponType {

        PRIMARY, SECONDARY, SPECIAL;

    }

    public void listWeapons(Player player, WeaponType type, int page) {

        Collection<SplatoonWeapon> weapons = SplatoonWeaponRegistry.getWeapons();
        ArrayList<SplatoonWeapon> visible = new ArrayList<>();
        for(SplatoonWeapon weapon : weapons) {

            WeaponType currentType = WeaponType.PRIMARY;
            if(weapon instanceof SplatoonSecondaryWeapon) { currentType = WeaponType.SECONDARY; }
            if(weapon instanceof SplatoonSpecialWeapon) { currentType = WeaponType.SPECIAL; }
            if(currentType.equals(type)) {

                visible.add(weapon);

            }

        }
        SplatoonWeapon[] weapons1 = weapons.toArray(new SplatoonWeapon[]{});
        if(page < 1) { page = 1; }

        int min = (page * 8) - 8;
        int max = min + 8;
        player.sendMessage(Chat.SYSTEM_PREFIX + "Waffenauflistung §8- §7Typ: §e" + type.name() + " §8/ §7Seite §e" + page);
        for(int i = min; i < max; i++) {

            if(i <= (visible.size() - 1)) {

                SplatoonWeapon weapon = visible.get(i);
                player.sendMessage("§8" + Characters.SMALL_X + " §6#" + weapon.getID() + " §e" + weapon.getName());

            } else {

                player.sendMessage("§8" + Characters.SMALL_X + " §7Leer");

            }

        }

    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender instanceof Player) {

            Player player = (Player)commandSender;
            if(player.hasPermission("xenyria.splatoon.weapon")) {

                if(args.length == 0) {

                    player.sendMessage(Chat.SYSTEM_PREFIX + "Splatoon-Waffenbefehl");
                    player.sendMessage("§7Waffentypen: §ePRIMARY, SECONDARY, SPECIAL");
                    player.sendMessage("§7Registrierte Klassen: §e" + SplatoonWeaponRegistry.getRegisterCount());
                    player.sendMessage("§7Waffe geben: §e/weapon give <ID>");
                    player.sendMessage("§7Waffen auflisten: §e/weapon list <Typ> [Seite]");

                } else if(args.length == 2) {

                    if(args[0].equalsIgnoreCase("give")) {

                        int id = 1;
                        try {

                            id = Integer.parseInt(args[1]);

                        } catch (Exception e) {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe eine Zahl als Parameter an!");
                            return true;

                        }

                        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        if(player1.getTeam() == null) {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Du bist derzeit keinem Team zugeteilt. Daher kannst du dir keine Waffe geben.");
                            return true;

                        }

                        Class<SplatoonWeapon> weapon = SplatoonWeaponRegistry.getWeaponClass(id);
                        if(weapon != null) {

                            SplatoonWeapon dummy = SplatoonWeaponRegistry.getDummy(id);
                            if(dummy instanceof SplatoonPrimaryWeapon) {

                                player1.getEquipment().setPrimaryWeapon(id);

                            } else if(dummy instanceof SplatoonSecondaryWeapon) {

                                player1.getEquipment().setSecondaryWeapon(id);

                            } else {

                                player1.getEquipment().setSpecialWeapon(id);

                            }
                            player.sendMessage(Chat.SYSTEM_PREFIX + "§6" + dummy.getName() + " §7erhalten!");

                        } else {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Eine Waffe mit der angegebenen ID kann nicht gefunden werden.");

                        }

                    } else if (args[0].equalsIgnoreCase("list")) {

                        String enumName = args[1];
                        WeaponType type = null;
                        try {

                            type = WeaponType.valueOf(enumName.toUpperCase());

                        } catch (Exception e) {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Unbekannter Waffentyp. Nutze §ePRIMARY, SECONDARY oder SPECIAL");
                            return true;

                        }

                        listWeapons(player, type, 1);

                    }

                } else if(args.length == 3) {

                    String enumName = args[1];
                    WeaponType type = null;
                    try {

                        type = WeaponType.valueOf(enumName.toUpperCase());

                    } catch (Exception e) {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "Unbekannter Waffentyp. Nutze §ePRIMARY, SECONDARY oder SPECIAL");
                        return true;

                    }
                    int page = 1;
                    try {

                        page = Integer.parseInt(args[2]);

                    } catch (Exception e) {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe eine Zahl als Parameter an.");
                        return true;

                    }

                    listWeapons(player, type, page);

                } else {

                    player.sendMessage(Chat.SYSTEM_PREFIX + "Unbekannter Unterbefehl!");

                }

            } else {

                player.sendMessage(Chat.NO_PERMISSIONS);

            }

        } else {

            commandSender.sendMessage(Chat.MUST_BE_PLAYER);

        }

        return true;

    }

}
