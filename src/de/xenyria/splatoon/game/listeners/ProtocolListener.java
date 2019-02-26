package de.xenyria.splatoon.game.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.math.BitUtil;
import de.xenyria.servercore.spigot.XenyriaSpigotServerCore;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBlaster;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplatling;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.WeaponModel;
import de.xenyria.splatoon.game.match.intro.IntroManager;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.scoreboard.EntityHighlightController;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.lobby.shop.AbstractShopkeeper;
import de.xenyria.splatoon.lobby.shop.gear.GearShopItem;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ProtocolListener extends PacketAdapter {

    public ProtocolListener() {

        super(XenyriaSplatoon.getPlugin(), ListenerPriority.LOWEST,
                PacketType.Play.Client.USE_ITEM, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Client.STEER_VEHICLE, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_DESTROY,
                PacketType.Play.Server.ABILITIES, PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.POSITION,
                PacketType.Play.Server.ENTITY_TELEPORT, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Client.SPECTATE,
                PacketType.Play.Server.ENTITY_EQUIPMENT, PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.USE_ITEM, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT);

    }

    @Override
    public void onPacketSending(PacketEvent event) {

        if(event.getPacketType() == PacketType.Play.Server.SET_SLOT) {

            ItemStack stack = event.getPacket().getItemModifier().read(0);
            if (ResourcePackUtil.hasCustomResourcePack(event.getPlayer())) {

                if (ItemBuilder.hasValue(stack, "rwr_mat")) {

                    ItemStack rewritten = Gear.rewriteItemStack(stack);
                    //System.out.println("rewrite " + rewritten);
                    event.getPacket().getItemModifier().write(0, rewritten);

                }

            }

        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {

            ItemStack stack = event.getPacket().getItemModifier().read(0);
            if (ResourcePackUtil.hasCustomResourcePack(event.getPlayer())) {

                if (ItemBuilder.hasValue(stack, "rwr_mat")) {

                    ItemStack rewritten = Gear.rewriteItemStack(stack);
                    //System.out.println("rewrite " + rewritten);
                    event.getPacket().getItemModifier().write(0, rewritten);

                }

            }

        } else if(event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {

            ArrayList<ItemStack> recreate = new ArrayList<>();
            for(ItemStack stack : event.getPacket().getItemListModifier().read(0)) {

                if (ItemBuilder.hasValue(stack, "rwr_mat")) {

                    ItemStack rewritten = Gear.rewriteItemStack(stack);
                    //System.out.println("rewrite 2" + rewritten);
                    recreate.add(rewritten);

                } else {

                    recreate.add(stack);

                }

            }
            event.getPacket().getItemListModifier().write(0, recreate);

        } else if(event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {

            EnumWrappers.PlayerInfoAction action = event.getPacket().getPlayerInfoAction().read(0);
            List<PlayerInfoData> dataList = event.getPacket().getPlayerInfoDataLists().read(0);
            ArrayList<PlayerInfoData> replacement = new ArrayList<>(dataList.size());
            for(PlayerInfoData data : dataList) {

                if(data.getProfile().getProperties().containsKey("xenyria")) {

                    PlayerInfoData data1 = new PlayerInfoData(data.getProfile(), 0, EnumWrappers.NativeGameMode.SURVIVAL, data.getDisplayName());
                    replacement.add(data1);

                } else {

                    replacement.add(data);

                }

            }
            event.getPacket().getPlayerInfoDataLists().write(0, dataList);

        } else if(event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {

            Sound sound = event.getPacket().getSoundEffects().read(0);
            if (sound == Sound.ITEM_ARMOR_EQUIP_GENERIC) {

                event.setCancelled(true);

            }

        } else if(event.getPacketType() == PacketType.Play.Server.POSITION) {

            Player player = event.getPlayer();
            if (PlayerEventHandler.ignoreTeleportMap.contains(player)) {

                PlayerEventHandler.ignoreTeleportMap.remove(player);

            }

        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {

            Player player = event.getPlayer();
            int id = event.getPacket().getIntegers().read(0);
            if (ResourcePackUtil.hasCustomResourcePack(player)) {

                if (WeaponModel.resourcePackIgnoreIDs.contains(id)) {

                    event.setCancelled(true);

                }

            }

        } else if(event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
            int id = event.getPacket().getIntegers().read(0);
            if (ResourcePackUtil.hasCustomResourcePack(event.getPlayer())) {

                if (WeaponModel.resourcePackIgnoreIDs.contains(id)) {

                    event.setCancelled(true);

                }

            }

            if(player != null) {

                int i = event.getPacket().getIntegers().read(0);
                for (EntityNPC npc : EntityNPC.getNPCs()) {

                    if (npc.tagEntityID() == i) {

                        if(player.getTeam() != null && !player.getTeam().equals(npc.getTeam())) {

                            event.setCancelled(true);

                        }

                    }

                }

            }

        } else if(event.getPacketType() == PacketType.Play.Server.ABILITIES) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
            if(player != null && player.getMatch() != null) {

                if (player.getMatch().inIntro()) {

                    if (player.getMatch().getIntroManager() != null) {

                        if (player.getMatch().getIntroManager().introPhase() == IntroManager.Phase.MAP_PREVIEW) {

                            event.getPacket().getFloat().write(1, 0.35f);

                        } else {

                            event.getPacket().getFloat().write(1, 0.7f);

                        }

                    }

                    //event.getPacket().getFloat().write(1, 5f);
                } else if (player.getMatch().inOutro()) {

                    if (player.getMatch().getOutroManager() != null) {

                        if (player.getMatch().getOutroManager().isZoomActive()) {

                            event.getPacket().getFloat().write(1, 0.2f);

                        }

                    }

                }

                if (player != null && player.getEquipment().getPrimaryWeapon() != null) {

                    if (player.getEquipment().getPrimaryWeapon() instanceof AbstractCharger) {

                        AbstractCharger charger = (AbstractCharger) player.getEquipment().getPrimaryWeapon();
                        if (charger.hasZoom() && charger.isSelected() && charger.isCharging()) {

                            event.getPacket().getFloat().write(1, charger.getZoomModificator());

                        }

                    } else if (player.getEquipment().getPrimaryWeapon() instanceof AbstractSplatling) {

                        event.getPacket().getFloat().write(1, 0f);

                    } else if (player.getEquipment().getPrimaryWeapon() instanceof AbstractBlaster) {

                        event.getPacket().getFloat().write(1, 0f);

                    }

                }

            }

        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
            if(player != null) {

                int id = event.getPacket().getIntegers().read(0);
                if(player.getHighlightController().isHighlighted(id)) {

                    EntityHighlightController.TeamEntry entry = player.getHighlightController().getEntry(id);
                    WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(entry.entity.getBukkitEntity()).deepClone();
                    WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);

                    byte origVal = (byte) watcher.getObject(0);
                    watcher.setObject(0, serializer, BitUtil.setBit(origVal, 6, true));

                    event.getPacket().getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

                } else {

                    if(GearShopItem.getShopItemEntityIDs().containsKey(id)) {

                        WrappedDataWatcher watcher = new WrappedDataWatcher(GearShopItem.getShopItemEntityIDs().get(id).getDataWatcher()).deepClone();
                        ItemStack stack = watcher.getItemStack(6);
                        if (ItemBuilder.hasValue(stack, "rwr_mat")) {

                            stack = Gear.rewriteItemStack(stack);

                        }
                        watcher.setObject(6, stack);
                        event.getPacket().getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

                    } else {

                        if (player.getEquipment().getSpecialWeapon() != null && player.getEquipment().getSpecialWeapon().isActive()) {

                            if (player.getEquipment().getSpecialWeapon() instanceof Baller) {

                                Baller baller = ((Baller) player.getEquipment().getSpecialWeapon());
                                if (baller.mimic != null && id == baller.mimic.getId()) {

                                    WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(baller.mimic.getBukkitEntity()).deepClone();
                                    WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);

                                    byte origVal = (byte) watcher.getObject(0);
                                    watcher.setObject(0, serializer, BitUtil.setBit(origVal, 1, true));
                                    event.getPacket().getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

                                }

                            }

                        }

                    }

                }

            }

        }

    }

    @Override
    public void onPacketReceiving(PacketEvent event) {

        if(event.getPacketType() == PacketType.Play.Client.SPECTATE) {


        } else if(event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {

            Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
                double z = event.getPacket().getFloat().read(0);
                double x = event.getPacket().getFloat().read(1);
                final double origX = x;

                boolean passToSpecial = player.getEquipment().getSpecialWeapon() != null &&
                        player.getPlayer().getInventory().getHeldItemSlot() == 3 &&
                        ((player.getEquipment().getSpecialWeapon() instanceof Jetpack && ((Jetpack)player.getEquipment().getSpecialWeapon()).isActive()) || (player.getEquipment().getSpecialWeapon() instanceof Baller && player.getEquipment().getSpecialWeapon().isActive()));
                boolean riding = player.isRidingOnInkRail() || player.isRidingOnRideRail();

                double mX = 0d, mZ = 0d;
                if (Math.abs(x) > 0.01 || Math.abs(z) > 0.01) {

                    if (x != 0d) {

                        Location location = event.getPlayer().getLocation().clone();
                        if (!riding) {
                            location.setPitch(0f);
                        }

                        Vector vec = location.getDirection().clone().normalize();

                        if (x < 0) {
                            vec = vec.multiply(-1);
                        }

                        mX += vec.getX();
                        mZ += vec.getZ();

                    }

                    if (z != 0d) {

                        Location location = event.getPlayer().getLocation().clone();
                        if (!riding) {
                            location.setPitch(0f);
                        }

                        if (z > 0) {

                            location.setYaw(location.getYaw() - 90f);

                        } else {

                            location.setYaw(location.getYaw() + 90f);

                        }

                        Vector vec = location.getDirection().clone();
                        mX += vec.getX();
                        mZ += vec.getZ();

                    }

                }

                if(!passToSpecial) {

                    boolean space = event.getPacket().getBooleans().read(0);
                    boolean shift = event.getPacket().getBooleans().read(1);

                    if (player != null && player.isSquid()) {

                        player.handleInput(mX, mZ, origX, space, shift, true);

                    }

                } else {

                    if(player.getEquipment().getSpecialWeapon() instanceof Jetpack) {

                        boolean space = event.getPacket().getBooleans().read(0);
                        boolean shift = event.getPacket().getBooleans().read(1);
                        Jetpack jetpack = (Jetpack) player.getEquipment().getSpecialWeapon();
                        jetpack.handleInput(mX, mZ);

                        if (space) {

                            jetpack.boost();

                        }

                    } else if(player.getEquipment().getSpecialWeapon() instanceof Baller) {

                        boolean space = event.getPacket().getBooleans().read(0);
                        Baller baller = ((Baller)player.getEquipment().getSpecialWeapon());
                        baller.handleInput(mX, mZ, space);

                    }

                }

            });


        } else if(event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {


            int entityID = event.getPacket().getIntegers().read(0);

            // "Cannot interact with self" Kick prevention
            if(entityID == event.getPlayer().getEntityId()) { event.setCancelled(true); return; }

            EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().read(0);

            SplatoonHumanPlayer player = SplatoonHumanPlayer.getPlayer(event.getPlayer());
            if(player != null) {


                EnumWrappers.EntityUseAction action1 = event.getPacket().getEntityUseActions().read(0);
                if (action1 == EnumWrappers.EntityUseAction.ATTACK) {

                    Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                        player.handleLeftClick();

                    });

                } else {

                    player.updateLastInteraction();
                    if(entityID == player.getTank().getId()) {

                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            PlayerEventHandler.handleInteraction(event.getPlayer(), player.getPlayer().getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR);

                        }, 1l);

                    }

                }

            }

        }

        // PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.USE_ITEM

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return super.getSendingWhitelist();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return super.getReceivingWhitelist();
    }

    @Override
    public Plugin getPlugin() { return XenyriaSplatoon.getPlugin(); }

}
