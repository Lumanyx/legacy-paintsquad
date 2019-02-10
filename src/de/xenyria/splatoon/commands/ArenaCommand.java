package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Chat;
import de.xenyria.schematics.internal.placeholder.SchematicPlaceholder;
import de.xenyria.schematics.internal.placeholder.StoredPlaceholder;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaData;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.MapBoundBuilder;
import de.xenyria.splatoon.arena.boundary.ArenaBoundaryConfiguration;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.match.turfwar.TurfWarMatch;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;

public class ArenaCommand implements CommandExecutor {

    private static BukkitTask arenaBuildTask = null;
    private static ArenaProvider.ArenaGenerationTask task = null;
    private static boolean progress = false;
    private String prefix = "§c[BoundBuilder] §7";

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (commandSender.hasPermission("xenyria.splatoon.admin")) {

            if (args.length == 3) {

                if (args[0].equalsIgnoreCase("build")) {

                    if (!progress) {

                        // build <mapID> <MatchType>
                        int mapID = 0;
                        try {

                            mapID = Integer.parseInt(args[1]);

                        } catch (Exception e) {

                            commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe eine Zahl als Arena-ID an.");
                            return true;

                        }
                        ArenaData data = XenyriaSplatoon.getArenaRegistry().getArenaData(mapID);
                        if (data != null) {

                            MatchType type = null;
                            try {

                                type = MatchType.valueOf(args[2].toUpperCase());

                            } catch (Exception e) {

                                commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Bitte gebe einen gültigen Match-Typ an: §eTURFWAR, RAINMAKER, SPLATZONES");

                            }

                            task = XenyriaSplatoon.getArenaProvider().requestArena(mapID, type);
                            final MatchType copyType = type;

                            progress = true;
                            commandSender.sendMessage(prefix + "Beginne mit dem Bauvorgang. Arena §e" + data.getArenaName());

                            arenaBuildTask = Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

                                try {

                                    if (task.isDone()) {

                                        if (task.isSuccessful()) {

                                            commandSender.sendMessage(prefix + "Die Arena wurde erfolgreich gepastet.");

                                        } else {

                                            commandSender.sendMessage(prefix + "Die Arena konnte nicht gepastet werden.");

                                        }
                                        arenaBuildTask.cancel();

                                        Location location = null;
                                        Vector min1 = null, max1 = null;

                                        for (StoredPlaceholder placeholder : task.getSchematic().getStoredPlaceholders()) {

                                            if (placeholder.type == SchematicPlaceholder.Splatoon.SPAWN_POINT) {

                                                location = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                                                        placeholder.x, placeholder.y, placeholder.z);
                                                location = location.add(task.getOffset());

                                            } else if (placeholder.type == SchematicPlaceholder.Splatoon.MAP_BOUND) {

                                                if (min1 == null) {

                                                    min1 = new Vector(placeholder.x, placeholder.y, placeholder.z).add(task.getOffset());

                                                } else if (max1 == null) {

                                                    max1 = new Vector(placeholder.x, placeholder.y, placeholder.z).add(task.getOffset());

                                                }

                                            }

                                        }

                                        if (location != null) {

                                            if (min1 != null && max1 != null) {

                                                Vector min = new Vector(
                                                        Math.min(min1.getX(), max1.getX()),
                                                        Math.min(min1.getY(), max1.getY()),
                                                        Math.min(min1.getZ(), max1.getZ())
                                                );
                                                Vector max = new Vector(
                                                        Math.max(min1.getX(), max1.getX()),
                                                        Math.max(min1.getY(), max1.getY()),
                                                        Math.max(min1.getZ(), max1.getZ())
                                                );
                                                commandSender.sendMessage(prefix + "Min/Max-Werte bestimmt. Generiere .sbounds-Datei...");
                                                MapBoundBuilder boundBuilder = new MapBoundBuilder(task.getOffset(), location, min, max);
                                                boundBuilder.generatePossibleVectors();

                                                ArenaBoundaryConfiguration configuration = ArenaBoundaryConfiguration.fromMapBounds(boundBuilder, task.getArenaData());

                                                File targetFile = new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + task.getArenaData().getMap().get(copyType) + ".sbounds");
                                                XenyriaSplatoon.getXenyriaLogger().log("Speichere .sbounds: " + task.getArenaData().getArenaName() + " - " + task.getArenaData().getMap().get(copyType));
                                                if (!targetFile.exists()) {

                                                    targetFile.getParentFile().mkdirs();

                                                }
                                                try {

                                                    commandSender.sendMessage(prefix + "Map erstellt!");
                                                    configuration.save(targetFile);

                                                } catch (Exception e) {

                                                    e.printStackTrace();

                                                }

                                            } else {

                                                commandSender.sendMessage(prefix + "Map-Boundaries sind nicht festgelegt!");

                                            }

                                        } else {

                                            commandSender.sendMessage(prefix + "Spawnpunkt in der Schematic nicht gefunden!");

                                        }

                                    }

                                } finally {

                                    progress = false;

                                }

                            }, 1l, 1l);

                            /*
                            Vector minBounds = task.getMin();
                            Vector maxBounds = task.getMax();

                            Location spawnLocation = null;
                            for(StoredPlaceholder placeholder : task.getSchematic().getStoredPlaceholders()) {

                                if(placeholder.type == SchematicPlaceholder.Splatoon.SPAWN_POINT) {

                                    spawnLocation = new Location(XenyriaSplatoon.getArenaProvider().getArenaWorld(),
                                            (int)task.getOffset().getX() + placeholder.x,
                                            (int)task.getOffset().getY() + placeholder.y,
                                            (int)task.getOffset().getZ() + placeholder.z);
                                    break;

                                }

                            }

                            MapBoundBuilder boundBuilder = new MapBoundBuilder(task.getOffset(), spawnLocation,
                                    minBounds, maxBounds);
                            boundBuilder.generatePossibleVectors();

                            ArenaBoundaryConfiguration configuration = ArenaBoundaryConfiguration.fromMapBounds(boundBuilder, task.getArenaData());

                            File targetFile = new File(XenyriaSplatoon.getPlugin().getDataFolder() + File.separator + "arena" + File.separator + "boundaries" + File.separator + task.getArenaData().getMap().get(type) + ".sbounds");
                            if (!targetFile.exists()) {

                                targetFile.getParentFile().mkdirs();

                            }
                            try {

                                commandSender.sendMessage(prefix + "Map erstellt!");
                                configuration.save(targetFile);

                            } catch (Exception e) {

                                e.printStackTrace();

                            }*/

                        } else {

                            commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Eine Arena mit der §eID " + mapID + " §7ist nicht registriert.");

                        }

                    } else {

                        commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Eine Mapbuild-Anfrage ist momentan aktiv. Bitte warte einen Augenblick.");

                    }

                } else {

                    commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Unbekannter Unterbefehl!");

                }

            } else {

                commandSender.sendMessage(Chat.SYSTEM_PREFIX + "Unbekannter Unterbefehl!");

            }

        } else {

            commandSender.sendMessage(Chat.NO_PERMISSIONS);

        }

        /*
        Player player = ((Player)commandSender);
        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
        TurfWarMatch match = new TurfWarMatch();
        player1.joinMatch(match);
        match.start();*/

        return true;

    }


}
