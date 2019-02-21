package de.xenyria.splatoon.commands;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.NoteBlockSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.camera.CinematicCamera;
import de.xenyria.servercore.spigot.camera.CinematicSequence;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.NavigationManager;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.LaunchPad;
import de.xenyria.splatoon.game.objects.SuctionBomb;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.BurstBombProjectile;
import de.xenyria.splatoon.game.projectile.SprinklerProjectile;
import de.xenyria.splatoon.game.projectile.SuctionBombProjectile;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.sound.MusicTrack;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.tutorial.TutorialMatch;
import net.minecraft.server.v1_13_R2.*;
import net.minecraft.server.v1_13_R2.World;
import org.bukkit.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.util.*;

public class AIDebugCommand implements CommandExecutor {

    private HashMap<UUID, Location> pos1 = new HashMap<>(), pos2 = new HashMap<>();
    private ArrayList<ArmorStand> armorStands = new ArrayList<>();

    public static final Location IA3_PLAYER_SPAWN = new Location(Bukkit.getWorld("world"), -32.5, 87, 13.5, -45, 0f);
    public static final Location IA3_AI_SPAWN = new Location(Bukkit.getWorld("world"), -21.5, 87, 24.5, 135f, 0f);

    public static float val = 0f;
    public static int tick = 0;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender instanceof Player) {

            if(commandSender.hasPermission("xenyria.splatoon.aidbg")) {

                Player player = (Player)commandSender;
                if(args.length == 1) {

                    if (args[0].equalsIgnoreCase("1")) {

                        pos1.put(player.getUniqueId(), player.getLocation().getBlock().getLocation().add(0.5, 0, .5));
                        player.sendMessage("EyeLOC weapons OK!");

                    } else if(args[0].equalsIgnoreCase("2")) {

                        pos2.put(player.getUniqueId(), player.getLocation().getBlock().getLocation().add(0.5, 0, .5));
                        player.sendMessage("BODY POS weapons - OK!");

                    } else if(args[0].equalsIgnoreCase("music")) {

                        MusicTrack[] tracks = XenyriaSplatoon.getMusicManager().getTrackList(1);
                        RadioSongPlayer player1 = new RadioSongPlayer(new Playlist(tracks[0].getSong()));
                        player1.addPlayer(player);
                        player1.setVolume((byte) 100);
                        player1.setPlaying(true);

                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            player1.setTick(tracks[0].getSong().getLength());
                            player1.setRepeatMode(RepeatMode.NO);
                            player1.setPlaylist(new Playlist(tracks[1].getSong()));
                            player1.playSong(0);
                            player1.setTick((short) 0);
                            player1.setPlaying(true);

                        }, 40l);

                    } else if(args[0].equalsIgnoreCase("dir")) {

                        Scoreboard scoreboard = player.getScoreboard();
                        for(org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {

                            player.sendMessage(team.getName() + "...");
                            team.unregister();

                        }

                    } else if(args[0].equalsIgnoreCase("lm")) {

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        BattleMatch match = (BattleMatch) player1.getMatch();
                        match.setMatchTicks(20*3);
                        player.sendMessage("Last minute");

                    } else if(args[0].equalsIgnoreCase("special")) {

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        player1.setSpecialPoints(9999);

                    } else if(args[0].equalsIgnoreCase("endtut")) {

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        Match match = player1.getMatch();
                        if(match != null && match instanceof TutorialMatch) {

                            TutorialMatch match1 = (TutorialMatch)match;
                            for(GameObject object : match1.getGameObjects()) {

                                if(object instanceof LaunchPad) {

                                    LaunchPad pad = (LaunchPad)object;
                                    player.teleport(pad.getLocation());

                                }

                            }

                        }

                    } else if(args[0].equalsIgnoreCase("cap")) {

                        /*
                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                                new PacketPlayOutAbilities(((CraftPlayer) player).getHandle().abilities)
                        );*/

                        /*
                        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
                        stand.setVisible(false);
                        stand.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 2, 99999, false, false, false));
                        player.setGameMode(GameMode.SPECTATOR);

                        player.sendMessage("ok!");*/

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(player.getEyeLocation(),
                                player.getLocation().add(player.getLocation().getDirection().clone().multiply(7.5)), 0.72d, player1.getMatch(),
                                SplatoonHumanPlayer.getPlayer(player).getTeam(), null, SplatoonHumanPlayer.getPlayer(player), .72);
                        SprinklerProjectile projectile = new SprinklerProjectile(player1, player1.getEquipment().getPrimaryWeapon(), player1.getMatch());
                        projectile.spawn(result.getTrajectory(), player.getLocation(), result.getHitLocation());

                    } else if(args[0].equalsIgnoreCase("outro")) {

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        if(player1.getMatch().getAllPlayers().size() != 4) {

                            Team team = player1.getTeam();
                            Location location = player1.getMatch().getSpawnPoint(player1);

                            player1.setSpawnPoint(location);
                            for(int i = 0; i < 3; i++) {

                                Location location1 = player1.getMatch().getNextSpawnPoint(team);
                                EntityNPC npc1 = new EntityNPC("test", location1, player1.getTeam(), player1.getMatch());
                                npc1.disableAI();
                                npc1.getEquipment().setPrimaryWeapon(1);
                                npc1.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

                            }

                        }
                        player1.getEquipment().setPrimaryWeapon(4);


                        player1.getMatch().initOutroManager();
                        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), player1.getMatch().getOutroManager()::tick, 1l, 1l);

                    } else if(args[0].equalsIgnoreCase("intro")) {

                        SplatoonHumanPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

                        if(player1.getMatch().getAllPlayers().size() != 4) {

                            Team team = player1.getTeam();
                            Location location = player1.getMatch().getSpawnPoint(player1);

                            player1.setSpawnPoint(location);
                            for(int i = 0; i < 3; i++) {

                                Location location1 = player1.getMatch().getNextSpawnPoint(team);
                                EntityNPC npc1 = new EntityNPC("test", location1, player1.getTeam(), player1.getMatch());
                                npc1.disableAI();
                                npc1.getEquipment().setPrimaryWeapon(1);
                                npc1.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

                            }

                        }

                        player1.getEquipment().setPrimaryWeapon(4);
                        int i = 0;
                        for(Team team : player1.getMatch().getRegisteredTeams()) {

                            player1.getMatch().getMap().pasteSpawn(player1.getMatch().getWorld(),
                                    player1.getMatch().getMap().getSpawns().get(i),
                                    team);

                        }

                        player1.setSpawnPoint(player1.getMatch().getSpawnPoint(player1));
                        //player.teleport(player1.getSpawnPoint());
                        player1.getMatch().initIntroManager();
                        player1.getMatch().getIntroManager().initializeSequence();
                        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), player1.getMatch().getIntroManager()::tick, 1l, 1l);
                        /*new Thread(() -> {

                            while (true) {

                                try { Thread.sleep(1000 / 60); } catch (Exception e) {}

                                val+=0.9f;
                                tick++;
                                if(tick < 5) {

                                    HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<>();
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
                                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                                            new PacketPlayOutPosition(0, 0, 0, 0.9f, 0f, flags, 0)
                                    );

                                } else {
                                    HashSet<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<>();
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z);
                                    flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
                                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                                            new PacketPlayOutPosition(0, 0, 0, val, 0f, flags, 0)
                                    );

                                    tick = 0;

                                }

                            }


                        }).start();*/

                    } else if(args[0].equalsIgnoreCase("spawn")) {

                        /*for(de.xenyria.splatoon.game.map.Map.TeamSpawn.PositionWithMaterial material : de.xenyria.splatoon.game.map.Map.TeamSpawn.getSpawnSchematic()) {

                            Vector playerPos = new Vector(
                                    (int)player.getLocation().getX(),
                                    (int)player.getLocation().getY(),
                                    (int)player.getLocation().getZ());

                            playerPos = playerPos.add(material.relativePosition);
                            player.getWorld().getBlockAt(playerPos.getBlockX(), playerPos.getBlockY(), playerPos.getBlockZ()).setType(material.material);

                        }*/

                    } else if(args[0].equalsIgnoreCase("nodes")) {

                        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        player1.getMatch().getAIController().initSpots(player1.getMatch().getAIController().gatherNodesBySpawns());
                        for (Node node : player1.getMatch().getAIController().getFoundNodes()) {

                            player.spawnParticle(Particle.SPELL_INSTANT, node.toVector().toLocation(player.getWorld()), 0);

                        }

                    } else if(args[0].equalsIgnoreCase("bomb")) {

                        BombProjectile projectile = new BombProjectile(SplatoonHumanPlayer.getPlayer(player),
                                null, SplatoonHumanPlayer.getPlayer(player).getMatch(), 50f, 20, 0, false);
                        projectile.spawn(0, player.getLocation());

                    } else if(args[0].equalsIgnoreCase("spots")) {

                        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        for(PaintableRegion region : player1.getMatch().getAIController().getPaintableRegions()) {

                            ArrayList<Material> materials = new ArrayList<>();
                            for(Material material : Material.values()) {

                                if(material.name().toUpperCase().endsWith("_STAINED_GLASS")) {

                                    materials.add(material);

                                }

                            }


                            player.sendBlockChange(region.getCenter().toLocation(player1.getMatch().getWorld()), CraftBlockData.newData(Material.OAK_PLANKS, ""));
                            Material material = materials.get(new Random().nextInt(materials.size() - 1));
                            System.out.println(region.getCenter());
                            for(Block block : region.getPaintableBlocks(player1.getColor())) {

                            }

                        }

                    } else if(args[0].equalsIgnoreCase("ia3")) {

                        //player.teleport(new Location(Bukkit.getWorld("world"), -31.5, 87, 14.5));


                        //player1.setSpawnPoint(player1.getMatch().getSpawnPoint(player1));

                        /*player.teleport(IA3_PLAYER_SPAWN);

                        player1.setSpawnPoint(IA3_PLAYER_SPAWN);
                        EntityNPC npc = new EntityNPC(IA3_AI_SPAWN, SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(1), SplatoonHumanPlayer.getPlayer(player).getMatch());
                        npc.getEquipment().setPrimaryWeapon(4);
                        npc.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());*/

                        /*de.xenyria.splatoon.game.map.Map.TeamSpawn spawn = player1.getMatch().getMap().getSpawns().get(0);

                        Vector[] offsets = de.xenyria.splatoon.game.map.Map.TeamSpawn.getSpawnPositionOffsets(spawn.getDirection());
                        player1.getMatch().getMap().pasteSpawn(player.getWorld(), spawn, SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(2));
                        //Vector[] offsets = new Vector[]{new Vector(-1, 0, 0), new Vector(1, 0, 0), new Vector(0, 0, 1), new Vector(0, 0, -1)};
                        for(Vector vector : offsets) {

                            EntityNPC npc1 = new EntityNPC(SplatoonHumanPlayer.getPlayer(player).getMatch().getMap().getSpawns().get(0).getPosition().clone().add(vector), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(2), SplatoonHumanPlayer.getPlayer(player).getMatch());
                            npc1.disableAI();
                            npc1.getEquipment().setPrimaryWeapon(4);
                            npc1.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());
                            //npc1.superJump(player.getLocation(), 27);

                        }

                        player.setGameMode(GameMode.SPECTATOR);
                        player.teleport(player1.getMatch().getMap().getSpawns().get(0).introVector(player.getWorld()));
                        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                                new PacketPlayOutAbilities(((CraftPlayer)player).getHandle().abilities)
                        );

                        /*for(Vector vector : offsets) {

                            EntityNPC npc1 = new EntityNPC(SplatoonHumanPlayer.getPlayer(player).getMatch().getMap().getSpawns().get(1).getPosition().clone().add(vector), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(3), SplatoonHumanPlayer.getPlayer(player).getMatch());
                            npc1.disableAI();
                            npc1.getEquipment().setPrimaryWeapon(4);
                            npc1.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());
                            //npc1.superJump(player.getLocation(), 27);

                        }
                        /*for(Vector vector : offsets) {

                            EntityNPC npc1 = new EntityNPC(SplatoonHumanPlayer.getPlayer(player).getMatch().getMap().getSpawns().get(1).getPosition().clone().add(vector), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(0), SplatoonHumanPlayer.getPlayer(player).getMatch());
                            npc1.getEquipment().setPrimaryWeapon(1);
                            npc1.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

                        }*/
                        /*EntityNPC npc2 = new EntityNPC(new Location(Bukkit.getWorld("world"), -24.5, 87, 47, 90, 0f), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(0), SplatoonHumanPlayer.getPlayer(player).getMatch());
                        npc2.getEquipment().setPrimaryWeapon(1);
                        npc2.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

                        EntityNPC npc3 = new EntityNPC(new Location(Bukkit.getWorld("world"), -24.5, 87, 47, 90, 0f), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(2), SplatoonHumanPlayer.getPlayer(player).getMatch());
                        npc3.getEquipment().setPrimaryWeapon(1);
                        npc3.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

                        EntityNPC npc4 = new EntityNPC(new Location(Bukkit.getWorld("world"), -27.5, 66, 4.5, 0, 0), SplatoonHumanPlayer.getPlayer(player).getMatch().getRegisteredTeams().get(3), SplatoonHumanPlayer.getPlayer(player).getMatch());
                        npc4.getEquipment().setPrimaryWeapon(1);
                        npc4.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());

/*
                        player.sendMessage(npc2.getNMSEntity().getHeadHeight() + " < height");

                        npc2.joinMatch(SplatoonHumanPlayer.getPlayer(player).getMatch());*/

                        /*Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            BombProjectile projectile = new BombProjectile(npc1, null, npc1.getMatch(), 40f, 0, 1, false);
                            projectile.spawn(0, player.getLocation());

                        }, 20l);*/

                        player.sendMessage("OK!");

                    } else if(args[0].equalsIgnoreCase("find")) {

                        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        Location location = pos1.get(player.getUniqueId());
                        Location target = pos2.get(player.getUniqueId());

                        PathfindingTarget target1 = new PathfindingTarget() {
                            @Override
                            public boolean needsUpdate(Vector vector) {
                                return false;
                            }

                            @Override
                            public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {
                                return node.equals(pathfinder.getGoalNode());
                            }

                            @Override
                            public boolean useGoalNode() {
                                return true;
                            }

                            @Override
                            public SquidAStar.MovementCapabilities getMovementCapabilities() {
                                return new SquidAStar.MovementCapabilities();
                            }

                            @Override
                            public void beginPathfinding() {

                            }

                            @Override
                            public void endPathfinding() {

                            }

                            @Override
                            public int maxNodeVisits() {
                                return 100;
                            }

                            @Override
                            public NodeListener getNodeListener() {
                                return new NodeListener() {
                                    @Override
                                    public boolean isPassable(Node node, int nX, int nY, int nZ) {
                                        return true;
                                    }

                                    @Override
                                    public boolean useAlternativeTargetCheck() {
                                        return false;
                                    }

                                    @Override
                                    public double getAdditionalWeight(Node node) {

                                        /*double distanceToTarget = node.toVector().distance(player.getLocation().toVector());
                                        if(distanceToTarget < 10) {

                                            return (10-distanceToTarget) * 5;

                                        }*/
                                        return 0d;

                                    }

                                    @Override
                                    public Node getBestNodeFromRemaining(Node[] nodes) {

                                        double highestDist = 0d;
                                        Node lastNode = null;
                                        for(Node node : nodes) {

                                            if (lastNode == null || node.toVector().distance(player.getLocation().toVector()) > highestDist) {

                                                highestDist = node.toVector().distance(player.getLocation().toVector());
                                                lastNode = node;

                                            }

                                        }
                                        return lastNode;

                                    }

                                };
                            }

                            @Override
                            public Vector getEstimatedPosition() {
                                return target.toVector();
                            }
                        };

                        SquidAStar aStar = new SquidAStar(player.getWorld(), location.toVector(), target1, player1.getMatch(), player1.getTeam(), 9000);
                        long start = System.nanoTime();
                        aStar.beginProcessing();
                        long end = System.nanoTime();

                        double time = (end - start) / 1000000f;

                        if(aStar.getRequestResult() == SquidAStar.RequestResult.FOUND) {

                            player.sendMessage("§aSquidAStar: Gefunden: " + time + " ms");
                            NodePath path = aStar.getNodePath();
                            if(!armorStands.isEmpty()) {

                                for(ArmorStand stand : armorStands) {

                                    stand.remove();

                                }
                                armorStands.clear();

                            }

                            int i = 0;
                            for(NodePath.NodePosition position : path.getNodes()) {

                                ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(position.toLocation(player.getWorld()), EntityType.ARMOR_STAND);
                                stand.setCustomNameVisible(true);
                                stand.setCustomName("§e#" + i + ": " + position.getType());
                                stand.setVisible(false);
                                stand.setCanMove(false);
                                stand.setCanTick(false);

                                if(!position.getData().isEmpty()) {

                                    for(Map.Entry<String, Object> entry : position.getData().entrySet()) {

                                        stand.setCustomName(stand.getCustomName() + " " + entry.getKey() + " " + entry.getValue());

                                    }

                                }
                                armorStands.add(stand);

                                i++;

                            }

                            if(aStar.getRequestResult() == SquidAStar.RequestResult.FOUND) {

                                NavigationManager.DEBUG_PATH = aStar.getNodePath();

                            }


                        } else {

                            player.sendMessage("§c" + aStar.getRequestResult() + " -> " + time);

                        }


                    }

                } else if(args.length == 2) {

                    if(args[0].equalsIgnoreCase("test")) {

                        double impulse = Double.parseDouble(args[1]);
                        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);
                        Location l1 = pos1.get(player.getUniqueId());
                        Location l2 = pos2.get(player.getUniqueId());

                        ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(l1, l2, impulse, player1.getMatch(), player1.getTeam(), player1);
                        player.sendMessage("Resultat - Treffer? §e" + result.isTargetReached() + " | impulse " + impulse);

                        InkProjectile projectile = new InkProjectile(player1, null, player1.getMatch());
                        if(result.getTrajectory() != null) {

                            //projectile.spawn(result.getTrajectory(), l1);
                            player1.getMatch().queueProjectile(projectile);

                        }

                        if(result.getHitLocation() != null) {

                            for(int i = 0; i < 20; i++) {

                                Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                    player.spawnParticle(Particle.END_ROD, result.getHitLocation(), 0);

                                }, i * 2);

                            }

                        }

                    }

                }

            } else {

                commandSender.sendMessage(Chat.NO_PERMISSIONS);

            }

        } else {

            commandSender.sendMessage(Chat.MUST_BE_PLAYER);

        }

        return true;

    }

}
