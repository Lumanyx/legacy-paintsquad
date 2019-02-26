package de.xenyria.splatoon.commands;

import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.projectile.ProjectileExaminer;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ProjectileExaminerCommand implements CommandExecutor {

    private Location a,b;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if(commandSender instanceof Player) {

            if(commandSender.hasPermission("xenyria.splatoon.unbranded")) {

                Player player = (Player)commandSender;
                if(args.length == 1) {

                    if(args[0].equalsIgnoreCase("1")) {

                        player.sendMessage(Chat.SYSTEM_PREFIX + "Standort festgelegt!");
                        a = new Location(player.getWorld(), ((int)player.getEyeLocation().getX()) + .5,
                                (player.getEyeLocation().getY()),
                                ((int)player.getEyeLocation().getZ()) + .5);

                    } else if(args[0].equalsIgnoreCase("2")) {

                        Vector direction = player.getEyeLocation().getDirection();
                        Location start = player.getEyeLocation();
                        RayTraceResult result = player.getWorld().rayTraceBlocks(start, direction, 5d);
                        if(result != null && result.getHitBlock() != null) {

                            Block block = result.getHitBlock();
                            World world = ((CraftWorld)player.getWorld()).getHandle();
                            IBlockData data = world.getType(new BlockPosition(block.getX(), block.getY(), block.getZ()));
                            player.sendBlockChange(result.getHitBlock().getLocation(), CraftBlockData.newData(Material.BLACK_STAINED_GLASS, ""));
                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                player.sendBlockChange(result.getHitBlock().getLocation(), CraftBlockData.fromData(data));

                            }, 20l);
                            player.sendMessage(Chat.SYSTEM_PREFIX + "OK!");
                            b = result.getHitBlock().getLocation();

                        } else {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Keinen Block gefunden.");

                        }

                    } else if(args[0].equalsIgnoreCase("fire")) {

                        ProjectileExaminer.Result result = ProjectileExaminer.examineInkProjectile(a, b.clone().add(.5, .5, .5), 0.91, SplatoonHumanPlayer.getPlayer(player).getMatch(),
                                SplatoonHumanPlayer.getPlayer(player).getTeam(), b.getBlock(), SplatoonHumanPlayer.getPlayer(player), 1);

                        if(result.isTargetReached()) {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Treffer.");
                            Location location = result.getHitLocation();

                            Trajectory trajectory = result.getTrajectory();
                            for(Vector3f vector3f : trajectory.getVectors()) {

                                player.spawnParticle(Particle.VILLAGER_HAPPY, vector3f.x, vector3f.y, vector3f.z, 0);

                            }


                        } else {

                            player.sendMessage(Chat.SYSTEM_PREFIX + "Nicht möglich.");
                            if(result.getHitLocation() != null) {

                                player.spawnParticle(Particle.VILLAGER_HAPPY,
                                        result.getHitLocation().getX(),
                                        result.getHitLocation().getY(),
                                        result.getHitLocation().getZ(), 0);


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
