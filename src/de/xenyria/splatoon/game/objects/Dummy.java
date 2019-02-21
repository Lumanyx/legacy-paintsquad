package de.xenyria.splatoon.game.objects;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Dummy extends GameObject implements HitableEntity, TentaMissleTarget {

    private float currentDamage = 0f;
    private float maximumDamage = 0f;

    private ArrayList<Player> trackers = new ArrayList<>();

    public Dummy(Match match, Location location, float maximumDamage) {

        super(match);
        this.maximumDamage = maximumDamage;
        World world = ((CraftWorld)location.getWorld()).getHandle();

        stand = new EntityArmorStand(world, location.getX(), location.getY(), location.getZ());
        stand.locX = location.getX();
        stand.locY = location.getY();
        stand.locZ = location.getZ();
        stand.yaw = location.getYaw();
        stand.aS = location.getYaw();

        ((ArmorStand)stand.getBukkitEntity()).setCustomNameVisible(true);
        ((ArmorStand)stand.getBukkitEntity()).setCustomName("§7Attrape");
        ((ArmorStand)stand.getBukkitEntity()).setCanMove(false);
        ((ArmorStand)stand.getBukkitEntity()).setCanTick(false);
        ((ArmorStand)stand.getBukkitEntity()).setCollidable(false);
        ((ArmorStand)stand.getBukkitEntity()).setGravity(false);
        ((ArmorStand)stand.getBukkitEntity()).setBasePlate(false);

        setItems();
        updateDamage();

    }

    public void setItems() {

        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET).create();
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(Color.BLACK);
        helmet.setItemMeta(meta);

        ItemStack chest = new ItemBuilder(Material.LEATHER_CHESTPLATE).create();
        meta = (LeatherArmorMeta) chest.getItemMeta();
        meta.setColor(Color.BLACK);
        chest.setItemMeta(meta);

        ItemStack boots = new ItemBuilder(Material.LEATHER_BOOTS).create();
        meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(Color.BLACK);
        boots.setItemMeta(meta);

        ((ArmorStand)stand.getBukkitEntity()).setHelmet(helmet);
        ((ArmorStand)stand.getBukkitEntity()).setChestplate(chest);
        ((ArmorStand)stand.getBukkitEntity()).setBoots(boots);

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                    stand.getId(),
                    EnumItemSlot.HEAD,
                    stand.getEquipment(EnumItemSlot.HEAD)
            ));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                    stand.getId(),
                    EnumItemSlot.CHEST,
                    stand.getEquipment(EnumItemSlot.CHEST)
            ));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(
                    stand.getId(),
                    EnumItemSlot.FEET,
                    stand.getEquipment(EnumItemSlot.FEET)
            ));

        }

    }

    public void updateDamage() {

        DecimalFormat format = new DecimalFormat("#.#");
        float percentage = (currentDamage / maximumDamage) * 100f;
        String prefix = "";
        if(percentage < 33) {

            prefix = "§a";

        } else if(percentage > 33 && percentage <= 66) {

            prefix = "§e";

        } else if(percentage > 66 && percentage <= 90) {

            prefix = "§c";

        } else {

            prefix = "§4§l";

        }
        stand.getBukkitEntity().setCustomName(prefix + currentDamage);
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true));

        }

    }

    private EntityArmorStand stand;
    private int invisibleTicks = 0;
    public AxisAlignedBB aabb() { return stand.getBoundingBox(); }

    @Override
    public boolean isDead() {
        return stand.isInvisible();
    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile instanceof DamageDealingProjectile) {

            DamageDealingProjectile projectile1 = (DamageDealingProjectile) projectile;
            currentDamage += projectile1.getDamage();
            if(projectile.getShooter() != null) {

                projectile.getShooter().hitMark(((ArmorStand)stand.getBukkitEntity()).getLocation());

            }

            invincibleTicks = 3;

            updateDamage();
            if(currentDamage >= maximumDamage) {

                ((ArmorStand)stand.getBukkitEntity()).setVisible(false);
                ((ArmorStand)stand.getBukkitEntity()).getLocation().getWorld().playSound(((ArmorStand)stand.getBukkitEntity()).getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.25f);
                invisibleTicks = 60;
                ((ArmorStand)stand.getBukkitEntity()).setBoots(null);
                ((ArmorStand)stand.getBukkitEntity()).setHelmet(null);
                ((ArmorStand)stand.getBukkitEntity()).setChestplate(null);

                for(Player player : trackers) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.HEAD, stand.getEquipment(EnumItemSlot.HEAD)));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.CHEST, stand.getEquipment(EnumItemSlot.CHEST)));
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.FEET, stand.getEquipment(EnumItemSlot.FEET)));

                }

            }

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(invincibleTicks > 0) { return false; }
        if(!((ArmorStand)stand.getBukkitEntity()).isVisible()) { return false; }

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            /*
            Vector pos = stand.getLocation().toVector();
            pos = pos.add(new Vector(0, stand.getEyeHeight() / 2, 0));
            return RayProjectile.rayCast(stand.getWorld(), ((RayProjectile)projectile).originVec(), pos, stand.getEntityId());
            */
            return true;

        }

        return aabb().c(projectile.aabb()) && ((ArmorStand)stand.getBukkitEntity()).isVisible();
    }

    @Override
    public double distance(SplatoonProjectile bombProjectile) {
        return ((ArmorStand)stand.getBukkitEntity()).getLocation().distance(bombProjectile.getLocation());
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.DUMMY;
    }

    private int invincibleTicks = 0;

    @Override
    public void onTick() {

        if(invincibleTicks > 0) { invincibleTicks--; }

        if(invisibleTicks > 0) {

            invisibleTicks--;
            if(invisibleTicks < 1) {

                currentDamage = 0;
                setItems();
                updateDamage();
                ((ArmorStand)stand.getBukkitEntity()).setVisible(true);

            }

        }

        // Tracking
        Iterator<Player> iterator = trackers.iterator();
        while (iterator.hasNext()) {

            Player player = iterator.next();
            if(!player.isOnline() || !player.getWorld().equals(getLocation().getWorld()) || player.getLocation().distance(getLocation()) > 64D) {

                iterator.remove();
                if(player.isOnline()) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(stand.getId()));

                }

            }

        }

        for(Player player : getLocation().getWorld().getPlayers()) {

            if(!trackers.contains(player)) {

                if(player.getLocation().distance(getLocation()) <= 64D) {

                    trackers.add(player);
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(stand));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.HEAD, stand.getEquipment(EnumItemSlot.HEAD)));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.CHEST, stand.getEquipment(EnumItemSlot.CHEST)));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.FEET, stand.getEquipment(EnumItemSlot.FEET)));
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true));

                }

            }

        }

    }

    @Override
    public void reset() {

        invisibleTicks = 0;
        currentDamage = 0f;
        updateDamage();
        stand.setCustomNameVisible(true);
        setItems();


    }

    @Override
    public boolean isTargetable() {
        return invisibleTicks == 0 && ((ArmorStand)stand.getBukkitEntity()).isVisible();
    }

    @Override
    public LocationProvider getTargetLocationProvider() {
        return new LocationProvider() {
            @Override
            public Location getLocation() {
                return ((ArmorStand)stand.getBukkitEntity()).getLocation();
            }
            public Vector getLastDelta() { return new Vector(0,0,0); }
        };
    }

    @Override
    public UUID getUUID() {
        return ((ArmorStand)stand.getBukkitEntity()).getUniqueId();
    }

    @Override
    public int getEntityID() {
        return ((ArmorStand)stand.getBukkitEntity()).getEntityId();
    }

    @Override
    public Location getLocation() {
        return ((ArmorStand)stand.getBukkitEntity()).getLocation();
    }

    @Override
    public double height() {
        return ((ArmorStand)stand.getBukkitEntity()).getHeight();
    }

    @Override
    public String getName() {
        return "Dummy";
    }

    @Override
    public Team getTeam() {
        return null;
    }

    @Override
    public DataWatcher getDataWatcher() {
        return getNMSEntity().getDataWatcher();
    }

    @Override
    public Entity getNMSEntity() {
        return stand;
    }
}
