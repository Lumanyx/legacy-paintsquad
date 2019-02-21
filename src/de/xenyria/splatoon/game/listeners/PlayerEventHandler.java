package de.xenyria.splatoon.game.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.collect.Sets;
import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Chat;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.servercore.spigot.events.PlayerLoginFinishedEvent;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;
import de.xenyria.splatoon.arena.ArenaProvider;
import de.xenyria.splatoon.arena.builder.ArenaBuilder;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrush;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractDualies;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.ai.MatchAIManager;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import de.xenyria.splatoon.lobby.shop.item.ShopItem;
import de.xenyria.splatoon.lobby.shop.weapons.WeaponShop;
import de.xenyria.splatoon.shootingrange.ShootingRange;
import de.xenyria.splatoon.tutorial.TutorialManager;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerEventHandler implements Listener {

    public PlayerEventHandler() {

        XenyriaSplatoon.registerListener(this);
        Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), () -> {

            /*if(currentBlock != null && currentBlock.hasMetadata("CoordinateX")) {

                int x = currentBlock.getMetadata("CoordinateX").get(0).asInt();
                int y = currentBlock.getMetadata("CoordinateY").get(0).asInt();
                int z = currentBlock.getMetadata("CoordinateZ").get(0).asInt();
                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(Bukkit.getOnlinePlayers().iterator().next());
                Match match = player.getMatch();
                MatchAIManager matchAIManager = match.getAIController();
                for(PaintableRegion region : matchAIManager.getPaintableRegions()) {

                    if(region.getCoordinate().getX() == x && region.getCoordinate().getY() == y && region.getCoordinate().getZ() == z) {

                        ArrayList<Vector> vectorArrayList = region.visibleFrom.getOrDefault(currentBlock, null);
                        if(vectorArrayList == null) {

                            Bukkit.broadcastMessage("Not visible!");

                        } else {

                            for(Vector vector : vectorArrayList) {

                                player.getPlayer().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, vector.toLocation(player.getWorld()), 0);

                            }
                            Bukkit.broadcastMessage(vectorArrayList.size() + " entries");

                        }

                    }

                }

            }*/

        }, 10l, 10l);

    }


    @EventHandler
    public void discover(PlayerRecipeDiscoverEvent event) {

        event.setCancelled(true);

    }

    public static CopyOnWriteArrayList<Player> ignoreTeleportMap = new CopyOnWriteArrayList<>();

    private static Block currentBlock = null;

    @EventHandler
    public void anim(PlayerAnimationEvent event) {

        if(event.getAnimationType() == PlayerAnimationType.ARM_SWING) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
            player.handleLeftClick();

        }

    }

    @EventHandler
    public void debugShowVisibility(BlockBreakEvent event) {

        Player player = event.getPlayer();
        //currentBlock = event.getBlock();
        //event.setCancelled(true);

    }

    @EventHandler
    public void antiLoad(ChunkLoadEvent event) {

        World world = event.getWorld();
        String name = world.getName();
        if(name.equalsIgnoreCase("sp_arena")) {

            if(!ArenaBuilder.keepLoaded(new ChunkCoordIntPair(
                    event.getChunk().getX(), event.getChunk().getZ()
            ))) {

                //event.getChunk().unload(false);

            }

        }

    }

    @EventHandler
    public void save(WorldSaveEvent event) {


    }

    @EventHandler
    public void antiSave(ChunkUnloadEvent event) {

        World world = event.getWorld();
        String name = world.getName();
        if(name.equalsIgnoreCase("sp_tutorial") || name.equalsIgnoreCase("sp_arena")) {

            if(name.equalsIgnoreCase("sp_arena")) {

                event.setSaveChunk(false);
                if(ArenaBuilder.keepLoaded(new ChunkCoordIntPair(event.getChunk().getX(), event.getChunk().getZ()))) {

                    event.setCancelled(true);

                }

            } else {

                event.setSaveChunk(true);

            }

        }

    }

    @EventHandler
    public void toggleFlight(PlayerToggleFlightEvent event) {

        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
        if(player.getMatch().inIntro() || player.getMatch().inOutro()) {

            event.setCancelled(true);
            if(player.getPlayer().getAllowFlight()) {

                player.getPlayer().setFlying(true);

            }

        }

    }

    @EventHandler
    public void autoStair(PlayerMoveEvent event) {

        Vector delta = event.getTo().toVector().subtract(event.getFrom().toVector()).multiply(2);
        Player player = event.getPlayer();

        if(delta.getY() >= 0) {

            delta.setY(0);
            EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
            Vector targetLoc = event.getFrom().toVector().add(delta);
            AxisAlignedBB bb = new AxisAlignedBB(targetLoc.getX() - .3, targetLoc.getY(), targetLoc.getZ() - .3, targetLoc.getX() + .3, targetLoc.getY() + 1.8, targetLoc.getZ() + .3);
            if (!AABBUtil.hasSpace(player.getWorld(), bb)) {

                Vector wrap = AABBUtil.resolveWrap(event.getPlayer().getWorld(), targetLoc, bb);
                if (wrap != null) {

                    double diffY = Math.abs(wrap.getY()-event.getFrom().getY());
                    if(diffY > 0.5) {

                        entityPlayer.setPositionRotation(wrap.getX(), wrap.getY(), wrap.getZ(), entityPlayer.yaw, entityPlayer.pitch);
                        entityPlayer.yaw = event.getTo().getYaw();
                        entityPlayer.lastX = wrap.getX();
                        entityPlayer.lastY = wrap.getY();
                        entityPlayer.lastZ = wrap.getZ();

                        Vector vel = player.getVelocity().clone();
                        Location newLoc = player.getLocation();
                        newLoc = newLoc.set(wrap.getX(), wrap.getY(), wrap.getZ());


                        newLoc.setYaw(event.getTo().getYaw());
                        newLoc.setPitch(event.getTo().getPitch());
                        event.setTo(newLoc);


                        if (!ignoreTeleportMap.contains(player)) {

                            ignoreTeleportMap.add(player);

                        }
                        Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags = new HashSet<>();
                        flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT);
                        flags.add(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT);

                        PacketPlayOutPosition packet = new PacketPlayOutPosition(
                                newLoc.getX(), newLoc.getY(), newLoc.getZ(),
                                0f, 0f, flags, 0
                        );

                        PacketContainer container = new PacketContainer(PacketType.Play.Server.POSITION, packet);
                        try {

                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container, false);

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                        player.setVelocity(vel);

                    }

                }

            }

        }

    }

    @EventHandler
    public void click(InventoryClickEvent event) {

        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
        Inventory inventory = event.getInventory();
        if(inventory.getTitle() != null && inventory.getTitle().endsWith(Match.JUMP_MENU_TITLE)) {

            event.setCancelled(true);

            if(player.isSplatted()) {

                event.getWhoClicked().closeInventory();
                return;

            }

            JumpPoint point = player.getMatch().jumpPointForSlot(player.getTeam(), event.getSlot());
            if(point != null) {

                if(point.isAvailable(player.getTeam())) {

                    Location location = point.getLocation();
                    if(location.distance(player.getLocation()) > 3) {

                        player.getPlayer().closeInventory();
                        player.superJump(location, () -> { point.onJumpEnd(); });
                        point.onJumpBegin();

                    } else {

                        player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Supersprung nicht möglich: Das Ziel ist zu nah.");

                    }

                } else {

                    player.getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Supersprung nicht möglich: Das Ziel nicht mehr gültig.");

                }

            }

        }

    }

    @EventHandler
    public void physics(BlockPhysicsEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void merge(ItemMergeEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void drag(InventoryDragEvent event) {

        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer((Player) event.getWhoClicked());
        Inventory inventory = event.getInventory();
        if(inventory.getTitle() != null && inventory.getTitle().endsWith(Match.JUMP_MENU_TITLE)) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {

        SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
        if(player.getMatch() != null) {

            try {

                player.leaveMatch();

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

        SplatoonHumanPlayer.getHumanPlayers().remove(player);
        XenyriaSplatoon.getXenyriaLogger().log("§eSpieler " + event.getPlayer().getName() + " (#?) §7hat den Server verlassen.");

    }

    @EventHandler
    public void antiKick(PlayerKickEvent event) {

        String msg = event.getReason();
        if(msg.contains("Flying is not enabled")) {

            SplatoonHumanPlayer player = ((SplatoonHumanPlayer.getPlayer(event.getPlayer())));
            if(player.isSquid()) {

                event.setCancelled(true);

            } else if(player.getEquipment().getSpecialWeapon() != null && player.getEquipment().getSpecialWeapon().isActive()){

                SplatoonSpecialWeapon splatoonSpecialWeapon = player.getEquipment().getSpecialWeapon();
                if(splatoonSpecialWeapon instanceof Baller || splatoonSpecialWeapon instanceof Jetpack) {

                    event.setCancelled(true);

                }

            }

        }

    }


    @EventHandler
    public void interactAt(PlayerInteractEntityEvent e) {

        ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
        handleInteraction(e.getPlayer(), stack, Action.RIGHT_CLICK_AIR);



        if(e.getRightClicked() != null) {

            for (AbstractShopkeeper shopkeeper : AbstractShopkeeper.getShopkeepers()) {

                if (e.getRightClicked().getEntityId() == shopkeeper.getEntityID()) {

                    shopkeeper.onInteraction(e.getPlayer());
                    e.setCancelled(true);

                }

            }

        }

    }

    public static void handleInteraction(Player player1, ItemStack currentItem, Action action) {

        if(action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(player1);
            if(currentItem != null) {

                ItemStack stack = currentItem;
                if (stack.equals(StaticItems.OPEN_JUMP_MENU)) {

                    player.getPlayer().openInventory(player.getMatch().getOrCreateJumpMenu(player.getTeam()));

                } else if(stack.equals(SplatoonLobby.OPEN_INVENTORY)) {

                    player.getInventory().open();

                } else if(stack.equals(StaticItems.RETURN_TO_LOBBY)) {

                    if (!(player.getMatch() instanceof SplatoonLobby)) {

                        XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player);

                    }

                } else if(stack.equals(StaticItems.RETURN_TO_WEAPONSHOP)) {

                    if (!(player.getMatch() instanceof SplatoonLobby)) {

                        XenyriaSplatoon.getLobbyManager().addPlayerToLobby(player);
                        player.getPlayer().teleport(XenyriaSplatoon.getLobbyManager().getLobby().weaponShopTeleport);

                    }

                } else if(stack.equals(StaticItems.RESET_MAP)) {

                    if(player.getMatch() instanceof ShootingRange) {

                        ShootingRange shootingRange = (ShootingRange) player.getMatch();
                        if(shootingRange.lastResetMillis() >= 2000) {

                            shootingRange.reset();
                            player1.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 2, false, false, false));

                            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                if (player1.isOnline()) {

                                    player1.sendMessage(Chat.SYSTEM_PREFIX + "Die Map wurde zurückgesetzt!");
                                    player.teleport(player.getSpawnPoint());

                                }

                            }, 12l);

                        }


                    }

                } else if(stack.equals(StaticItems.CHANGE_WEAPON)) {

                    SplatoonLobby lobby = XenyriaSplatoon.getLobbyManager().getLobby();
                    WeaponShop shop = lobby.getWeaponShop();
                    player1.openInventory(shop.getOverviewInventory());

                } else if(stack.equals(StaticItems.SPECTATE)) {

                    Match match = player.getMatch();
                    if(match instanceof BattleMatch) {

                        BattleMatch match1 = (BattleMatch) match;
                        player1.openInventory(match1.createSpectatorMenu());

                    }

                }

            }

            if(player != null) {

                player.updateLastInteraction();

            }

        } else if(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(player1);
            if(player != null && player.getEquipment().getPrimaryWeapon() != null) {

                player.handleLeftClick();

            }

        }

    }

    @EventHandler
    public void updateInteraction(PlayerInteractEvent event) {

        ItemStack itemStack = event.getItem();
        if(itemStack != null) {

            if(ItemBuilder.hasValue(itemStack, "SpecialWeapon") || ItemBuilder.hasValue(itemStack, "SecondaryWeapon")) {

                event.setCancelled(true);

            }

        }

        handleInteraction(event.getPlayer(), event.getItem(), event.getAction());

    }
    @EventHandler
    public void updateInteractionEntity(PlayerInteractAtEntityEvent event) {

        ItemStack stack = event.getPlayer().getInventory().getItem(event.getHand());
        handleInteraction(event.getPlayer(), stack, Action.RIGHT_CLICK_AIR);

        Entity entity = event.getRightClicked();
        if(entity.hasMetadata("ShopItemID")) {

            int shopItemID = entity.getMetadata("ShopItemID").get(0).asInt();
            for(ShopItem item : ShopItem.getItems()) {

                if(item.getShopItemID() == shopItemID) {

                    item.onInteract(event.getPlayer());
                    return;

                }

            }

        }

    }

    @EventHandler
    public void noExplosion(FireworkExplodeEvent event) {

        if(event.getEntity().hasMetadata("Xenyria")) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void foodLevel(FoodLevelChangeEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void regainHealth(EntityRegainHealthEvent event) {

        if(event.getRegainReason() != EntityRegainHealthEvent.RegainReason.CUSTOM) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void join1(PlayerJoinEvent event) {

        NoteBlockAPI.setPlayerVolume(event.getPlayer(), (byte)100);

    }

    @EventHandler
    public void join(PlayerLoginFinishedEvent event) {

        Player player1 = event.getSpigotPlayer();

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            SplatoonHumanPlayer player = new SplatoonHumanPlayer(event.getPlayer(), player1);

            if(event.getPlayer().getVariableData().getHandle().exists("splatoon.coins")) {

                player.getUserData().updateCoins(event.getPlayer().getVariableData().getHandle().getInt("splatoon.coins"));

            }

            SplatoonHumanPlayer.getHumanPlayers().add(player);
            player1.setFoodLevel(6);
            player.joinMatch(XenyriaSplatoon.getLobbyManager().getLobby());
            XenyriaSplatoon.getXenyriaLogger().log("§eSpieler " + player1.getName() + " (#?) §7hat den Server betreten.");

            if(player1.hasPermission("xenyria.teammember")) {

                if(TutorialManager.isEnabled()) {

                    player.getPlayer().sendMessage("§a[Splatoon] §7TutorialManager ist aktiv!");

                } else {

                    player.getPlayer().sendMessage("§a[Splatoon] §7TutorialManager nicht aktiviert.");

                }

            }


        }, 1l);

    }

    @EventHandler
    public void pickup(EntityPickupItemEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void drop1(PlayerDropItemEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void drop(EntityDropItemEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void noCombat(EntityDamageByEntityEvent event) {

        if(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
        event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
        event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
        event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {

            event.setCancelled(true);

        }

    }
    @EventHandler
    public void noSuff(EntityDamageEvent event) {

        if(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
                event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
                event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                event.getCause() == EntityDamageEvent.DamageCause.DRYOUT) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void swap(PlayerSwapHandItemsEvent event) {

        event.setCancelled(true);

    }

    @EventHandler
    public void fallDmg(EntityDamageEvent event) {

        if(event.getCause() == EntityDamageEvent.DamageCause.FALL) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent event) {

        for(Entity entity : event.getChunk().getEntities()) {

            if(entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.DROPPED_ITEM || entity.getType() == EntityType.ARMOR_STAND) {

                entity.remove();

            }

        }

    }

}
