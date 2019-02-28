package de.xenyria.splatoon.ai.entity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.math.SlowRotation;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.servercore.spigot.listener.ProtocolListener;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.navigation.NavigationManager;
import de.xenyria.splatoon.ai.paint.PaintableRegionTracker;
import de.xenyria.splatoon.ai.pathfinding.grid.NodeGrid;
import de.xenyria.splatoon.ai.target.TargetManager;
import de.xenyria.splatoon.ai.task.AITaskController;
import de.xenyria.splatoon.ai.task.signal.SignalManager;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.Equipment;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.objects.Hook;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.TeamEntity;
import de.xenyria.splatoon.game.player.superjump.SuperJump;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftSquid;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityNPC extends SplatoonPlayer implements TeamEntity {

    private static CopyOnWriteArrayList<EntityNPC> npcs = new CopyOnWriteArrayList<>();
    public static void tickNPCs() {

        for(EntityNPC npc : npcs) {

            npc.tick();

        }

    }

    private boolean visibleInTab = true;
    public boolean isVisibleInTab() { return visibleInTab; }
    public void setVisibleInTab(boolean visibleInTab) { this.visibleInTab = visibleInTab; }

    public static CopyOnWriteArrayList<EntityNPC> getNPCs() { return npcs; }

    public void handleProjectileHit(SplatoonProjectile projectile) {

        if(!isSplatted()) {

            if (projectile.getShooter() != null && !projectile.getShooter().isSplatted() && projectile instanceof InkProjectile) {

                if (getTargetManager().getTarget() != null) {

                    if (hasLineOfSight(projectile.getShooter())) {

                        if (RandomUtil.random(80)) {

                            getTargetManager().target(projectile.getShooter());

                        }

                    }

                } else {

                    if (hasLineOfSight(projectile.getShooter())) {

                        getTargetManager().target(projectile.getShooter());

                    }

                }

            }

        }

    }

    public boolean hasLineOfSight(Location location) {

        Vector eyeLoc = getEyeLocation().toVector();
        Vector playerLoc = location.toVector();
        Vector direction = playerLoc.clone().subtract(eyeLoc).normalize();
        if(VectorUtil.isValid(direction)) {

            RayTraceResult result = getWorld().rayTraceBlocks(getEyeLocation(), direction, eyeLoc.distance(playerLoc));
            if(result == null || result.getHitPosition().distance(playerLoc) <= 0.512d) {

                return true;

            } else {

                return false;

            }

        } else {

            return false;

        }

    }

    public boolean hasLineOfSight(SplatoonPlayer player) {

        Vector eyeLoc = getEyeLocation().toVector();
        Vector playerLoc = player.centeredHeightVector();
        Vector direction = playerLoc.clone().subtract(eyeLoc).normalize();
        if(VectorUtil.isValid(direction)) {

            RayTraceResult result = getWorld().rayTraceBlocks(getEyeLocation(), direction, eyeLoc.distance(playerLoc));
            if(result == null || result.getHitPosition().distance(playerLoc) <= 0.05) {

                return true;

            } else {

                return false;

            }

        } else {

            return false;

        }

    }

    public int teamMemberCount() {

        return match.getPlayers(team).size() - 1;

    }
    public ArrayList<SplatoonPlayer> getNearbyTeamMembers(Vector location, double radius) {

        ArrayList<SplatoonPlayer> players = new ArrayList<>();
        for(SplatoonPlayer player : getMatch().getPlayers(team)) {

            if(player != this && player.getLocation().toVector().distance(location) <= radius) {

                players.add(player);

            }

        }
        return players;

    }

    public static void equipmentAsyncTick() {

        for(EntityNPC npc : npcs) {

            if(npc.getEquipment() != null) {

                npc.getEquipment().asyncTick();

            }

        }

    }
    public static void equipmentSyncTick() {

        for(EntityNPC npc : npcs) {

            if(npc.getEquipment() != null) {

                npc.getEquipment().syncTick();

            }

        }

    }

    public boolean isSquidFormAvailable() {

        return !isDead() && !isShooting();

    }

    public long millisSinceLastDamage() {

        return System.currentTimeMillis() - lastDamageTimestamp;

    }

    public double getEyeHeight() { return 1.62; }

    public boolean onTeamTerritory() {

        if(lastInkContactTicks < 10) {

            return true;

        } else {

            if(match.isOwnedByTeam(getLocation().getBlock().getRelative(BlockFace.DOWN), team)) {

                return false;

            }

        }
        return false;

    }

    public boolean isOnOwnInk() {

        Location location = getLocation().clone().add(0, -.5, 0);
        Block block = location.getBlock();
        if(block != null && match.isOwnedByTeam(block, team)) {

            return true;

        }

        return false;

    }

    private boolean trackerActive = true;
    public void disableTracker() {

        trackerActive = false;
        if(squidManager.visualSquid != null) {

            squidManager.visualSquid.remove();
            squidManager.visualSquid = null;

        }

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(nmsEntity.getId()));
            //((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(squidManager.visualSquid.getEntityId()));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(tank.getId()));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nmsEntity));

        }
        trackers.clear();

    }

    public void enableTracker() {

        trackerActive = true;
        if(squidManager.visualSquid == null) {

            squidManager.spawnSquid();

        }

    }

    public void enableAI() {

        aiActive = true;

    }

    public int lastInkContactTicks() { return lastInkContactTicks; }

    public void setLastInkContactTicks(int i) {

        this.lastInkContactTicks = i;

    }

    public void remove() {

        disableAI();
        disableTracker();
        match.removePlayer(this);
        removed = true;
        EntityNPC.getNPCs().remove(this);

        /*
        if(diagnosticStand1 != null && !diagnosticStand1.isDead()) {

            diagnosticStand1.remove();

        }
        if(diagnosticStand2 != null && !diagnosticStand2.isDead()) {

            diagnosticStand2.remove();

        }
        */

    }

    private boolean removed;
    public boolean isRemoved() {

        return removed;

    }

    public void setProperties(AIProperties properties) { this.properties = properties; }

    public void broadcastPositionUpdate() {

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(nmsEntity));

        }

    }

    public boolean estimateEnemyPosition() {

        AIWeaponManager.AIPrimaryWeaponType type = getWeaponManager().getAIPrimaryWeaponType();
        if(type == AIWeaponManager.AIPrimaryWeaponType.CHARGER) { return false; }
        return true;

    }

    private int ticksSinceRespawn = 0;
    public int getTicksSinceRespawn() { return ticksSinceRespawn; }
    private boolean diedOnce = false;

    public void untrack(SplatoonPlayer player) {

        trackers.remove(player);
        sendRemovalPackets(((SplatoonHumanPlayer)player).getPlayer());

    }

    public class SquidManager {

        private Squid visualSquid;
        private EntityNPC npc;

        public void spawnSquid() {

            if(visualSquid == null) {

                visualSquid = (Squid) getLocation().getWorld().spawnEntity(getLocation(), EntityType.SQUID);
                ((CraftSquid)visualSquid).getHandle().noclip = true;

            }

            if(trackerActive) {

                visualSquid.setCustomName(npc.getTeam().getColor().prefix() + npc.getName());
                visualSquid.setCustomNameVisible(true);
                visualSquid.setGravity(false);
                visualSquid.setCollidable(false);
                setSquidInvisible();

            }

        }

        public SquidManager(EntityNPC entityNPC) {

            this.npc = entityNPC;
            visualSquid = (Squid) entityNPC.getLocation().getWorld().spawnEntity(entityNPC.getLocation(), EntityType.SQUID);

            spawnSquid();

        }

        public void tick() {

            if(visualSquid != null) {

                visualSquid.teleport(npc.getLocation());

            }

        }


        public void setSquidInvisible() {

            if(trackerActive) {

                visualSquid.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false, false));

            }

        }
        public void setSquidVisible() {

            if(trackerActive) {

                visualSquid.removePotionEffect(PotionEffectType.INVISIBILITY);

            }

        }

        public boolean isSquidVisible() {

            return trackerActive && !visualSquid.hasPotionEffect(PotionEffectType.INVISIBILITY);

        }

    }

    private boolean leaveRailFlag;
    public void forceRailLeave() { leaveRailFlag = true; }
    public boolean isRidingARail() { return isRidingOnInkRail() || isRidingOnRideRail(); }
    private int rideRailSoundTicker;

    public void updateInventory() {

        // Optisch
        if(heldItemSlot() == 0) {

            currentItemInMainHand = getEquipment().getPrimaryWeapon().asItemStack();
            for(Player player : trackers) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(
                        new PacketPlayOutEntityEquipment(nmsEntity.getId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(currentItemInMainHand)));

            }

        }

    }

    private Vector lastSuperJumpDelta;
    private int superJumpIndex;

    private boolean wallSwimMode = false;
    public boolean isSwimmingOnWall() { return wallSwimMode; }

    public double targetYForSwim = 0d;

    public boolean canSwimWall() {

        return true;

    }

    public void enableWallSwimMode() {

        wallSwimMode = true;

    }

    public void disableWallSwimMode() {

        wallSwimMode = false;

    }

    private boolean squid;

    private Equipment equipment;
    public Equipment getEquipment() { return equipment; }

    @Override
    public double getHealth() {
        return health;
    }

    @Override
    public Location getEyeLocation() {

        Location location = getLocation().add(0, nmsEntity.getHeadHeight(), 0);
        return location;

    }

    @Override
    public void notEnoughInk() {

    }

    @Override
    public boolean canUseMainWeapon() {

        boolean flag = true;
        if(equipment.getSpecialWeapon() != null) {

            flag = !equipment.getSpecialWeapon().isActive();

        }

        return !isBeingDragged() && !isRidingOnInkRail() && flag;

    }

    @Override
    public boolean canUseSecondaryWeapon() {

        boolean flag = true;
        if(equipment.getSpecialWeapon() != null) {

            flag = !equipment.getSpecialWeapon().isActive();

        }

        return !isBeingDragged() && !isRidingOnInkRail() && flag;

    }

    public boolean isSquid() { return squid; }

    private boolean tankVisible = false;
    public void setTankVisible(boolean visible) {

        tankVisible = visible;
        if(visible) {

            for(Player player : trackers) {

                if(ResourcePackUtil.hasCustomResourcePack(player)) {

                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(tank.getId(), EnumItemSlot.HEAD,
                            CraftItemStack.asNMSCopy(new ItemBuilder(Material.GOLDEN_AXE).setDurability(getColor().tankDurabilityValue()).create())));

                }

            }

        } else {

            for(Player player : trackers) {

                if(ResourcePackUtil.hasCustomResourcePack(player)) {

                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(tank.getId(), EnumItemSlot.HEAD,
                            CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR))));

                }

            }

        }

    }

    private int lastFormChangeTicks = 0;

    public void enterSquidForm() {

        lastFormChangeTicks = 0;
        squid = true;
        Block below = getWorld().getBlockAt((int)getLocation().getX(), (int)(getLocation().getY() - .5), (int)getLocation().getZ());
        if(match.isOwnedByTeam(below, team) && !inSuperJump()) {

            lastInkContactTicks = 0;
            squidManager.setSquidInvisible();

        } else {

            squidManager.setSquidVisible();

        }

        nmsEntity.setInvisible(true);
        for(Player player : trackers) {

            //((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(nmsEntity.getId()));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getDataWatcher(), nmsEntity.onGround));
            sendEquipment(player);

        }
        setTankVisible(false);

    }

    public void leaveSquidForm() {


        lastFormChangeTicks = 0;
        setTankVisible(true);

        squid = false;
        squidManager.setSquidInvisible();
        nmsEntity.setInvisible(false);
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(nmsEntity));
            sendMetadataPackets(player);
            sendEquipment(player);

        }

    }

    private void sendMetadataPackets(Player player) {

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getDataWatcher(), isOnGround()));

    }

    private boolean squidFormLock = false;
    public void lockSquidForm() {

        squidFormLock = true;

    }

    public String getName() { return profile.name; }

    @Override
    public boolean isTargetable() {

        return !isSplatted();

    }

    private EntityNPC instance = this;

    @Override
    public LocationProvider getTargetLocationProvider() {
        return new LocationProvider() {

            @Override
            public Location getLocation() {
                return instance.getLocation();
            }

            @Override
            public Vector getLastDelta() {
                return lastPosition;
            }
        };
    }

    public void changeName(String name) {

        this.profile = new NPCProfile(name, UUID.randomUUID(), AISkin.randomSkin(team.getColor()));

    }

    public class NPCProfile {

        private String name;
        public String getName() { return name; }

        private UUID uuid;
        public UUID getUUID() { return uuid; }

        private String skinValue, signature;

        public GameProfile toGameProfile() {

            GameProfile profile = new GameProfile(uuid, name);
            profile.getProperties().put("textures", new Property("textures", skinValue, signature));
            profile.getProperties().put(ProtocolListener.GAMEPROFILE_IGNORE_KEY, new Property("xst", "xst"));

            return profile;

        }

        public NPCProfile(String name, UUID uuid, AISkin.SkinData data) {

            this.name = name;
            this.uuid = uuid;
            this.skinValue = data.getValue();
            this.signature = data.getSignature();

        }

    }

    private NPCProfile profile = null;
    public NPCProfile getProfile() { return profile; }

    @Override
    public UUID getUUID() {
        return profile.uuid;
    }

    @Override
    public boolean isHuman() {
        return false;
    }

    private Hook currentDragPoint;
    public boolean isBeingDragged() { return currentDragPoint != null; }

    private Location spawnPoint;
    public void setSpawnPoint(Location location) {

        this.spawnPoint = location;

    }

    @Override
    public Location getSpawnPoint() { return spawnPoint; }

    private BattleStatistic statistic = new BattleStatistic();
    public BattleStatistic getStatistic() { return statistic; }

    private SquidManager squidManager;
    public SquidManager getSquidManager() { return squidManager; }

    private VelocityProcessor processor = new VelocityProcessor();
    public VelocityProcessor getProcessor() { return processor; }

    public Vector getVelocity() { return processor.getVelocity(); }
    public void setVelocity(Vector vector) { processor.setVelocity(vector); }

    @Override
    public void addSquidVelocity(double v) {

        if(v >= 0) {

            // SetVelocity resettet airticks
            setVelocity(getVelocity().add(new Vector(0, v, 0)));

        } else {

            getVelocity().add(new Vector(0, v, 0));

        }

    }

    @Override
    public void sendActionBar(String s) {}

    @Override
    public void teleport(Location location) {

        nmsEntity.setPosition(location.getX(), location.getY(), location.getZ());
        xrotation.updateAngle(location.getPitch() + 90f);

        float yaw = location.getYaw();
        if(yaw < 0) {

            yaw+=360f;

        } else if(yaw > 360) {

            yaw-=360;

        }
        yrotation.updateAngle(yaw);

        //squidManager.squidPositionArmorStand.setPosition(location.getX(), location.getY(), location.getZ());

    }

    @Override
    public void forceSquidMovement(double x, double y, double z, boolean b) {

        Vector delta = new Vector(x,y,z).subtract(getLocation().toVector().clone());

        move(delta.getX(), delta.getY(), delta.getZ());
        if(b) { nmsEntity.onGround = true; processor.resetAirTicks(); }

    }

    @Override
    public void dragTowards(Hook hook) {

        this.currentDragPoint = hook;

        if(!isSquid()) {

            enterSquidForm();
            squidFormLock = true;

        }

    }

    @Override
    public void sendMessage(String s) {}

    private int itemSlot = 0;
    public void setItemSlot(int i) { this.itemSlot = i; }

    @Override
    public int heldItemSlot() {

        if(isSquid()) { return 2; }

        return itemSlot;

    }

    @Override
    public void specialNotReady() {}

    private double specialPoints = 0;
    public void resetSpecialGauge() {

        setSpecialPoints(0);

    }

    @Override
    public void resetLastInteraction() {}

    @Override
    public void setSpecialPoints(double val) {

        this.specialPoints = val;

    }

    @Override
    public double getSpecialPoints() { return specialPoints; }
    public EntityPlayer getNMSPlayer() { return nmsEntity; }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() { return nmsEntity.getBukkitEntity(); }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public void hitMark(Location location) {}

    @Override
    public void unlockSquidForm() {

        squidFormLock = false;

    }

    public void resetWallSwimProgress() {

        wallSwimMode = false;
        targetYForSwim = 0d;

    }

    public boolean isOnGround() {

        if(inSuperJump()) { return true; }

        return nmsEntity.onGround;

    }

    @Override
    public void onSplat(Color color, @Nullable SplatoonPlayer splatter, @Nullable SplatoonProjectile projectile, int ticks) {

        ticksSinceRespawn = 0;
        diedOnce = true;
        burst(splatter, (splatter != null) ? splatter.getColor() : getColor());

        getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_SQUID_DEATH, 1.4f, 1.4f);
        if(isSquid()) {

            leaveSquidForm();

        }
        respawnTicks = ticks;
        nmsEntity.setInvisible(true);
        for(Player player : trackers) {

            sendEquipment(player);
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(nmsEntity));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getDataWatcher(), nmsEntity.onGround));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(tank.getId()));

        }

        disableWalkSpeedOverride();
        disableWallSwimMode();
        targetManager.reset();
        weaponManager.reset();
        taskController.reset();
        signalManager.reset();
        navigationManager.resetTarget();
        setItemSlot(0);


        if(getMatch().getMatchType() == MatchType.TUTORIAL) {

            getMatch().queuePlayerRemoval(this);

        }

    }

    @Override
    public void setHealth(double health) {

        if(health < this.health) {

            for(Player player : trackers) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(nmsEntity, 1));

            }

        }

        this.health = health;

    }

    @Override
    public void setLastDamageTicks(int amount) {

        this.lastDamageTicks = amount;

    }

    private ArrayList<DamageHistory> history = new ArrayList<>();
    @Override
    public ArrayList<DamageHistory> getLastDamageHistory() {
        return history;
    }

    private long lastDamageTimestamp = 0;
    public void setLastDamage(long l) {

        lastDamageTimestamp = l;
    }

    @Override
    public boolean isValid() { return true; }

    @Override
    public boolean isSubmergedInInk() {
        return !squidManager.isSquidVisible();
    }

    private int lastTrailTicks = 0;
    public boolean isVisibleByTrail() {
        return lastTrailTicks < 10;
    }

    @Override
    public void handleHighlight(TentaMissleTarget target, int i) {

    }

    @Override
    public int getVisibleEntityID() {

        if(isSquid()) {

            return squidManager.visualSquid.getEntityId();

        } else {

            return nmsEntity.getId();

        }

    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public int lastDamageTicker() { return lastDamageTicks; }

    @Override
    public double getInk() { return ink; }

    @Override
    public void addInk(double amount) {

        if(isDebuffed()) { return; }
        this.ink+=amount; if(ink >= 100d) { ink = 100d; } updateLastInkModification();

    }

    private boolean unlimitedInk = false;

    @Override
    public void removeInk(double amount) {

        if(!unlimitedInk) {

            this.ink -= amount;
            if (ink < 0d) { ink = 0d; }

        }
        updateLastInkModification();

    }

    private EntityPlayer nmsEntity;

    @Override
    public double distance(SplatoonProjectile projectile) { return getLocation().distance(projectile.getLocation()); }

    @Override
    public int getEntityID() { return nmsEntity.getId(); }

    private org.bukkit.inventory.ItemStack currentItemInMainHand = new org.bukkit.inventory.ItemStack(Material.AIR);
    public void updateCurrentItemInMainHand(org.bukkit.inventory.ItemStack stack) {

        this.currentItemInMainHand = stack;
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(getEntityID(),
                    EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(stack)));

        }

    }
    public org.bukkit.inventory.ItemStack getCurrentItemInMainHand() { return currentItemInMainHand; }

    @Override
    public void push(double x, double y, double z) {

        getVelocity().add(new Vector(x,y,z));

    }

    public Location getLocation() {

        return new Location(nmsEntity.getBukkitEntity().getWorld(),
                    nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ, nmsEntity.yaw, nmsEntity.pitch);

    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return aabb();
    }

    @Override
    public double height() {
        if(isSquid()) {

            return 0.4;

        } else {

            return 1.8;

        }
    }

    private boolean shooting;
    public boolean isShooting() { return shooting; }
    public void setShooting(boolean value) { this.shooting = value; }

    @Override
    public boolean isSplatted() {
        return respawnTicks > 0;
    }

    public boolean canRideRideRail() { return currentRideRail == null && !specialActive(); }
    public boolean canRideInkRail() { return currentInkRail == null && !specialActive(); }

    @Override
    public boolean hasControl() {
        return !isRidingARail() && !isBeingDragged() && !inSuperJump();
    }

    @Override
    public boolean inSuperJump() {
        return currentJump != null;
    }

    private boolean jumpFlag;
    public boolean canJump() { return !jumpFlag; }

    public void jump(double impulse) {

        jumpFlag = true;
        getVelocity().add(new Vector(0, impulse, 0));

    }

    private Location origin;

    private NavigationManager navigationManager;

    private Match match;
    public Match getMatch() { return match; }

    private Team team;
    public Team getTeam() { return team; }

    @Override
    public DataWatcher getDataWatcher() { return nmsEntity.getDataWatcher(); }

    @Override
    public net.minecraft.server.v1_13_R2.Entity getNMSEntity() { return nmsEntity; }

    private AIWeaponManager weaponManager = new AIWeaponManager(this);
    public AIWeaponManager getWeaponManager() { return weaponManager; }

    private AIProperties properties = new AIProperties(50d, 50d, 30d);
    public AIProperties getProperties() { return properties; }

    private TargetManager targetManager = new TargetManager(this);
    public TargetManager getTargetManager() { return targetManager; }

    public ArrayList<Player> getTrackers() { return trackers; }

    private AITaskController taskController = new AITaskController(this);
    public AITaskController getTaskController() { return taskController; }

    // PaintManager paintManager = new PaintManager(this);
    //public PaintManager getPaintManager() { return paintManager; }

    private static int id;
    public static int nextID() { return id++; }

    private EntityArmorStand tank;

    private SignalManager signalManager = new SignalManager();
    public SignalManager getSignalManager() { return signalManager; }

    private PositionTimeLine timeLine = new PositionTimeLine(this);
    public PositionTimeLine getTimeLine() { return timeLine; }

    public EntityNPC(String name, Location location, Team team, Match match, AIProperties properties) {

        this(name, location, team, match);
        this.properties = properties;

    }

    public int tagEntityID() { return movementArmorStand.getId(); }

    public EntityNPC(String name, Location location, Team team, Match match) {

        this.origin = location.clone();
        this.movementArmorStand = new EntityArmorStand(match.nmsWorld(), location.getX(), location.getY(), location.getZ());
        movementArmorStand.setCustomNameVisible(true);
        movementArmorStand.getBukkitEntity().setCustomName(team.getColor().prefix() + name);
        movementArmorStand.setInvisible(true);
        movementArmorStand.setPosition(location.getX(), location.getY(), location.getZ());

        this.spawnPoint = origin.clone();
        this.match = match;
        this.team = team;
        this.profile = new NPCProfile(name, UUID.randomUUID(), AISkin.randomSkin(team.getColor()));
        GameProfile profile = this.profile.toGameProfile();
        nmsEntity = new EntityPlayer(
                ((CraftWorld)location.getWorld()).getHandle().getMinecraftServer(),
                ((CraftWorld)location.getWorld()).getHandle(),
                profile,
                new PlayerInteractManager(((CraftWorld)location.getWorld()).getHandle())
        );
        nmsEntity.listName = IChatBaseComponent.ChatSerializer.a("{\"text\": \"test\"}");
        nmsEntity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        nmsEntity.onGround = false;
        nmsEntity.recalcPosition();
        nmsEntity.setSilent(false);
        nmsEntity.setCustomNameVisible(true);

        xrotation.updateAngle(location.getPitch() + 90f);
        xrotation.target(location.getPitch() + 90);

        float yaw = location.getYaw();
        if(yaw > 360) { yaw-=360; } else if(yaw < 0) { yaw+=360f; }

        yrotation.target(yaw);
        yrotation.updateAngle(yaw);

        // Equipment
        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(team.getColor().getBukkitColor());
        helmet.setItemMeta(meta);

        org.bukkit.inventory.ItemStack chest = new org.bukkit.inventory.ItemStack(Material.LEATHER_CHESTPLATE);
        meta = (LeatherArmorMeta) chest.getItemMeta();
        meta.setColor(team.getColor().getBukkitColor());
        chest.setItemMeta(meta);

        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(Material.LEATHER_BOOTS);
        meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(team.getColor().getBukkitColor());
        boots.setItemMeta(meta);

        this.helmet = CraftItemStack.asNMSCopy(helmet);
        this.chest = CraftItemStack.asNMSCopy(chest);
        this.boots = CraftItemStack.asNMSCopy(boots);

        squidManager = new SquidManager(this);
        navigationManager = new NavigationManager(this);
        npcs.add(this);
        move(0,0,0);

        tank = new EntityArmorStand(getMatch().nmsWorld(), 0,0,0);
        tank.setInvisible(true);
        tank.setEquipment(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.APPLE)));
        //tank.setPosition(getLocation().getX(), getLocation().getY(), getLocation().getZ());
        //tank.getWorld().removeEntity(tank);
        setTankVisible(true);

        if(DEBUG_MODE) {

            a1 = (ArmorStand) getLocation().getWorld().spawnEntity(getLocation(), EntityType.ARMOR_STAND);
            a2 = (ArmorStand) getLocation().getWorld().spawnEntity(getLocation(), EntityType.ARMOR_STAND);
            a3 = (ArmorStand) getLocation().getWorld().spawnEntity(getLocation(), EntityType.ARMOR_STAND);
            a4 = (ArmorStand) getLocation().getWorld().spawnEntity(getLocation(), EntityType.ARMOR_STAND);
            a1.setCustomNameVisible(true);
            a2.setCustomNameVisible(true);
            a3.setCustomNameVisible(true);
            a4.setCustomNameVisible(true);
            a1.setVisible(false);
            a2.setVisible(false);
            a3.setVisible(false);
            a4.setVisible(false);
            a1.setGravity(false);
            a2.setGravity(false);
            a3.setGravity(false);
            a4.setGravity(false);

        }

        equipment = new Equipment(this);

    }

    public String debugName() {

        return "Entity-NPC (" + nmsEntity.getId() + ") " + team.getColor().prefix() + "Team";

    }

    private PaintableRegionTracker regionTracker = new PaintableRegionTracker(this);
    public PaintableRegionTracker getRegionTracker() { return regionTracker; }

    private EntityArmorStand movementArmorStand = null;

    public ArmorStand a1,a2,a3,a4;
    public static boolean DEBUG_MODE = false;

    public void move(double x, double y, double z) {

        if(x >= 8) { x = 8; } else if(x < -8) { x = -8; }
        if(y >= 8) { y = 8; } else if(y < -8) { y = -8; }
        if(z >= 8) { z = 8; } else if(z < -8) { z = -8; }

        if(getEquipment() != null && getEquipment().getSpecialWeapon() != null && getEquipment().getSpecialWeapon() instanceof Baller) {

            return;

        }

        Vector positionBefore = getLocation().toVector();

        boolean grounded = isOnGround();

        double prevX = nmsEntity.locX;
        double prevY = nmsEntity.locY;
        double prevZ = nmsEntity.locZ;

        x += processor.getVelocity().getX();
        y += processor.getVelocity().getY();
        z += processor.getVelocity().getZ();
        if (grounded && y > 0) { nmsEntity.onGround = false; grounded = false; }

        double targetX = nmsEntity.locX + x;
        double targetY = nmsEntity.locY + y;
        double targetZ = nmsEntity.locZ + z;

        ((CraftWorld)getLocation().getWorld()).getHandle().getChunkAtWorldCoords(new BlockPosition(targetX, targetY, targetZ));

        if(movementArmorStand != null) {

            if(movementArmorStand.getBoundingBox().getFilter() != null) {

                AxisAlignedBB box = movementArmorStand.getBoundingBox();
                if(box.getFilter() == NMSUtil.dragFilter) {

                    if(!isBeingDragged()) {

                        box.setFilter(null);

                    }

                }

            }

        }

        if(!isSquid()) {

            Vector oldPosition = getLocation().toVector();

            // Wrap Resolving
            if (grounded && y >= 0) {

                AxisAlignedBB newBB = new AxisAlignedBB(targetX - .25, targetY, targetZ - .25, targetX + .25, targetY + 1.8, targetZ + .25);
                if (!AABBUtil.hasSpace(nmsEntity.getBukkitEntity().getWorld(), newBB)) {

                    Vector vector = AABBUtil.resolveWrap(getLocation().getWorld(), new Vector(targetX, targetY, targetZ), newBB);
                    if (vector != null) {

                        targetX = vector.getX();
                        targetY = vector.getY();
                        targetZ = vector.getZ();

                    }

                }

            }

            x = targetX - positionBefore.getX();
            y = targetY - positionBefore.getY();
            z = targetZ - positionBefore.getZ();

            nmsEntity.getBoundingBox().setFilter(NMSUtil.ironBarStuckFilter);
            nmsEntity.move(EnumMoveType.SELF, x, y, z);

            Vector newPosition = getLocation().toVector();
            double distance = newPosition.distance(oldPosition);

            boolean hitX = nmsEntity.locX != targetX;
            boolean hitY = nmsEntity.locY != targetY;
            boolean hitZ = nmsEntity.locZ != targetZ;

            if (hitX) { getVelocity().setX(0); }
            if (hitZ) { getVelocity().setZ(0); }
            if (hitY) {

                processor.resetAirTicks();
                if (y < 0) { nmsEntity.onGround = true; }
                getVelocity().setY(0);

            }

            Packet packet = null;
            if (distance >= 7.9d) {

                packet = new PacketPlayOutEntityTeleport(nmsEntity);

            } else {

                // Rel Move
                long x1 = (long) ((newPosition.getX() * 32 - oldPosition.getX() * 32) * 128);
                long y1 = (long) ((newPosition.getY() * 32 - oldPosition.getY() * 32) * 128);
                long z1 = (long) ((newPosition.getZ() * 32 - oldPosition.getZ() * 32) * 128);

                packet = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(nmsEntity.getId(), x1, y1, z1,
                        floatToByte(nmsEntity.yaw),
                        floatToByte(nmsEntity.pitch), isOnGround());

            }
            for (Player player : trackers) {

                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(nmsEntity, floatToByte(nmsEntity.yaw)));

            }

        } else {

            Vector oldPosition = getLocation().toVector();

            // Wrap Resolving
            if (grounded && y >= 0) {

                AxisAlignedBB newBB = new AxisAlignedBB(targetX - .3, targetY, targetZ - .3, targetX + .3, targetY + 1.8, targetZ + .3);
                if (!AABBUtil.hasSpace(nmsEntity.getBukkitEntity().getWorld(), newBB)) {

                    Vector vector = AABBUtil.resolveWrap(getLocation().getWorld(), new Vector(targetX, targetY, targetZ), newBB, false);
                    if (vector != null) {

                        targetX = vector.getX();
                        targetY = vector.getY();
                        targetZ = vector.getZ();

                    }

                }

            }

            x = targetX - positionBefore.getX();
            y = targetY - positionBefore.getY();
            z = targetZ - positionBefore.getZ();

            nmsEntity.getBoundingBox().setFilter(NMSUtil.filter);

            nmsEntity.move(EnumMoveType.SELF, x, y, z);

            boolean hitX = nmsEntity.locX != targetX;
            boolean hitY = nmsEntity.locY != targetY;
            boolean hitZ = nmsEntity.locZ != targetZ;

            if (hitX) { getVelocity().setX(0); }
            if (hitZ) { getVelocity().setZ(0); }
            if (hitY) {

                processor.resetAirTicks();
                if (y < 0) { nmsEntity.onGround = true; }
                getVelocity().setY(0);

            }

            Block block = getBlock(new Vector(nmsEntity.locX, nmsEntity.locY - 0.5, nmsEntity.locZ));
            Location location = getLocation().clone();
            location.setPitch(0f);
            Vector direction = location.getDirection().clone();

            Vector vector = new Vector(nmsEntity.locX, nmsEntity.locY + .3, nmsEntity.locZ).add(direction.clone().multiply(.8));
            Block facingBlock = getBlock(vector);

            boolean bool1 = match.isOwnedByTeam(block, team);
            boolean bool2 = match.isOwnedByTeam(facingBlock, team);

            Block toMarkTrailAt = block;
            if(bool1) { toMarkTrailAt = block; } else if(bool2) { toMarkTrailAt = facingBlock; } else { toMarkTrailAt = null; }

            if(wallSwimMode && !bool2) {

                Block base = getLocation().getBlock();
                for(BlockFace face : NodeGrid.faces) {

                    Block target = base.getRelative(face);
                    if(match.isOwnedByTeam(target, team)) {

                        bool1 = false;
                        bool2 = true;
                        toMarkTrailAt = target;
                        break;

                    }

                }

            }

            if(toMarkTrailAt != null) {

                if(match.isOwnedByTeam(toMarkTrailAt, getTeam())) {

                    lastTrailTicks = 0;
                    if((Math.abs(x) + Math.abs(z)) >= 0.05) {

                        match.markTrail(toMarkTrailAt, team);
                        SplatoonServer.broadcastColorizedBreakParticle(getLocation().getWorld(),
                                nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ, team.getColor());

                    }

                }

            }

            if(bool2) {

                if((Math.abs(x) + Math.abs(z)) >= 0.05) {

                    if (match.isOwnedByTeam(facingBlock, team)) {

                        match.markTrail(facingBlock, team);

                    }
                    SplatoonServer.broadcastColorizedBreakParticle(getLocation().getWorld(),
                            nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ, team.getColor());
                    lastInkContactFacingTicks = 0;

                }

                if(wallSwimMode) {

                    double delta = (targetYForSwim - getLocation().getY());
                    double absDelta = Math.abs(delta);
                    double maxDel = 0.0625d;
                    if(absDelta > maxDel) {

                        if(delta < 0) { delta = -maxDel; } else { delta = maxDel; }

                    }
                    if(getVelocity().getY() <= delta) {

                        getVelocity().add(new Vector(0, delta, 0));
                        processor.resetAirTicks();

                    }

                }

            }

            if(bool1) {

                if(lastInkContactTicks > 10) {

                    SplatoonServer.broadcastColorizedBreakParticle(getWorld(), getLocation().getX(), getLocation().getY(), getLocation().getZ(), getTeam().getColor());

                }

                lastInkContactTicks = 0;
                lastEnemyInkContactTicks = 99;

            }

            Vector newPosition = getLocation().toVector();
            Packet packet = null;
            double distance = newPosition.distance(oldPosition);

            if (distance >= 7.9d) {

                packet = new PacketPlayOutEntityTeleport(nmsEntity);

            } else {

                // Rel Move
                long x1 = (long) ((newPosition.getX() * 32 - oldPosition.getX() * 32) * 128);
                long y1 = (long) ((newPosition.getY() * 32 - oldPosition.getY() * 32) * 128);
                long z1 = (long) ((newPosition.getZ() * 32 - oldPosition.getZ() * 32) * 128);

                packet = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(nmsEntity.getId(), x1, y1, z1,
                        floatToByte(nmsEntity.yaw),
                        floatToByte(nmsEntity.pitch), isOnGround());

            }
            for (Player player : trackers) {

                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(nmsEntity, floatToByte(nmsEntity.yaw)));

            }

        }
        hasMovedInThisTick = true;

        // Last Delta
        Vector last = new Vector(prevX, prevY, prevZ);
        Vector newLoc = new Vector(nmsEntity.locX, nmsEntity.locY, nmsEntity.locZ);
        lastPosition = newLoc.subtract(last);
        if(!isSplatted()) {

            for(Player player : trackers) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(movementArmorStand));

            }

        }

    }

    private int lastSquidSoundTicks = 0;
    private int lastInkContactTicks = 50;
    private int lastInkContactFacingTicks = 0;

    public static final double TRACKING_RANGE = 96D;

    public void sendRemovalPackets(Player player) {

        try {

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(
                    nmsEntity.getId()
            ));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(
                    movementArmorStand.getId()
            ));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(
                    tank.getId()
            ));

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void manageTracking() {

        if(trackerActive) {

            // Alte Einträge
            Iterator<Player> iterator = trackers.iterator();
            while (iterator.hasNext()) {

                Player player = iterator.next();
                if (!player.isOnline() || !player.getWorld().equals(nmsEntity.getBukkitEntity().getWorld()) || player.getLocation().distance(getLocation()) > TRACKING_RANGE) {

                    iterator.remove();
                    sendRemovalPackets(player);

                }

            }

            // Neue Einträge
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!trackers.contains(player)) {

                    if (player.getWorld().equals(nmsEntity.getBukkitEntity().getWorld()) && player.getLocation().distance(getLocation()) <= TRACKING_RANGE) {

                        trackers.add(player);
                        final Player playerCopy = player;


                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nmsEntity));
                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            if (trackers.contains(playerCopy)) {

                                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(nmsEntity));
                                sendSpawnPackets(playerCopy);

                            }
                            if(!visibleInTab) {

                                Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                                    if(trackers.contains(playerCopy)) {

                                        System.out.println("Despawn sent");
                                        ((CraftPlayer)playerCopy).getHandle().playerConnection.sendPacket(
                                                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER,
                                                        nmsEntity)
                                        );

                                    }

                                }, 3l);

                            }

                        }, 3l);

                    }

                }

            }

        }

    }

    private ArrayList<Player> trackers = new ArrayList<>();

    public static byte floatToByte(float val) {

        return (byte)((256F / 360F) * val);

    }

    private boolean hasMovedInThisTick = false;

    private double health = 100d;
    public boolean isDead() { return health <= 0d; }

    public Block getBlock(int x, int y, int z) {

        return nmsEntity.getBukkitEntity().getWorld().getBlockAt(x, y, z);

    }

    public Block getBlock(Vector vector) {

        return getBlock((int)vector.getX(), (int)vector.getY(), (int)vector.getZ());

    }

    public static ArrayList<PushableEntity> getNearbyEntities(EntityNPC npc) {

        ArrayList<PushableEntity> arrayList = new ArrayList<>();
        for(SplatoonPlayer player : npc.getMatch().getAllPlayers()) {

            if(player != npc && player.getWorld().equals(npc.nmsEntity.getBukkitEntity().getWorld()) && player.getLocation().distance(npc.getLocation()) < 1d) {

                arrayList.add(player);

            }

        }
        return arrayList;

    }

    public void pushEntities() {

        int pushes = 0;
        for(PushableEntity entity : getNearbyEntities(this)) {

            if(pushes < 8) {

                AxisAlignedBB bb = entity.getBoundingBox();
                if(bb.c(nmsEntity.getBoundingBox())) {

                    Vector delta = getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(-0.05);
                    if(VectorUtil.isValid(delta)) {

                        entity.push(delta.getX(), delta.getY(), delta.getZ());
                        pushes++;

                    }

                }

            }

        }

    }

    private int debugTicker = 0;

    public boolean hasFloorBelow() {

        AxisAlignedBB aabb = new AxisAlignedBB(getLocation().getX() - .3, getLocation().getY() - .001, getLocation().getZ() - .3, getLocation().getX() + .3, getLocation().getY() + 1.8, getLocation().getZ() + .3);
        BlockPosition position = new BlockPosition(getLocation().getX(), getLocation().getY() - .001, getLocation().getZ());
        World world = ((CraftWorld)nmsEntity.getBukkitEntity().getWorld()).getHandle();
        IBlockData data = world.getTypeIfLoaded(position);
        VoxelShape shape = data.getCollisionShape(world, position);
        if(shape != null && !shape.isEmpty()) {

            for(Object bb : shape.d()) {

                AxisAlignedBB blockBB = (AxisAlignedBB) bb;
                AxisAlignedBB newAABB = new AxisAlignedBB(position.getX() + blockBB.minX, position.getY() + blockBB.minY, position.getZ() + blockBB.minZ, position.getX() + blockBB.maxX, position.getY() + blockBB.maxY, position.getZ() + blockBB.maxZ);
                if(aabb.c(newAABB)) {

                    return true;

                }

            }

        }
        return false;

    }

    private double ink = 100d;

    private int nextJumpTicker = 0;

    public void ejectFromRideRail() {

        currentRideRail = null;

    }
    public void ejectFromInkRail(boolean jump) {

        currentInkRail = null;

        if(jump) {

            setVelocity(new Vector(0, 0.8, 0));

        }

    }

    public boolean isAtEndOfRail() {

        int indx = getRidingVectorIndex();
        if(isRidingOnRideRail()) {

            if((indx) >= (currentRideRail.getTrack().size() - 1)) {

                return true;

            }

        } else if(isRidingOnInkRail()) {

            if((indx) >= (currentInkRail.getTrack().size() - 1)) {

                return true;

            }

        }
        return false;

    }

    private boolean aiActive = true;
    public void disableAI() { aiActive = false; }

    private int lastEnemyInkContactTicks = 0;

    //public ArmorStand diagnosticStand1, diagnosticStand2;

    public int getLastFormChangeTicks() {

        return lastFormChangeTicks;

    }

    private SlowRotation yrotation = new SlowRotation();
    private SlowRotation xrotation = new SlowRotation();

    public void tick() {

        if(a1 != null) {

            a1.teleport(getLocation().clone().add(0, 1, 0));
            a2.teleport(getLocation().clone().add(0, .75, 0));
            a3.teleport(getLocation().clone().add(0, .5, 0));
            a4.teleport(getLocation().clone().add(0, .25, 0));

        }

        if(respawnTicks < 1) {

            ticksSinceRespawn++;

        }

        if(match != null) {

            if(match.inIntro() || match.inOutro() || (match instanceof BattleMatch && !((BattleMatch)match).getAIStartFlag())) {

                return;

            }

        }

        lastFormChangeTicks++;

        tank.locX = nmsEntity.locX;
        tank.locY = nmsEntity.locY;
        tank.locZ = nmsEntity.locZ;
        tickHighlights();

        //getLocation().getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, getLocation().getX(), getLocation().getY(), getLocation().getZ(), 0);
        if(!isDead()) {

            tickDebuff();
            tickInkArmor();
            timeLine.tick();
            updateLastSafe();
            if(getWeaponManager() != null) {


            }
            if(!xrotation.isReached()) {

                xrotation.rotate(2f);
                nmsEntity.pitch = xrotation.getAngle() + 90f;

            }
            if(!yrotation.isReached()) {

                yrotation.rotate(2f);
                nmsEntity.yaw = yrotation.getAngle();

            }

            lastTrailTicks++;
            lastEnemyInkContactTicks++;

            signalManager.tick();
            getWeaponManager().tick();
            targetManager.tick();
            if(aiActive) {

                taskController.tick();

            }

            Block block = getBlock(new Vector(nmsEntity.locX, nmsEntity.locY - 0.5, nmsEntity.locZ));
            if(match.isEnemyTurf(block, team)) {

                lastEnemyInkContactTicks = 0;

                if(getHealth() >= 50d && lastDamageTicks > 4) {

                    lastDamageTicks = 0;
                    double health = getHealth();
                    health -= 5d;
                    if(health <= 50d) { health = 50d; }
                    setHealth(health);

                }

            }

            processRegeneration();
            if(superJumpStartDelay > 0) {

                superJumpStartDelay--;
                if (superJumpStartDelay < 1) {

                    superJumpIndex = 0;

                    // Launch
                    getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.65f);
                    for (int i = 0; i < 10; i++) {

                        double offX = new Random().nextDouble() * .5;
                        double offY = new Random().nextDouble() * .5;
                        double offZ = new Random().nextDouble() * .5;

                        SplatoonServer.broadcastColorParticleExplosion(getWorld(), getLocation().getX() + offX,
                                getLocation().getY() + offY,
                                getLocation().getZ() + offZ, team.getColor());

                    }

                }

            } else {

                if(currentJump != null) {

                    if ((superJumpIndex + 1) < currentJump.getTrajectory().getVectors().size()) {

                        if(!getSquidManager().isSquidVisible()) {

                            getSquidManager().setSquidVisible();

                        }

                        nmsEntity.onGround = true;
                        processor.resetAirTicks();

                        superJumpIndex++;
                        Vector3f vec3f = currentJump.getTrajectory().getVectors().get(superJumpIndex);
                        Vector targetVec = new Vector(vec3f.x, vec3f.y, vec3f.z);

                        Vector beginVec = getLocation().toVector();
                        Vector trueDelta = beginVec.subtract(targetVec).multiply(-1);
                        lastSuperJumpDelta = trueDelta.clone();

                        if(VectorUtil.isValid(trueDelta)) {

                            Location location = new Location(getWorld(),0,0,0);
                            location.setDirection(trueDelta);
                            nmsEntity.yaw = location.getYaw();
                            nmsEntity.pitch = location.getPitch();

                        }

                        teleport(new Location(getWorld(), targetVec.getX(), targetVec.getY(), targetVec.getZ(), nmsEntity.yaw, nmsEntity.pitch));

                    } else {

                        Location oldLoc = getLocation().clone();
                        updateInventory();
                        currentJump = null;
                        for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

                            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutRemoveEntityEffect(getEntityID(),
                                        MobEffects.INVISIBILITY));
                            sendEquipment(player.getPlayer());

                        }

                        if(executeAfterJump != null) { executeAfterJump.run(); }
                        squidFormLock = false;
                        superJumpIndex = 0;

                        nmsEntity.onGround = true;
                        processor.resetAirTicks();

                        if(wasSquidBeforeJump) {

                            teleport(oldLoc);
                            enterSquidForm();

                        } else {

                            if (lastSuperJumpDelta != null) {

                                setVelocity(lastSuperJumpDelta.clone().multiply(0.2));

                            }

                        }

                    }

                }

            }

            // Rails / Hooks
            if (isRidingOnRideRail()) {

                boolean invalid = false;
                if(currentRideRail.getOwningTeam() == null || currentRideRail.getOwningTeam() != team) { invalid = true; }

                if (leaveRailFlag || invalid) {

                    leaveRailFlag = false;
                    ejectFromRideRail();

                } else {

                    rideRailSoundTicker++;
                    if (rideRailSoundTicker > 30) {

                        rideRailSoundTicker = 0;
                        getWorld().playSound(getLocation(), Sound.ENTITY_MINECART_RIDING, 0.2f, 2f);

                    }

                    SplatoonServer.broadcastColorizedBreakParticle(getWorld(), getLocation().getX(),
                            getLocation().getY(),
                            getLocation().getZ(),
                            team.getColor());
                    updateInkRailRidingIndex(getRidingVectorIndex() + 1);
                    Vector nextPosition = currentRideRail.vectorFor(getRidingVectorIndex());

                    if (nextPosition == null) {

                        ejectFromRideRail();

                    } else {

                        if (nextPosition.distance(getLocation().toVector()) >= 0.5) {

                            updateRidingIndex(getRidingVectorIndex() - 1);

                        }

                        processor.resetAirTicks();
                        processor.setVelocity(new Vector());

                        if(!isSquid()) {

                            Vector delta = getLocation().toVector().subtract(nextPosition).normalize().multiply(-0.26);
                            move(delta.getX(), delta.getY(), delta.getZ());

                        } else {

                            //((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutMount(squidCameraPosition));
                            this.forceSquidMovement(nextPosition.getX(), nextPosition.getY(), nextPosition.getZ(), false);
                            setVelocity(new Vector(0,0,0));

                        }

                    }

                }

            } else if(isRidingOnInkRail()) {

                boolean invalid = false;
                if(currentInkRail.getOwningTeam() == null || currentInkRail.getOwningTeam() != team) { invalid = true; }

                SplatoonServer.broadcastColorizedBreakParticle(getWorld(), getLocation().getX(),
                        getLocation().getY(),
                        getLocation().getZ(),
                        team.getColor());
                updateRidingIndex(getRidingVectorIndex() + 2);
                if(!invalid) {

                    if(getRidingVectorIndex() >= (currentInkRail.getTrack().size() - 1)) {

                        invalid = true;

                    }

                }

                if(!isSquid() || invalid) {

                    ejectFromInkRail(false);

                } else {

                    Vector targetVector = currentInkRail.vectorFor(getRidingVectorIndex());
                    if(targetVector != null) {

                        Vector delta = new Vector(
                                targetVector.getX() - getLocation().getX(),
                                targetVector.getY() - getLocation().getY(),
                                targetVector.getZ() - getLocation().getZ()
                        );
                        processor.resetAirTicks();
                        processor.setVelocity(new Vector());
                        move(delta.getX(), delta.getY(), delta.getZ());

                    } else {

                        ejectFromInkRail(false);

                    }

                }

            } else if(isBeingDragged()) {

                Vector current = getLocation().toVector();
                Vector delta = currentDragPoint.delta(getLocation().toVector()).multiply(-0.34);
                forceSquidMovement(current.getX() + delta.getX(), current.getY() + delta.getY(), current.getZ() + delta.getZ(), true);

                if(currentDragPoint.distance(getLocation().toVector()) < 0.3) {

                    currentDragPoint = null;
                    leaveSquidForm();
                    squidFormLock = false;

                }

            }

            if(isSquid()) {

                Location location = getLocation().clone();
                location.setPitch(0f);
                Vector direction = location.getDirection().clone();

                Vector vector = new Vector(nmsEntity.locX, nmsEntity.locY + .3, nmsEntity.locZ).add(direction.clone().multiply(.8));
                Block facingBlock = getBlock(vector);

                boolean bool1 = match.isOwnedByTeam(block, team);
                boolean bool2 = match.isOwnedByTeam(facingBlock, team);

                if(bool1) {

                    lastInkContactTicks = 0;

                }
                if(bool2) {

                    lastInkContactTicks = 0;
                    lastInkContactFacingTicks = 0;

                }

                if(lastInkContactTicks < 10) {

                    ink+=BASE_INK_CHARGE_VALUE;
                    if(ink >= 100) { ink = 100; }

                }

                if(!squidManager.isSquidVisible()) {

                    if(lastInkContactTicks > 10 || isOnEnemyTurf()) {

                        squidManager.isSquidVisible();

                    }

                } else {

                    if(!inSuperJump() && lastInkContactTicks < 10 && !isOnEnemyTurf()) {

                        squidManager.setSquidInvisible();

                    }

                }

            } else {

                ink+=HUMAN_INK_CHARGE_VALUE;
                if(ink >= 100) { ink = 100; }

            }

            navigationManager.tick();
            squidManager.tick();

            if(inSuperJump() && !squidManager.isSquidVisible()) {

                squidManager.setSquidVisible();

            }

            /*
            if(!isSquid()) {

                nmsEntity.onGround = hasFloorBelow();

            } else {

                nmsEntity.onGround = hasFloorBelow();

            }*/
            nmsEntity.onGround = hasFloorBelow();


            //if(debugTicker > 60) {

                //debugTicker = 0;
               // getVelocity().add(new Vector(0, 0.25, 0));

            //}

            // Death checks
            if(nmsEntity.locY <= 0 || getBlock(getLocation().toVector()).isLiquid()) {

                hurt(10d);
                if(isDead()) { return; }

            }

            if (processor.hasVelocity() || !isOnGround() || !hasMovedInThisTick) {

                move(0, 0, 0);

            }

            manageTracking();
            pushEntities();

            processor.setGrounded(isOnGround());
            processor.process();

            // Flags zurücksetzen
            hasMovedInThisTick = false;

            if(hasControl()) {

                if(isSquid() && !squidManager.isSquidVisible()) {

                    if(isOnEnemyTurf()) {

                        squidManager.setSquidVisible();

                    }

                }

            }

        } else {

            if(squidManager.isSquidVisible()) {

                squidManager.setSquidInvisible();

            }

            setShooting(false);
            respawnTicks--;
            if(respawnTicks < 1) {

                respawn();

            }

        }

        if(isDead() || isOnGround()) {

            nextJumpTicker++;
            jumpFlag = false;
            nextJumpTicker = 0;

        }

        // Ticker
        if(lastDamageTicks > 0) {

            lastDamageTicks--;

        }
        lastInkContactTicks++;
        lastInkContactFacingTicks++;
        debugTicker++;

        if(movementArmorStand != null) {

            movementArmorStand.locX=getLocation().getX();
            movementArmorStand.locY=getLocation().getY();
            movementArmorStand.locZ=getLocation().getZ();

        }

    }

    private boolean isOnEnemyTurf() {

        Location location = getLocation().clone().add(0, -.2, 0);
        Block block = location.getBlock();
        return match.isEnemyTurf(block, team);

    }

    private boolean walkSpeedOverride;
    private float walkSpeedOverrideValue;

    @Override
    public void enableWalkSpeedOverride() { walkSpeedOverride = true; }
    public void disableWalkSpeedOverride() { walkSpeedOverride = false; }
    public boolean doOverrideWalkspeed() { return walkSpeedOverride; }

    public void updateEquipment() {



    }

    @Override
    public void setOverrideWalkSpeed(float walkSpeed) { walkSpeedOverrideValue = walkSpeed; }

    private Vector lastPosition = new Vector();
    public Vector getLastDelta() {
        return lastPosition;
    }

    @Override
    public double getSquidVelocityY() {
        return getVelocity().getY();
    }

    private int rideRailRidingIndex = 0;
    public void updateInkRailRidingIndex(int indx) { rideRailRidingIndex=indx; }

    @Override
    public void forceNMSSquidPosition(Location location) {

    }

    @Override
    public void joinMatch(Match match) {

        this.match = match;
        match.addPlayer(this);

    }

    @Override
    public void leaveMatch() {

        this.match.removePlayer(this);
        match = null;

    }

    @Override
    public boolean superJump(Location location, int ticks) {
        return superJump(location, ticks, new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public boolean superJump(Location location, Runnable runnable) {
        return superJump(location, 26, runnable);
    }

    private Runnable executeAfterJump;
    private boolean wasSquidBeforeJump;
    private int superJumpStartDelay = 0;

    @Override
    public boolean superJump(Location location, int ticks, Runnable runnable) {

        executeAfterJump = runnable;

        currentJump = new SuperJump(getLocation().toVector(), location.toVector());
        if(currentJump.calculate()) {

            if(!isSquid()) {

                enterSquidForm();

            }

            squidFormLock = true;
            Location spawnLoc = getLocation().clone();
            spawnLoc.setY(spawnLoc.getY() - 1.625);

            for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(getEntityID(),
                        getNMSEntity().getDataWatcher(), isOnGround()));
                for(EnumItemSlot slot : EnumItemSlot.values()) {

                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(nmsEntity.getId(), slot, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR))));

                }
                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityEffect(nmsEntity.getId(), new MobEffect(MobEffects.INVISIBILITY, 2, 99999, false, false, false)));

            }

            wasSquidBeforeJump = isSquid();
            superJumpStartDelay = ticks;
            return true;

        } else {

            currentJump = null;

        }
        return false;
    }

    private int lastDamageTicks = 0;

    public void hurt(double dmg) {

        if(lastDamageTicks > 4) {

            lastDamageTicks = 0;
            health-=dmg;

            if(health <= 0d) {

                kill();

            }

            for(Player player : trackers) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(nmsEntity, 1));

            }

        }

    }

    private int respawnTicks = 0;
    public void respawn() {

        health = 100d;
        nmsEntity.setPositionRotation(origin.getX(), origin.getY(), origin.getZ(), origin.getYaw(), origin.getPitch());
        nmsEntity.yaw = 0f;
        nmsEntity.pitch = 0f;
        setTankVisible(true);

        for(Player player : trackers) {

            sendSpawnPackets(player);

        }

        timeLine.reset();
        targetManager.reset();
        weaponManager.reset();
        taskController.reset();
        signalManager.reset();
        navigationManager.resetTarget();
        regionTracker.reset();
        disableWalkSpeedOverride();
        disableWallSwimMode();

        if(getNMSEntity().isInvisible()) {

            getNMSEntity().setInvisible(false);
            for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(getEntityID(),
                        getNMSEntity().getDataWatcher(), isOnGround()));
                player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(nmsEntity));


            }

        }

    }

    public ItemStack helmet,chest,boots;

    public void sendEquipment(Player player) {

        for (EnumItemSlot slot : EnumItemSlot.values()) {

            ItemStack stack = nmsEntity.getEquipment(slot);
            if(!isSquid() && !isSplatted()) {

                if (slot == EnumItemSlot.MAINHAND) {

                    stack = CraftItemStack.asNMSCopy(currentItemInMainHand);

                } else if (slot == EnumItemSlot.HEAD) {

                    stack = CraftItemStack.asNMSCopy(getEquipment().getHeadGear().asItemStack(getColor()));

                } else if (slot == EnumItemSlot.CHEST) {

                    stack = CraftItemStack.asNMSCopy(getEquipment().getBodyGear().asItemStack(getColor()));

                } else if (slot == EnumItemSlot.FEET) {

                    stack = CraftItemStack.asNMSCopy(getEquipment().getFootGear().asItemStack(getColor()));

                }

            } else {

                stack = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR));

            }

            if(getArmorHealth() != 0d && (slot == EnumItemSlot.HEAD || slot == EnumItemSlot.CHEST || slot == EnumItemSlot.FEET)) {

                org.bukkit.inventory.ItemStack stack1 = CraftItemStack.asBukkitCopy(stack);
                ItemMeta meta = stack1.getItemMeta();
                meta.addEnchant(Enchantment.DURABILITY, 1, false);
                stack1.setItemMeta(meta);
                stack = CraftItemStack.asNMSCopy(stack1);

            }

            if (stack == null) {
                stack = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR));
            }
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(nmsEntity.getId(), slot, stack));

        }

    }

    private void sendSpawnPackets(Player player) {

        nmsEntity.lastYaw = nmsEntity.yaw;
        nmsEntity.aS = nmsEntity.yaw;

        if(!isSquid()) {

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getDataWatcher(), isOnGround()));
            sendEquipment(player);

        }

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(movementArmorStand));
        player.sendMessage("Recv: " + movementArmorStand);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(movementArmorStand.getId(), movementArmorStand.getDataWatcher(), false));

        if(ResourcePackUtil.hasCustomResourcePack(player)) {

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(tank));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(tank.getId(), tank.getDataWatcher(), false));

            if(tankVisible) {

                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(tank.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new ItemBuilder(Material.GOLDEN_AXE).setDurability(getColor().tankDurabilityValue()).create())));

            } else {

                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityEquipment(tank.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR))));

            }

            try {

                PacketContainer container = new PacketContainer(PacketType.Play.Server.MOUNT);
                container.getIntegers().write(0, nmsEntity.getId());
                container.getIntegerArrays().write(0, new int[]{tank.getId()});
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

    }

    public float getMovementSpeed() {

        double mod = 1d;
        if(isDebuffed()) { mod = SplatoonPlayer.DEBUFF_SPEED_MOD; }
        double val = 0d;

        if(!squid) {

            if(!walkSpeedOverride) {

                if (lastEnemyInkContactTicks < 3) {

                    val = 0.125f;

                } else {

                    val = 0.25f;

                }

            } else {

                val = walkSpeedOverrideValue;

            }

        } else {

            if(!walkSpeedOverride) {

                if(lastEnemyInkContactTicks < 3) {

                    val = 0.06f;

                } else {

                    if (lastInkContactTicks < 10) {

                        val = swimmingSpeed;

                    } else {

                        val = 0.12f;

                    }

                }

            } else {

                val = walkSpeedOverrideValue;

            }

        }

        return (float) (val*mod);

    }

    private float swimmingSpeed = 0.42f;

    public void kill() {

        respawnTicks = 300;
        health = 0d;

        trackers.clear();
        nmsEntity.setInvisible(true);
        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(nmsEntity.getId(), nmsEntity.getDataWatcher(), false));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(nmsEntity));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(tank.getId()));

        }

    }

    public NavigationManager getNavigationManager() { return navigationManager; }

    public float targetYaw, targetPitch;

    public void updateAngles(float yaw, float pitch) {

        this.targetYaw = yaw;
        this.targetPitch = pitch;

        nmsEntity.yaw = yaw;
        nmsEntity.pitch = pitch;

        float yaw1 = yaw;
        if(yaw1 < 0) {

            yaw1+=360f;

        } else if(yaw1 > 360) {

            yaw1-=360;

        }

        float eYaw = nmsEntity.yaw;
        if(eYaw < 0) {

            eYaw+=360f;

        } else if(eYaw > 360) {

            eYaw-=360;

        }

        xrotation.updateAngle(nmsEntity.pitch+90f);
        xrotation.target(targetPitch+90f);
        yrotation.updateAngle(eYaw);
        yrotation.target(yaw1);

        for(Player player : trackers) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(
                    nmsEntity, floatToByte(yaw)
            ));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                    nmsEntity.getId(), (long)0, (long)0, (long)0, floatToByte(yaw), floatToByte(pitch), nmsEntity.onGround
            ));

        }

    }

}
