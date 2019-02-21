package de.xenyria.splatoon.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.navigation.TransitionType;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SplatoonCommand implements CommandExecutor {

    public static double travelledY = 0d;
    public static double lastDeltaY = 0d;
    public static int tick = 0;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {


        Player player = (Player)commandSender;
        Location a = player.getLocation();

        SplatoonPlayer player1 = SplatoonHumanPlayer.getPlayer(player);

        /*player.getInventory().setChestplate(new ItemStack(Material.APPLE));
        player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(9999d);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            ItemStack stack = new ItemBuilder(Material.STONE_HOE).setDurability(4).create();
            stack.setDurability((short)5);
            player.getInventory().setItemInMainHand(stack);

        }, 20l);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            ItemStack stack = new ItemBuilder(Material.STONE_HOE).setDurability(5).create();
            stack.setDurability((short)6);
            player.getInventory().setItemInMainHand(stack);

        }, 24l);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            ItemStack stack = new ItemBuilder(Material.STONE_HOE).setDurability(6).create();
            stack.setDurability((short)7);
            player.getInventory().setItemInMainHand(stack);

        }, 28l);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            ItemStack stack = new ItemBuilder(Material.STONE_HOE).setDurability(6).create();
            stack.setDurability((short)8);
            player.getInventory().setItemInMainHand(stack);

        }, 32l);

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            ItemStack stack = new ItemBuilder(Material.STONE_HOE).setDurability(6).create();
            stack.setDurability((short)9);
            player.getInventory().setItemInMainHand(stack);

        }, 36l);


        EntityArmorStand stand = new EntityArmorStand(((CraftPlayer)player).getHandle().getWorld());
        stand.setInvisible(true);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));
        try {

            PacketContainer container = new PacketContainer(PacketType.Play.Server.MOUNT);
            container.getIntegers().write(0, player.getEntityId());
            container.getIntegerArrays().write(0, new int[]{stand.getId()});
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.HEAD,
                    CraftItemStack.asNMSCopy(new ItemStack(Material.APPLE))));

            Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

                stand.yaw = player.getLocation().getYaw();
                stand.setHeadPose(new Vector3f(0f, (float)player.getLocation().getYaw(), 0f));
                //((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), false));
                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(stand, ((byte)(stand.yaw * 0.7111))));

            }, 1l, 1l);

        } catch (Exception e) {

            e.printStackTrace();

        }*/

        EntityNPC npc = new EntityNPC("test", a, player1.getMatch().getRegisteredTeams().get(1), player1.getMatch());
        npc.disableAI();
        BombProjectile projectile = new BombProjectile(SplatoonHumanPlayer.getPlayer(player), null, npc.getMatch(), 40f, 0, 1, false);
        projectile.spawn(0, player.getLocation());
        npc.joinMatch(player1.getMatch());
        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), npc::tick, 1l, 1l);
        npc.getNavigationManager().setTarget(new PathfindingTarget() {

            @Override
            public boolean needsUpdate(Vector vector) {

                double dist = player.getLocation().toVector().distance(vector);
                return dist > 3;
            }

            @Override
            public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

                double dist = player.getLocation().toVector().distance(vector);
                return dist <= 3;

            }

            @Override
            public boolean useGoalNode() {
                return false;
            }

            @Override
            public SquidAStar.MovementCapabilities getMovementCapabilities() {
                return null;
            }

            @Override
            public void beginPathfinding() {

            }

            @Override
            public void endPathfinding() {

            }

            @Override
            public int maxNodeVisits() {
                return 120;
            }

            @Override
            public NodeListener getNodeListener() {
                return null;
            }

            @Override
            public Vector getEstimatedPosition() {
                return player.getLocation().toVector();
            }
        });

        /*npc.getNavigationManager().dbgAddPoint(0, 65, 1, TransitionType.WALK);
        npc.getNavigationManager().dbgAddPoint(0, 65, 0, TransitionType.WALK);
        npc.getNavigationManager().dbgAddPoint(0, 66, -2, TransitionType.JUMP_TO);
        npc.getNavigationManager().dbgAddPoint(0, 67, -4, TransitionType.JUMP_TO);
        npc.getNavigationManager().dbgAddPoint(0, 68, -6, TransitionType.JUMP_TO);
        npc.getNavigationManager().dbgAddPoint(0, 68, -9, TransitionType.WALK);*/

        /*a.setYaw(0f);
        a.setPitch(0f);
        a.weapons(((int)a.getX()) + .5, ((int)a.getY() + 325), ((int)a.getZ()) + .5);
        player.teleport(a);
        player.setVelocity(new Vector(0, 0, 0));
        travelledY = 0d;
        tick = 0;
        lastDeltaY = 0;
        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

            if(player.getVelocity().getY() < 0) {

                double curDelta = player.getLocation().getY();
                double velDelta = curDelta - lastDeltaY;
                System.out.println(tick + " > " + velDelta);
                lastDeltaY = curDelta;

            } else {

                player.sendMessage(travelledY + " m");

            }
            tick++;

        }, 1l, 1l);


        */


        /*
        Vector dir = player.getLocation().getDirection();
        Vector start = player.getEyeLocation().clone().toVector();
        Location location = player.getEyeLocation().clone();
        location.setYaw(location.getYaw() + 90f);
        Vector sideways = location.getDirection().clone();
        location.setPitch(location.getPitch() - 90f);
        Vector upwards = location.getDirection().clone();

        Vector target = start.clone().add(dir.clone().multiply(.4)).add(sideways.clone().multiply(.135)).add(upwards.clone().multiply(-.05));
        player.spawnParticle(Particle.END_ROD, target.getX(), target.getY(), target.getZ(), 0);

        SplatoonPlayer splatoonPlayer = SplatoonPlayer.getPlayer(player);
        //splatoonPlayer.splat(Color.RED, null);
        //splatoonPlayer.superJump(player.getLocation().clone().add(128, 10, 0));

        Vector[] vecs = new Vector[6];

        vecs[0] = new Vector(player.getLocation().getX() +3, player.getLocation().getY() + 0.6, player.getLocation().getZ() + 5d);
        vecs[1] = new Vector(player.getLocation().getX() +1, player.getLocation().getY() + 3, player.getLocation().getZ() + 7d);
        vecs[2] = new Vector(player.getLocation().getX() + 2, player.getLocation().getY() + 4, player.getLocation().getZ() + 30d);
        vecs[3] = new Vector(player.getLocation().getX() + 7, player.getLocation().getY() + 7, player.getLocation().getZ() + 20d);
        vecs[4] = new Vector(player.getLocation().getX() + 7, player.getLocation().getY() + 20, player.getLocation().getZ() + 20d);
        vecs[5] = new Vector(player.getLocation().getX() + 2, player.getLocation().getY() + 24, player.getLocation().getZ() + 10d);

        RideRail inkRail = new RideRail(XenyriaSplatoon.getMatch(), vecs);
        try {

            inkRail.interpolateVectors();

        } catch (Exception e) {

            e.printStackTrace();

        }
        XenyriaSplatoon.getMatch().addGameObject(inkRail);
        //inkRail.updateOwningTeam(SplatoonPlayer.getPlayer(player).getTeam());
        //splatoonPlayer.superJump(new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ() + 5d));*/

        /*
        commandSender.sendMessage(
                ((Player)commandSender).getResourcePackStatus() + ""
        );
        commandSender.sendMessage(
                ((Player)commandSender).getResourcePackHash() + ""
        );

        /*

        splatoonPlayer.getEquipment().setPrimaryWeapon(4);
        splatoonPlayer.getEquipment().setSecondaryWeapon(20);
        splatoonPlayer.getEquipment().setSpecialWeapon(3);*/

        /*
        Dummy dummy = new Dummy(splatoonPlayer.getMatch(), splatoonPlayer.getPlayer().getLocation().clone().add(0, 2, 5), 100f);
        Dummy dummy1 = new Dummy(splatoonPlayer.getMatch(), splatoonPlayer.getPlayer().getLocation().clone().add(0, 2, 7), 100f);
        //Dummy dummy2 = new Dummy(splatoonPlayer.getMatch(), splatoonPlayer.getPlayer().getLocation().clone().add(0, 2, 3), 100f);
        XenyriaSplatoon.getMatch().addGameObject(dummy);
        XenyriaSplatoon.getMatch().addGameObject(dummy1);

        RainmakerBarrier barrier = new RainmakerBarrier(splatoonPlayer.getMatch(), player.getLocation().clone().add(15, 1, 15));
        XenyriaSplatoon.getMatch().addGameObject(barrier);

        /**/

        /*
        Vector targetPos = player.getEyeLocation().toVector().add(new Vector(5, 0, 5));

        Sponge sponge = new Sponge(splatoonPlayer.getMatch(), player.getWorld().getBlockAt((int)targetPos.getX(), (int)targetPos.getY(), (int)targetPos.getZ()));
        splatoonPlayer.getMatch().addGameObject(sponge);

        Gusher gusher = new Gusher(splatoonPlayer.getMatch(), player.getLocation().getBlock(), BlockFace.UP, 2d);
        splatoonPlayer.getMatch().addGameObject(gusher);

        Hook hook = new Hook(splatoonPlayer.getMatch(), player.getLocation().clone().add(0, 5, 0));
        splatoonPlayer.getMatch().addGameObject(hook);
        /*WorldServer server;*/

        return true;

    }

}
