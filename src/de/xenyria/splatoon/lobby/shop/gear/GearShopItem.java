package de.xenyria.splatoon.lobby.shop.gear;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.math.BitUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.boots.LegacyFootGear;
import de.xenyria.splatoon.game.equipment.gear.chest.LegacyBodyGear;
import de.xenyria.splatoon.game.equipment.gear.head.LegacyHeadGear;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.lobby.shop.item.ShopItem;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityMetadata;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;

public class GearShopItem extends ShopItem {

    public static final String BUY_EQUIPMENT_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cAusrüstung kaufen?";
    public static final String PURCHASE_EQUIP_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cGekauft! Jetzt anlegen?";
    public GearShopItem(Gear gear, String name, String desc, String desc2, int price, Location spawnLocation) {

        super(name, desc, desc2, price, spawnLocation);

        buyInventory = Bukkit.createInventory(null, 36, BUY_EQUIPMENT_TITLE);
        for(int i = 0; i < 36; i++) {

            buyInventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        dummy = gear;
        buyInventory.setItem(13, gear.asItemStack(null));
        boolean generic = gear instanceof LegacyHeadGear || gear instanceof LegacyBodyGear || gear instanceof LegacyFootGear;
        buyInventory.setItem(29, new ItemBuilder(Material.BARRIER).addToNBT("Dismiss", true).setDisplayName("§cLieber nicht").create());
        buyInventory.setItem(33, new ItemBuilder(Material.EMERALD).
                addToNBT("PurchaseItem", getShopItemID()).
                addToNBT("Generic", generic).setDisplayName("§aKaufen").create());
        setItem(gear.asItemStack(null));

        visibleHitbox = new AxisAlignedBB(
                spawnLocation.getX() - .625, spawnLocation.getY() - .5, spawnLocation.getZ() - .6255, spawnLocation.getX() + .625, spawnLocation.getY() + 2.5, spawnLocation.getZ() + .625
        );

    }

    private ArrayList<Player> visibles = new ArrayList<>();
    public void tick() {

        Iterator<Player> iterator = visibles.iterator();
        while (iterator.hasNext()) {

            Player player = iterator.next();
            if(!canSee(player)) {

                for(EntityArmorStand stand : getTags()) {

                    sendVisible(player, stand, false);

                }
                iterator.remove();

            }

        }

    }

    public void sendVisible(Player player, EntityArmorStand stand, boolean visible) {

        PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), false));

        WrappedDataWatcher watcher = new WrappedDataWatcher(stand.getDataWatcher()).deepClone();
        watcher.setObject(3, visible, false);
        container.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        try {

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container, false);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void playerVisibilityCheck(Player player) {

        if(!visibles.contains(player)) {

            if (canSee(player)) {

                visibles.add(player);
                for(EntityArmorStand stand : getTags()) {

                    sendVisible(player, stand, true);

                }

            }

        }

    }

    public boolean canSee(Player player) {

        double dist = 5d;
        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), dist);

        if(result != null) {

            dist = result.getHitPosition().distance(player.getEyeLocation().toVector());

        }

        BoundingBox boundingBox = new BoundingBox(visibleHitbox.minX, visibleHitbox.minY, visibleHitbox.minZ, visibleHitbox.maxX, visibleHitbox.maxY, visibleHitbox.maxZ);
        Vector begin = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection();
        RayTraceResult bbResult = boundingBox.rayTrace(begin, direction, dist);
        if(bbResult != null) {

            double requiredDist = bbResult.getHitPosition().distance(begin);
            if(requiredDist <= dist) {

                return true;

            }

        }
        return false;

    }

    private AxisAlignedBB visibleHitbox;
    public AxisAlignedBB getVisibleHitbox() { return visibleHitbox; }

    private Gear dummy;
    public Gear getDummy() { return dummy; }
    public Gear newInstance() {

        if(dummy instanceof LegacyHeadGear || dummy instanceof LegacyBodyGear || dummy instanceof LegacyFootGear) {

            return XenyriaSplatoon.getGenericGearRegistry().getNewGearInstance(dummy.getOriginID());

        } else {

            return XenyriaSplatoon.getGearRegistry().newInstance(dummy.getOriginID());

        }

    }

    private Inventory buyInventory;

    @Override
    public void onRemove() {

        if(buyInventory != null) {

            buyInventory = null;

        }

    }

    public static GearShopItem fromGear(Gear gear, Location location) {

        String name = colorPrefix(gear.getMaxSubAbilities()) + gear.getName();
        String desc = getDescription(gear);

        return new GearShopItem(gear, name, desc,  gear.getBrand().getDisplayName(), gear.getPrice(), location);

    }

    private static String colorPrefix(int abilityCount) {

        switch (abilityCount) {

            case 1: return "§7";
            case 2: return "§e";
            case 3: return "§6§l";

        }
        return "";
    }
    private static String getDescription(Gear gear) {

        String desc = "§e";

        for(int i = 0; i < gear.getMaxSubAbilities(); i++) {

            desc+=Characters.STAR;

        }

        desc+=" §8| §r" + gear.getMainAbility().getShortName();
        return desc;

    }

    @Override
    public void onInteract(Player player) {

        player.openInventory(buyInventory);

    }
}
