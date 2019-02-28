package de.xenyria.splatoon.game.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.Title;
import com.mojang.authlib.GameProfile;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.servercore.spigot.player.XenyriaSpigotPlayer;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.AISkin;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.Equipment;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSetRegistry;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.Hook;
import de.xenyria.splatoon.game.objects.InkRail;
import de.xenyria.splatoon.game.objects.RideRail;
import de.xenyria.splatoon.game.player.scoreboard.EntityHighlightController;
import de.xenyria.splatoon.game.player.scoreboard.PlayerScoreboardManager;
import de.xenyria.splatoon.game.player.superjump.SuperJump;
import de.xenyria.splatoon.game.player.userdata.UserData;
import de.xenyria.splatoon.game.player.userdata.inventory.UserInventory;
import de.xenyria.splatoon.game.player.userdata.inventory.gear.GearItem;
import de.xenyria.splatoon.game.player.userdata.inventory.set.WeaponSetItem;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import de.xenyria.splatoon.lobby.SplatoonLobby;
import de.xenyria.splatoon.lobby.npc.RecentPlayer;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftSquid;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Squid;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplatoonHumanPlayer extends SplatoonPlayer {

    private static CopyOnWriteArrayList<SplatoonHumanPlayer> humanPlayers = new CopyOnWriteArrayList<>();

    private boolean walkSpeedOverrideActive;
    public void enableWalkSpeedOverride() { walkSpeedOverrideActive = true; }
    public void disableWalkSpeedOverride() { walkSpeedOverrideActive = false; }
    public boolean doOverrideWalkspeed() { return walkSpeedOverrideActive; }

    private ActiveEffect currentEffect;
    public ActiveEffect getCurrentEffect() { return currentEffect; }

    public static class ActiveEffect {

        public String getName() {

            return type.getName() + " (" + getModifier(level) + "x) (Noch für " + remainingMatches + ((remainingMatches==1) ? "Match" : "Matches") + ")";

        }

        public static enum Type {

            EXPERIENCE("Erfahrungsbooster"),
            MONEY("Talerbooster"),
            GEAR_ABILITY("Ausrüstungsbooster");

            private String name;
            public String getName() { return name; }

            Type(String name) {

                this.name = name;

            }

        }

        public static float getModifier(int level) {

            switch (level) {

                case 1: return 1.0f;
                case 2: return 1.5f;
                case 3: return 2f;
                case 4: return 3f;

            }
            return 1f;

        }

        private Type type;
        public Type getType() { return type; }

        private int remainingMatches;
        public int getRemainingMatches() { return remainingMatches; }

        private int level;
        public int getLevel() { return level; }

        private SpecialEffect effect;
        public SpecialEffect getEffect() { return effect; }
        public boolean isAbilityEffect() { return effect != null; }

        public ActiveEffect(Type type, int remainingMatches, int level, @Nullable SpecialEffect effect) {

            this.type = type;
            this.remainingMatches = remainingMatches;
            this.level = level;
            this.effect = effect;

        }

    }

    private EntityArmorStand tank;
    public EntityArmorStand getTank() { return tank; }

    public boolean isTankVisible() {

        return getMatch() != null && !(getMatch() instanceof SplatoonLobby);

    }

    private float overrideWalkSpeed;
    public void setOverrideWalkSpeed(float speed) { this.overrideWalkSpeed = speed; }

    public final float WALK_SPEED_DEFAULT = 0.25f;

    private static boolean flag = false;

    private UserInventory inventory = new UserInventory(this);
    public UserInventory getInventory() { return inventory; }

    public SplatoonHumanPlayer(XenyriaSpigotPlayer spigotPlayer, Player resolve) {

        this.spigotPlayer = spigotPlayer;
        WeaponSetItem.fromSet(this, WeaponSetRegistry.getSet(1), true);
        GearItem.createItem(this, 1, GearItem.StoredGearData.fromBase(1), true);
        GearItem.createItem(this, 2, GearItem.StoredGearData.fromBase(2), true);
        GearItem.createItem(this, 3, GearItem.StoredGearData.fromBase(3), true);

        this.player = resolve;
        spawnPoint = resolve.getLocation().clone();
        equipment = new Equipment(this);

        refillRecentPlayerPool();
        tank = new EntityArmorStand(((CraftWorld)getWorld()).getHandle());
        tank.setInvisible(true);

    }

    private EntityHighlightController highlightController = new EntityHighlightController(this);
    public EntityHighlightController getHighlightController() { return highlightController; }


    public static CopyOnWriteArrayList<SplatoonHumanPlayer> getHumanPlayers() { return humanPlayers; }

    private Hook targetHook;
    public boolean isBeingDragged() { return targetHook != null; }

    public static SplatoonHumanPlayer getPlayer(Player player) {

        for(SplatoonHumanPlayer player1 : humanPlayers) {

            if(player1.getPlayer() == player) {

                return player1;

            }

        }
        return null;

    }


    private Player player;
    public Player getPlayer() { return player; }


    private Team team;
    public Team getTeam() { return team; }

    @Override
    public Vector getVelocity() {
        return player.getVelocity();
    }

    @Override
    public void setVelocity(Vector vector) {
        player.setVelocity(vector);

    }

    public void setTeam(Team team) {

        Team oldTeam = this.team;
        this.team = team;
        if(match.getMatchController() != null) {

            match.getMatchController().teamChanged(this, oldTeam, team);

        }

    }

    @Override
    public DataWatcher getDataWatcher() {
        return ((CraftPlayer)player).getHandle().getDataWatcher();
    }

    @Override
    public Entity getNMSEntity() {
        return ((CraftPlayer)player).getHandle();
    }


    private int rideRailPunishTicks = 0, inkRailPunishTicks = 0;
    private int rideRailSoundTicker = 0;
    public void ejectFromInkRail(boolean jump) {

        currentInkRail = null;
        inkRailPunishTicks = 10;
        updateRidingIndex(0);
        player.setAllowFlight(false);

        if(jump) {

            squidVelocityY = 0.8;
            player.setVelocity(new Vector(0, 0.8, 0));

        }

    }

    public void ejectFromRideRail() {

        currentRideRail = null;
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        Vector vector = player.getVelocity();
        vector.setY(0.4);
        player.setVelocity(vector);

        updateRidingIndex(0);
        rideRailPunishTicks = 15;
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);

    }

    // SUPER JUMP
    private SuperJump currentJump;
    private int superJumpIndex = 0;
    private int superJumpStartDelay = 0;
    public boolean inSuperJump() { return currentJump != null && superJumpIndex < currentJump.getTrajectory().getVectors().size(); }

    private boolean squidFormLocked = false;

    public boolean superJump(Location target) {

        return superJump(target, 26);

    }

    public boolean superJump(Location location, Runnable runnable) {

        return superJump(location, 26, runnable);

    }

    public boolean superJump(Location target, int i) {

        return superJump(target, i, () -> {});

    }

    private Runnable executeAfterJump;
    public boolean superJump(Location target, int i, Runnable runnable) {

        executeAfterJump = runnable;

        currentJump = new SuperJump(getPlayer().getLocation().toVector(), target.toVector());
        if(currentJump.calculate()) {

            if(!isSquid()) {

                enterSquidForm();

            }

            squidFormLocked = true;
            player.getInventory().clear();
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false));

            if(superJumpRidingStand != null) {

                superJumpRidingStand.remove();
                NMSUtil.broadcastEntityRemovalToSquids(superJumpRidingStand);

            }
            Location spawnLoc = player.getLocation().clone();
            spawnLoc.setY(spawnLoc.getY() - 1.625);

            wasSquidBeforeJump = isSquid();
            superJumpRidingStand = (ArmorStand) target.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            superJumpRidingStand.setCustomNameVisible(false);
            superJumpRidingStand.setVisible(false);
            superJumpRidingStand.setGravity(false);
            superJumpRidingStand.addPassenger(player);
            superJumpStartDelay = i;
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
            player.setFlying(false);
            //player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false, false));
            return true;

        } else {

            currentJump = null;

        }
        return false;

    }

    private long lastMatchSwitch = 0;
    public long millisSinceMatchSwitch() { return System.currentTimeMillis()-lastMatchSwitch; }
    private Match match;
    public Match getMatch() { return match; }
    public void joinMatch(Match match) {

        this.match = match;
        match.addPlayer(this);
        lastMatchSwitch = System.currentTimeMillis();

    }
    public void leaveMatch() {

        setArmorHealth(0d, false);
        updateEquipment();
        this.match.removePlayer(this);
        setTeam(null);
        player.setFlying(false);
        player.setFlySpeed(.2f);
        player.setWalkSpeed(.2f);
        getEquipment().resetWeapons();
        getEquipment().unassignWeapons();

        player.setAllowFlight(false);
        match = null;

    }

    private ArmorStand superJumpRidingStand = null;
    private Vector lastSuperJumpDelta = null;
    private boolean wasSquidBeforeJump = false;
    private boolean swimFlag = false;
    private Vector lastPosition = new Vector();
    private boolean lastPosSet = false;

    private Vector lastDelta = new Vector();
    public Vector getLastDelta() { return lastDelta; }

    public double getSquidVelocityY() { return squidVelocityY; }

    public void forceNMSSquidPosition(Location location) {

        if(squidPositionStand != null) {

            squidPositionStand.setPosition(location.getX(), location.getY(), location.getZ());
            squidCameraPosition.setPosition(location.getX(), location.getY(), location.getZ());

        }

    }

    private ArrayList<DamageHistory> lastDamageList = new ArrayList<DamageHistory>();

    private int lastDamageTicker = 0;
    private int lastEnemyTurfDamageTicker = 0;
    private boolean inEnemyTurf = false;

    private PlayerScoreboardManager scoreboardManager = new PlayerScoreboardManager(this);
    public PlayerScoreboardManager getScoreboardManager() { return scoreboardManager; }

    public void tick() {

        tank.yaw = player.getLocation().getYaw();
        if(getMatch() != null) {

            if(!isSpectator()) {

                inventory.tick();

                tickHighlights();
                scoreboardManager.tick();
                if (!isSplatted()) {

                    tickDebuff();
                    if (!isSquid() && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {

                        player.removePotionEffect(PotionEffectType.INVISIBILITY);

                    }

                    updateLastSafe();
                    tickInkArmor();

                }

                lastTrailTicks++;
                if (lastDamageTicker > 0) {
                    lastDamageTicker--;
                }
                if (lastEnemyTurfDamageTicker > 0) {
                    lastEnemyTurfDamageTicker--;
                }

                Iterator<DamageHistory> iterator = lastDamageList.iterator();
                while (iterator.hasNext()) {

                    DamageHistory history = iterator.next();
                    history.remainingTicks--;
                    if (history.remainingTicks < 1) {

                        iterator.remove();

                    }

                }

                processRegeneration();
                if (!lastPosSet) {

                    lastPosition = getPlayer().getLocation().toVector();
                    lastPosSet = true;

                } else {

                    lastDelta = getPlayer().getLocation().toVector().subtract(lastPosition);
                    lastPosition = getPlayer().getLocation().toVector();

                }

                if (isSquid()) {
                    getPlayer().setVelocity(new Vector());
                }

                double val = 0d;
                double mod = 1d;
                if (isDebuffed()) {
                    mod = SplatoonPlayer.DEBUFF_SPEED_MOD;
                }
                if (doOverrideWalkspeed()) {

                    if (inEnemyTurf) {

                        val = (float) (overrideWalkSpeed * .5);

                    } else {

                        val = (overrideWalkSpeed);

                    }

                } else {

                    if (inEnemyTurf) {

                        val = (float) (WALK_SPEED_DEFAULT * .5);

                    } else {

                        float speed = WALK_SPEED_DEFAULT;
                        if (getEquipment().getPrimaryWeapon() != null && isShooting() && getEquipment().getPrimaryWeapon().isSelected()) {

                            SplatoonPrimaryWeapon weapon = getEquipment().getPrimaryWeapon();
                            float offset = weapon.getMovementSpeedOffset();
                            if (offset != 0f) {

                                speed += offset;

                            }

                        }
                        val = (speed);

                    }

                }
                player.setWalkSpeed((float) (val * mod));

                Vector loc = locationVector();
                if (loc.getY() < 0) {

                    if (getMatch() != null && !isSplatted()) {

                        splat(getTeam().getColor(), null, null);

                    }

                }

                if (getHighlightController() != null) {

                    getHighlightController().tick();

                }

                boolean isRiding = isRidingOnInkRail() || isRidingOnRideRail();

                if (isSquid()) {

                    floorInkCheck();

                } else {

                    Block block = locationVector().toLocation(getPlayer().getWorld()).getBlock().getRelative(BlockFace.DOWN);
                    if (getMatch().isEnemyTurf(block, team)) {

                        inEnemyTurf = true;
                        if (health > 50 && lastEnemyTurfDamageTicker < 1) {

                            health -= 3d;

                            double ratio = (health / 100d);
                            if (ratio > 1) {
                                ratio = 1;
                            }
                            double hp = 0.1 + (19.9 * ratio);
                            getPlayer().setHealth(hp);
                            lastDamage = System.currentTimeMillis();
                            lastEnemyTurfDamageTicker = 6;

                        }

                    } else {

                        inEnemyTurf = false;

                    }

                }


                if (visualSquid != null) {

                    boolean remove = false;
                    if ((lastInkContactTicks < 2 || !isSquid() || isRiding) && (!inSuperJump() && !isBeingDragged())) {

                        remove = true;

                    }
                    if (remove) {

                        removeVisualSquid();

                    } else {

                        Vector vector = locationVector();
                        if (inSuperJump()) {

                            vector = vector.clone().add(new Vector(0, ARMORSTAND_SQUID_Y_OFFSET, 0));

                        }

                        Location location = new Location(getPlayer().getWorld(), vector.getX(), vector.getY(), vector.getZ());
                        location.setPitch(0f);
                        if (inSuperJump()) {

                            location.setYaw(currentJump.yaw(player.getWorld()));

                        } else {

                            location.setYaw(player.getLocation().getYaw());

                        }

                        visualSquid.teleport(location);
                        visualSquid.setVelocity(new Vector());

                    }

                } else {

                    if (isSquid() && !isRiding) {

                        if (lastInkContactTicks > 2 || inSuperJump() || isBeingDragged()) {

                            spawnVisualSquid();

                        }

                    }

                }

                if (!isSplatted()) {

                    // Display
                    double inkToDisplay = ink;
                    if (inkToDisplay < 0) {
                        inkToDisplay = 0d;
                    }
                    if (inkToDisplay > 100) {
                        inkToDisplay = 100d;
                    }
                    player.setExp((float) (inkToDisplay / 100f));

                    if (currentJump != null) {

                        if (superJumpStartDelay < 1) {

                            player.sendActionBar("§7Supersprung in Vorgang...");

                            if ((superJumpIndex + 1) < currentJump.getTrajectory().getVectors().size()) {

                                superJumpIndex++;
                                Vector3f vec3f = currentJump.getTrajectory().getVectors().get(superJumpIndex);
                                Vector targetVec = new Vector(vec3f.x, vec3f.y, vec3f.z);

                                EntityArmorStand as = ((CraftArmorStand) superJumpRidingStand).getHandle();
                                Vector beginVec = new Vector(as.locX, as.locY, as.locZ);
                                Vector trueDelta = beginVec.subtract(targetVec).multiply(-1);
                                lastSuperJumpDelta = trueDelta.clone();
                                as.move(EnumMoveType.SELF, trueDelta.getX(), trueDelta.getY(), trueDelta.getZ());

                                //superJumpRidingStand.teleport(new Location(getPlayer().getWorld(), targetVec.getX(), targetVec.getY(), targetVec.getZ()));

                            } else {


                                postJumpCall();

                            }

                        } else {

                            superJumpStartDelay--;
                            if (superJumpStartDelay < 1) {

                                // Launch
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.65f);
                                for (int i = 0; i < 10; i++) {

                                    double offX = new Random().nextDouble() * .5;
                                    double offY = new Random().nextDouble() * .5;
                                    double offZ = new Random().nextDouble() * .5;

                                    SplatoonServer.broadcastColorParticleExplosion(player.getWorld(), player.getLocation().getX() + offX,
                                            player.getLocation().getY() + offY,
                                            player.getLocation().getZ() + offZ, team.getColor());

                                }

                            }

                        }

                    }

                    if (player.getGameMode() == GameMode.ADVENTURE) {

                        if (!squidFormLocked) {

                            if (player.getInventory().getHeldItemSlot() == 2) {

                                if (!isSquid()) {

                                    enterSquidForm();

                                }

                            } else {

                                if (isSquid()) {

                                    leaveSquidForm();

                                }

                            }

                        }

                    }

                    if (rideRailPunishTicks > 0) {
                        rideRailPunishTicks--;
                    }
                    if (inkRailPunishTicks > 0) {
                        inkRailPunishTicks--;
                    }

                    if (isRidingOnRideRail()) {

                        boolean invalid = false;
                        if (currentRideRail.getOwningTeam() == null || currentRideRail.getOwningTeam() != team) {
                            invalid = true;
                        }

                        if (player.isSneaking() || invalid) {

                            ejectFromRideRail();

                        } else {

                            rideRailSoundTicker++;
                            if (rideRailSoundTicker > 30) {

                                rideRailSoundTicker = 0;
                                player.playSound(player.getLocation(), Sound.ENTITY_MINECART_RIDING, 0.2f, 2f);

                            }

                            SplatoonServer.broadcastColorizedBreakParticle(player.getWorld(), locationVector().getX(),
                                    locationVector().getY(),
                                    locationVector().getZ(),
                                    team.getColor());
                            updateRidingIndex(getRidingVectorIndex()+1);
                            Vector nextPosition = currentRideRail.vectorFor(getRidingVectorIndex());

                            if (nextPosition == null) {

                                ejectFromRideRail();

                            } else {

                                if (nextPosition.distance(locationVector()) >= 0.5) {

                                    updateRidingIndex(getRidingVectorIndex()-1);

                                }

                                if (!isSquid()) {

                                    Vector delta = locationVector().subtract(nextPosition).normalize().multiply(-0.26);
                                    player.setVelocity(delta);

                                } else {

                                    //((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutMount(squidCameraPosition));
                                    this.forceSquidMovement(nextPosition.getX(), nextPosition.getY(), nextPosition.getZ(), false);
                                    squidPositionStand.setNoGravity(true);
                                    squidCameraPosition.setNoGravity(true);

                                    EntityPlayer player = ((CraftPlayer) getPlayer()).getHandle();

                            /*try {

                                Field vehicleField = player.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredField("vehicle");
                                vehicleField.setAccessible(true);
                                vehicleField.weapons(player, squidCameraPosition);

                            } catch (Exception e) {

                                e.printStackTrace();

                            }*/

                                    squidVelocityY = 0d;

                                }

                            }

                        }

                    } else if (isRidingOnInkRail()) {

                        boolean invalid = false;
                        if (currentInkRail.getOwningTeam() == null || currentInkRail.getOwningTeam() != team) {
                            invalid = true;
                        }

                        if (!isSquid() || invalid) {

                            ejectFromInkRail(false);

                        } else {

                            Vector targetVector = currentInkRail.vectorFor(getRidingVectorIndex());
                            if (targetVector != null) {

                                Vector delta = new Vector(
                                        targetVector.getX() - squidPositionStand.locX,
                                        targetVector.getY() - squidPositionStand.locY,
                                        targetVector.getZ() - squidPositionStand.locZ
                                );
                                moveSquidPositionStand(delta.getX(), delta.getY(), delta.getZ());

                            } else {

                                ejectFromInkRail(false);

                            }

                        }

                    } else if (isBeingDragged()) {

                        Vector current = locationVector();
                        Vector delta = targetHook.delta(locationVector()).multiply(-0.34);
                        forceSquidAbsMovement(current.getX() + delta.getX(), current.getY() + delta.getY(), current.getZ() + delta.getZ(), true);

                        if (targetHook.distance(locationVector()) < 0.3) {

                            targetHook = null;
                            player.getInventory().setHeldItemSlot(0);
                            leaveSquidForm();
                            squidFormLocked = false;

                        }

                    }

                    if (isSquid()) {

                        if (hasControl()) {

                            if (!movedInThisTick) {

                                handleSquidFalling();
                                moveSquidPositionStand(0, squidVelocityY, 0);

                            } else {

                                movedInThisTick = false;

                            }

                        }

                        Location location = new Location(player.getWorld(), locationVector().getX(), locationVector().getY(), locationVector().getZ());

                        if (lastInkContactTicks < 2) {

                            addInk(BASE_INK_CHARGE_VALUE);

                        }

                        // Positionupdate
                        squidCameraPosition.locX = squidPositionStand.locX;
                        squidCameraPosition.locY = squidPositionStand.locY - ARMORSTAND_SQUID_Y_OFFSET;
                        squidCameraPosition.locZ = squidPositionStand.locZ;
                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(squidCameraPosition));

                    } else {

                        if (elapsedSecondsSinceLastInkModification() > 2) {

                            addInk(HUMAN_INK_CHARGE_VALUE);
                            resetLastInkModification();

                        }

                    }

                } else {

                    if (splatRespawnTicker > 0) {

                        splatRespawnTicker--;
                        splatFocusYaw += .8f;
                        updatePositionForSplatCamera();
                        PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(fakeCamera);
                        getNMSPlayer().setPosition(fakeCamera.locX, fakeCamera.locY, fakeCamera.locZ);
                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(teleport);
                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(fakeCamera,
                                (byte) ((fakeCamera.yaw * (0.70333F)))));

                        String str = SplatoonServer.formatFloat(splatRespawnTicker / 20d);
                        if (!str.contains(",")) {
                            str += ",0";
                        }

                        player.sendActionBar("§e" + str + " Sek. §7zum Wiedereinstieg");

                    } else {

                        splatted = false;

                        respawnCall();

                    }

                }

            } else {

                if(spectatorEntity != null && spectatedPlayer != null && spectatorCameraReady) {

                    if(spectatorCameraMode == SpectatorCameraMode.ROTATE) {

                        player.sendActionBar("§eAnsicht: Rotierend §8| §7Zum ändern §eSHIFT §7drücken.");
                        if(player.isSneaking()) {

                            if(match instanceof BattleMatch) {

                                if((player.getOpenInventory() == null)) {

                                    BattleMatch match1 = (BattleMatch) match;
                                    match1.openSpectateMenu(player, spectatedPlayer);

                                }

                            }

                        }
                        spectatorCameraYaw+=1f;
                        Vector target = spectatedPlayer.centeredHeightVector();
                        Vector direction = DirectionUtil.yawAndPitchToDirection(spectatorCameraYaw, -20f);
                        Vector cursor = target.clone();
                        for(double cameraDistance = 0.5d; cameraDistance < 5d; cameraDistance+=.25d) {

                            Vector start = cursor.clone();

                            if(getMatch().getWorldInformationProvider().rayTraceBlocks(start, direction, .25d, true) == null) {

                                cursor.add(direction.normalize().multiply(.25d));

                            } else {

                                break;

                            }

                        }

                        Vector cameraPos = cursor.clone();
                        Vector cameraDir = target.clone().subtract(cursor).normalize();
                        if(VectorUtil.isValid(cameraDir)) {

                            float[] floats = DirectionUtil.directionToYawPitch(cameraDir);
                            spectatorEntity.yaw = floats[0];
                            spectatorEntity.pitch = floats[1];

                        }
                        spectatorEntity.locX = cameraPos.getX();
                        spectatorEntity.locY = cameraPos.getY();
                        spectatorEntity.locZ = cameraPos.getZ();
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(spectatorEntity,
                                ((byte)(spectatorEntity.yaw*0.70333))));
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(spectatorEntity));

                    } else if(spectatorCameraMode == SpectatorCameraMode.FIRST_PERSON) {

                        player.sendActionBar("§eAnsicht: Erste Person §8| §7Zum ändern §eSHIFT §7drücken.");
                        if(player.isSneaking()) {

                            if(match instanceof BattleMatch) {

                                if((player.getOpenInventory() == null)) {

                                    BattleMatch match1 = (BattleMatch) match;
                                    match1.openSpectateMenu(player, spectatedPlayer);

                                }

                            }

                        }
                        int cameraIDForPlayer = spectatedPlayer.getVisibleEntityID();

                        PacketContainer container = new PacketContainer(PacketType.Play.Server.CAMERA);
                        container.getIntegers().write(0, cameraIDForPlayer);
                        try {

                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, container, false);

                        } catch (Exception e) {

                            e.printStackTrace();

                        }
                        spectatorCameraId = cameraIDForPlayer;

                    } else if(spectatorCameraMode == SpectatorCameraMode.FIXED) {

                        player.sendActionBar("§eAnsicht: Fixiert §8| §7Zum ändern §eSHIFT §7drücken.");
                        if(player.isSneaking()) {

                            if(match instanceof BattleMatch) {

                                if((player.getOpenInventory() == null)) {

                                    BattleMatch match1 = (BattleMatch) match;
                                    match1.openSpectateMenu(player, spectatedPlayer);

                                }

                            }

                        }

                        Vector target = spectatedPlayer.centeredHeightVector().add(new Vector(0, .4, 0));
                        Vector direction = DirectionUtil.yawAndPitchToDirection(spectatedPlayer.yaw()-180f, -5f);
                        Vector cursor = target.clone();
                        for(double cameraDistance = 0.5d; cameraDistance < 5d; cameraDistance+=.25d) {

                            Vector start = cursor.clone();

                            if(getMatch().getWorldInformationProvider().rayTraceBlocks(start, direction, .25d, true) == null) {

                                cursor.add(direction.normalize().multiply(.25d));

                            } else {

                                break;

                            }

                        }

                        Vector cameraPos = cursor.clone();
                        Vector cameraDir = target.clone().subtract(cursor).normalize();
                        if(VectorUtil.isValid(cameraDir)) {

                            float[] floats = DirectionUtil.directionToYawPitch(cameraDir);
                            spectatorEntity.yaw = floats[0];
                            spectatorEntity.pitch = floats[1];

                        }
                        spectatorEntity.locX = cameraPos.getX();
                        spectatorEntity.locY = cameraPos.getY();
                        spectatorEntity.locZ = cameraPos.getZ();
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(spectatorEntity,
                                ((byte)(spectatorEntity.yaw*0.70333))));
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(spectatorEntity));

                    }

                }

            }

        }

    }

    public int getVisibleEntityID() {

        if(visualSquid != null && !visualSquid.isDead()) {

            return visualSquid.getEntityId();

        } else {

            return player.getEntityId();

        }

    }

    private float spectatorCameraYaw = 0f;
    private int spectatorCameraId = 0;

    private SpectatorCameraMode spectatorCameraMode = SpectatorCameraMode.FIXED;

    @Override
    public void updateEquipment() {

        updateInventory();

    }

    public void respawnCall() {

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutCamera(((CraftPlayer) player).getHandle()));
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(fakeCamera.getId()));
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        fakeCamera = null;
        updateInventory();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(spawnPoint);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        health = 100d;
        player.setHealth(20d);
        addInk(100d);

    }

    private void postJumpCall() {

        Location oldLoc = superJumpRidingStand.getLocation().clone().add(0, 1.6, 0);
        if (superJumpRidingStand != null) {

            superJumpRidingStand.remove();
            NMSUtil.broadcastEntityRemovalToSquids(superJumpRidingStand);

        }

        updateInventory();
        currentJump = null;
        if(executeAfterJump != null) { executeAfterJump.run(); }
        squidFormLocked = false;
        superJumpIndex = 0;

        if(wasSquidBeforeJump) {

            player.teleport(oldLoc);
            ((CraftPlayer)player).getHandle().setPosition(
                    oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()
            );
            enterSquidForm();

        } else {

            if (lastSuperJumpDelta != null) {

                player.setVelocity(lastSuperJumpDelta.clone().multiply(0.2));

            }

            // TODO Remove afterwards
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);


        }

    }

    public boolean hasControl() {

        return currentJump == null && currentInkRail == null && currentRideRail == null && !isBeingDragged();

    }

    private int fallEnterTicker = 0;
    public void handleSquidFalling() {

        if(squidPositionStand.onGround) {

            fallingTicks = 0;
            boolean enter = squidVelocityY < -0.5;
            squidVelocityY = 0;

            if(enter) {

                fallEnterTicker++;
                //if(fallEnterTicker < 1) {

                    player.getWorld().playSound(player.getLocation(),
                            Sound.AMBIENT_UNDERWATER_ENTER, 1f, 1f);
                    for (int i = 0; i < 5; i++) {

                        Vector vector = locationVector();
                        if(team != null) {

                            SplatoonServer.broadcastColorParticleExplosion(player.getWorld(),
                                    vector.getX(), vector.getY(), vector.getZ(), team.getColor());

                        }

                    }

                //}

            }

        } else {

            fallEnterTicker = 0;
            fallingTicks++;
            squidVelocityY-=0.083d;
            if(squidVelocityY < -0.8) {

                squidVelocityY = -0.8;

            }

        }

    }

    private Location spawnPoint;
    public Location getSpawnPoint() { return spawnPoint; }

    @Override
    public BattleStatistic getStatistic() {
        return statistic;
    }

    public void setSpawnPoint(Location spawnPoint) { this.spawnPoint = spawnPoint; }

    public boolean canRideRideRail() { return currentRideRail == null && rideRailPunishTicks < 1 && !specialActive(); }
    public boolean canRideInkRail() { return currentInkRail == null && inkRailPunishTicks < 1 && !specialActive(); }

    public void moveSquidPositionStand(double d0, double d1, double d2) {

        if(isBeingDragged()) {

            squidPositionStand.getBoundingBox().setFilter(NMSUtil.dragFilter);

        } else {

            squidPositionStand.getBoundingBox().setFilter(NMSUtil.filter);

        }

        squidPositionStand.move(EnumMoveType.SELF, d0, d1, d2);
        squidCameraPosition.locX = squidPositionStand.locX;
        squidCameraPosition.locY = squidPositionStand.locY - ARMORSTAND_SQUID_Y_OFFSET;
        squidCameraPosition.locZ = squidPositionStand.locZ;
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(squidCameraPosition));

    }
    public void moveSquidAbsPositionStand(double d0, double d1, double d2) {

        if(isBeingDragged()) {

            squidPositionStand.getBoundingBox().setFilter(NMSUtil.dragFilter);

        } else {

            squidPositionStand.getBoundingBox().setFilter(NMSUtil.filter);

        }

        squidPositionStand.setPosition(d0, d1, d2);
        squidPositionStand.locX = d0;
        squidPositionStand.locY = d1;
        squidPositionStand.locZ = d2;
        squidCameraPosition.locX = squidPositionStand.locX;
        squidCameraPosition.locY = squidPositionStand.locY - ARMORSTAND_SQUID_Y_OFFSET;
        squidCameraPosition.locZ = squidPositionStand.locZ;
        System.out.println("ABS: " + d0 + " " + d1 + " " + d2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(squidCameraPosition));

    }

    private long lastInteraction = 0;
    public void updateLastInteraction() { lastInteraction = System.currentTimeMillis(); }
    public int shootDelayTolerance() {

        if(equipment.getSecondaryWeapon() != null && equipment.getSecondaryWeapon().isSelected()) {

            return 150;

        }

        if(equipment.getPrimaryWeapon() != null && equipment.getPrimaryWeapon() instanceof AbstractRoller && ((AbstractRoller) equipment.getPrimaryWeapon()).isRolling()) {

            if(!(equipment.getPrimaryWeapon() instanceof AbstractRoller)) {

                return 500;

            }

        }

        return 333;

    }
    public boolean isShooting() { return (lastInteraction + shootDelayTolerance()) > System.currentTimeMillis(); }

    private double health = 100d;
    public double getHealth() { return health; }

    @Override
    public Location getEyeLocation() {
        return getPlayer().getEyeLocation().clone();
    }

    private BattleStatistic statistic = new BattleStatistic();
    private long lastDamage;

    public long millisSinceLastDamage() {

        return System.currentTimeMillis() - lastDamage;

    }

    private int splatRespawnTicker = 0;

    private boolean splatted = false;
    public boolean isSplatted() { return splatted; }

    private EntityPlayer fakeCamera;

    private Location lastSplatFocusPosition = null;
    private SplatoonPlayer lastSplatter = null;
    private float splatFocusYaw = 0f;

    public void updatePositionForSplatCamera() {

        if(fakeCamera != null) {

            Vector targetLoc = splatCameraFocusPosition().toVector();
            Vector current = splatCameraAirPosition().toVector();

            Vector directional = current.clone().subtract(targetLoc).multiply(-1).normalize();
            Location location = new Location(player.getWorld(), 0, 0, 0);
            location.setDirection(directional);

            fakeCamera.locX = current.getX();
            fakeCamera.locY = current.getY();
            fakeCamera.locZ = current.getZ();
            fakeCamera.yaw = location.getYaw();
            fakeCamera.pitch = location.getPitch();

        }

    }

    public Location splatCameraAirPosition() {

        float angle = -45f;

        Vector startPos = splatCameraFocusPosition().toVector();
        Location dir = new Location(player.getWorld(), 0d, 0d, 0d);
        dir.setPitch(angle);
        dir.setYaw(splatFocusYaw);

        Vector direction = dir.getDirection();

        double dist = 3.5d;
        double cursor = 0d;
        boolean hit = false;
        while (cursor < dist && !hit) {

            cursor+=0.5d;

            Vector targetLoc = startPos.clone().add(direction.clone().multiply(cursor));
            Block block = player.getWorld().getBlockAt(targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());
            if(!block.isEmpty() && !block.isPassable()) {

                cursor-=0.5d;
                hit = true;

            }

        }

        Vector targetVector = startPos.clone().add(direction.clone().multiply(cursor));
        return new Location(player.getWorld(), targetVector.getX(), targetVector.getY(), targetVector.getZ());

    }

    public Location splatCameraFocusPosition() {

        if(lastSplatter == null) {

            return lastSplatFocusPosition;

        } else {

            lastSplatFocusPosition = lastSplatter.getLocation();
            return lastSplatFocusPosition;

        }

    }

    public String coloredName() {

        /*if(xenyriaPlayer.hasNickname()) {

            return team.getColor().prefix() + getXenyriaPlayer().getNickname();

        } else {

            return team.getColor().prefix() + getPlayer().getName();

        }*/

        return team.getColor().prefix() + getPlayer().getName();

    }

    public static final ItemStack TRANSFORM_TO_SQUID = new ItemBuilder(Material.INK_SAC).setDisplayName("§7§lTintenfischform").addToNBT("NoClick", true).create();

    private Equipment equipment;
    public Equipment getEquipment() { return equipment; }
    public void updateInventory() {

        player.getInventory().clear();

        if(match != null && match.getMatchController() != null) {

            match.getMatchController().addGUIItems(this);

        }

        if(equipment != null) {


            if (equipment.getPrimaryWeapon() != null) {

                player.getInventory().setItem(0, equipment.getPrimaryWeapon().asItemStack());

            } else {

                player.getInventory().setItem(0, null);

            }
            if (equipment.getSecondaryWeapon() != null) {

                player.getInventory().setItem(1, equipment.getSecondaryWeapon().asItemStack());

            } else {

                player.getInventory().setItem(1, null);

            }
            if (equipment.getSpecialWeapon() != null) {

                player.getInventory().setItem(3, equipment.getSpecialWeapon().asItemStack());

            } else {

                player.getInventory().setItem(3, null);

            }

            Color color = null;
            if(getMatch() != null && getTeam() != null) {

                color = getTeam().getColor();

            }

            if(!isSquid()) {

                if (equipment.getHeadGear() != null) {

                    ItemStack helmet = equipment.getHeadGear().asItemStack(color);
                    if (getArmorHealth() != 0d) {

                        helmet.addEnchantment(Enchantment.DURABILITY, 1);

                    }

                    player.getInventory().setHelmet(helmet);

                }
                if (equipment.getHeadGear() != null) {

                    ItemStack chest = equipment.getBodyGear().asItemStack(color);
                    if (getArmorHealth() != 0d) {

                        chest.addEnchantment(Enchantment.DURABILITY, 1);

                    }

                    player.getInventory().setChestplate(chest);

                }
                if (equipment.getHeadGear() != null) {

                    ItemStack boots = equipment.getFootGear().asItemStack(color);
                    if (getArmorHealth() != 0d) {

                        boots.addEnchantment(Enchantment.DURABILITY, 1);

                    }

                    player.getInventory().setBoots(boots);

                }

            }

        }

        if(!isSquid()) {

            player.getInventory().setItem(2, TRANSFORM_TO_SQUID);

        } else {

            player.getInventory().setItem(2, null);

        }

    }

    private int tempTaskID = 0;
    public void onSplat(Color color, @Nullable SplatoonPlayer splatter, @Nullable SplatoonProjectile projectile, int ticks) {
        getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_SQUID_DEATH, 1.4f, 1.4f);

        if(color != null) {

            SplatoonPlayer shooter = null;
            if(projectile != null && projectile.getShooter() != null) { shooter = projectile.getShooter(); }

            burst(shooter, color);

        }

        if(!isSplatted()) {

            Vector realLocation = locationVector();
            if(inSuperJump()) {

                currentJump = null;
                executeAfterJump = null;
                superJumpIndex = 0;
                superJumpRidingStand.eject();
                superJumpRidingStand.remove();
                NMSUtil.broadcastEntityRemovalToSquids(superJumpRidingStand);
                superJumpRidingStand = null;

            }

            squidFormLocked = false;
            if(isSquid()) {

                leaveSquidForm();

            }

            player.getInventory().clear();
            player.getInventory().setItem(EquipmentSlot.OFF_HAND, null);
            player.setGameMode(GameMode.ADVENTURE);

            player.setAllowFlight(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false, false));
            player.setFlying(true);
            player.setFlySpeed(0f);
            player.setWalkSpeed(0f);

            if(projectile != null) {

                if (projectile.getReason() == DamageReason.WEAPON) {

                    if (splatter != null) {

                        if (splatter != this) {

                            if (projectile.getWeapon() != null) {

                                Title title = new Title("", "§8" + Characters.SMALL_X + " §7Erledigt von §e" + splatter.coloredName() + " §7mit §8" + projectile.getWeapon().getName() + "§7.", 10, 40, 10);
                                player.sendTitle(title);

                            } else {

                                Title title = new Title("", "§8" + Characters.SMALL_X + " §7Erledigt von §e" + splatter.coloredName() + "§7.", 10, 40, 10);
                                player.sendTitle(title);

                            }

                        }

                    }

                } else if (projectile.getReason() == DamageReason.HUMAN_ERROR) {

                    Title title = new Title("", "§8" + Characters.SMALL_X + " §7Du hast dich selbst erledigt!", 10, 40, 10);
                    player.sendTitle(title);

                } else if (projectile.getReason() == DamageReason.SPAWN_BARRIER) {

                    Title title = new Title("", "§8" + Characters.SMALL_X + " §7Von der Spawnbarriere erledigt!", 10, 40, 10);
                    player.sendTitle(title);

                }

            }

            this.lastSplatter = splatter;
            splatRespawnTicker = 160;
            splatted = true;
            splatFocusYaw = 0f;
            Location realLoc = new Location(player.getWorld(), realLocation.getX(), realLocation.getY(), realLocation.getZ());
            lastSplatFocusPosition = realLoc.clone();

            SplatoonServer.broadcastColorizedBreakParticle(player.getWorld(), realLocation.getX(), realLocation.getY(), realLocation.getZ(), color);
            player.getWorld().playSound(realLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 1f, 1.7f);

            WorldServer server = ((CraftWorld) player.getWorld()).getHandle();

            GameProfile profile = new GameProfile(UUID.randomUUID(), "Kamera");

            fakeCamera = new EntityPlayer(server.getMinecraftServer(), server, profile, new PlayerInteractManager(server));
            fakeCamera.playerInteractManager.setGameMode(EnumGamemode.SPECTATOR);
            fakeCamera.locX = realLoc.getX();
            fakeCamera.locY = realLoc.getY();
            fakeCamera.locZ = realLoc.getZ();
            fakeCamera.yaw = 0f;
            fakeCamera.pitch = 0f;
            fakeCamera.setInvisible(true);

            updatePositionForSplatCamera();

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakeCamera));

            tempTaskID = Bukkit.getScheduler().runTaskTimer(XenyriaSplatoon.getPlugin(), new Runnable() {

                private int iter = 0;

                @Override
                public void run() {

                    iter++;
                    if(iter < 5) {
                        for (int i = 0; i < 10; i++) {

                            float offsetX = new Random().nextFloat() * .5f;
                            if (new Random().nextBoolean()) {
                                offsetX *= -1;
                            }
                            float offsetZ = new Random().nextFloat() * .5f;
                            if (new Random().nextBoolean()) {
                                offsetZ *= -1;
                            }

                            SplatoonServer.broadcastColorParticleExplosion(player.getWorld(), lastSplatFocusPosition.getX() + offsetX, lastSplatFocusPosition.getY() + (i * 0.125), lastSplatFocusPosition.getZ() + offsetZ, color);

                        }

                    } else {

                        Bukkit.getScheduler().cancelTask(tempTaskID);

                    }

                }

            },3l, 3l).getTaskId();

            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(fakeCamera));
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(fakeCamera.getId(), fakeCamera.getDataWatcher(), false));
                PacketPlayOutCamera camera = new PacketPlayOutCamera(fakeCamera);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(camera);
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, fakeCamera));

            }, 3l);

        }

    }

    @Override
    public void setHealth(double health) {

        this.health = health;

    }

    @Override
    public void setLastDamageTicks(int amount) { this.lastDamageTicker = amount; }

    @Override
    public ArrayList<DamageHistory> getLastDamageHistory() { return lastDamageList; }

    @Override
    public void setLastDamage(long l) { lastDamage = l; }

    @Override
    public boolean isValid() {
        return player.isOnline();
    }

    @Override
    public boolean isSubmergedInInk() {
        return lastInkContactTicks < 10;
    }

    private int lastTrailTicks = 0;
    public boolean isVisibleByTrail() {
        return lastTrailTicks < 10;
    }

    @Override
    public void handleHighlight(TentaMissleTarget target, int i) {

        EntityHighlightController.TeamEntry entry = getHighlightController().getEntry(target.getEntityID());
        if (entry != null) {

            if (entry.ticks < 20) {

                entry.ticks += 20;

            }

        } else {

            ChatColor chatColor = ChatColor.DARK_GRAY;
            if (target.getTeam() != null) {

                chatColor = ChatColor.getByChar(target.getTeam().getColor().getColor());

            }
            if(!getHighlightController().isHighlighted(target.getEntityID())) {

                getHighlightController().addEntry(new EntityHighlightController.TeamEntry(target.getName(),
                        chatColor, target.getNMSEntity(), i));

            }

        }

    }

    @Override
    public boolean isSpectator() {

        if(getMatch() != null && getMatch() instanceof BattleMatch) {

            BattleMatch match = (BattleMatch)getMatch();
            return (match.getSpectators().contains(this));

        }
        return false;

    }

    @Override
    public int lastDamageTicker() { return lastDamageTicker; }

    private EntityArmorStand squidPositionStand, squidCameraPosition;
    private boolean movedInThisTick = false;
    public static final double ARMORSTAND_SQUID_Y_OFFSET = 1.75d;

    private Squid visualSquid;
    public void spawnVisualSquid() {

        Vector loc = locationVector();
        Location location = new Location(player.getWorld(),
                loc.getX(), loc.getY(), loc.getZ());
        visualSquid = (Squid) player.getWorld().spawnEntity(location, EntityType.SQUID);
        visualSquid.setCustomNameVisible(true);
        ((CraftSquid)visualSquid).getHandle().noclip = true;

        visualSquid.setCollidable(false);
        visualSquid.setGravity(false);
        // TODO Nick
        visualSquid.setCustomName((team != null ? team.getColor().prefix() : "§8") + getPlayer().getName());

    }
    public void removeVisualSquid() {

        int id = visualSquid.getEntityId();

        for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(id));

        }

        visualSquid.remove();
        NMSUtil.broadcastEntityRemovalToSquids(visualSquid);
        visualSquid = null;


    }

    public void leaveSquidForm() {

        if(!squidFormLocked) {

            player.playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 1f, 1f);
            Vector vector = locationVector().clone().add(new Vector(0, 0.325, 0));

            try {

                PacketContainer container = new PacketContainer(PacketType.Play.Server.MOUNT);
                container.getIntegers().write(0, squidCameraPosition.getId());
                container.getIntegerArrays().write(0, new int[0]);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, container, false);

            } catch (Exception e) {

                e.printStackTrace();

            }

            player.teleport(new Location(player.getWorld(), vector.getX(), vector.getY(), vector.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(squidPositionStand.getId()));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(squidCameraPosition.getId()));
            squidPositionStand = null;
            squidCameraPosition = null;
            player.setGameMode(GameMode.ADVENTURE);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                player.removePotionEffect(PotionEffectType.INVISIBILITY);

            }, 1l);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.setFlying(false);
            player.setAllowFlight(false);

            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                player.sendActionBar("§7Du bist nun ein Mensch!");

            }, 1l);

            // Floating too long fix
            EntityPlayer player = ((CraftPlayer)getPlayer()).getHandle();
            try {

                Field field = player.playerConnection.getClass().getDeclaredField("C");
                field.setAccessible(true);
                field.set(player.playerConnection, 0);

            } catch (Exception e) {

                e.printStackTrace();

            }

        }
        updateInventory();

    }

    public void enterSquidForm() {

        if(!squidFormLocked) {

            getPlayer().setVelocity(new Vector());
            player.playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, 1f, 1f);
            squidPositionStand = new EntityArmorStand(((CraftPlayer) player).getHandle().world, player.getLocation().getX(), player.getLocation().getY() + 0.16, player.getLocation().getZ());
            squidPositionStand.setPosition(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
            squidPositionStand.setSize(.6f, .6f);
            squidCameraPosition = new EntityArmorStand(((CraftPlayer) player).getHandle().world, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
            squidCameraPosition.setPosition(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
            squidCameraPosition.getBukkitEntity().addPassenger(player);

            squidCameraPosition.setInvisible(true);
            squidPositionStand.setNoGravity(false);

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlySpeed(0f);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                player.sendActionBar("§7Du bist nun ein Tintenfisch!");

            }, 1l);
            //System.out.println(((CraftPlayer)player).getHandle().a(squidCameraPosition, false));
            //System.out.println(((CraftPlayer)player).getHandle().getVehicle());
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(squidCameraPosition));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(squidCameraPosition.getId(), squidCameraPosition.getDataWatcher(), squidCameraPosition.onGround));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutMount(squidCameraPosition));
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 5, false, false));

        }
        updateInventory();

    }

    private int fallingTicks = 0;
    private int lastInkContactTicks = 0;
    private double squidVelocityY = 0d;
    public void addSquidVelocity(double y) {

        squidVelocityY+=y;

    }

    @Override
    public void sendActionBar(String s) {

        player.sendActionBar(s);

    }

    @Override
    public void teleport(Location location) {

        player.teleport(location);

    }


    private void forceSquidAbsMovement(double absX, double absY, double absZ, boolean setOnGround) {

        moveSquidAbsPositionStand(absX, absY, absZ);
        if(setOnGround) { squidPositionStand.onGround = true; fallingTicks = 0; }

    }

    public void forceSquidMovement(double x, double y, double z, boolean setOnGround) {

        Vector delta = new Vector(x,y,z).subtract(locationVector().clone());

        moveSquidPositionStand(delta.getX(), delta.getY(), delta.getZ());
        if(setOnGround) { squidPositionStand.onGround = true; fallingTicks = 0; }

    }

    public void floorInkCheck() {

        Location location = new Location(player.getWorld(), locationVector().getX(), locationVector().getY() +.3, locationVector().getZ());
        if (match.isOwnedByTeam(location.getBlock().getRelative(BlockFace.DOWN), team)) {

            swimFlag = true;
            lastInkContactTicks = 0;

        }

    }

    public void handleInput(double x, double z, double origX, boolean space, boolean shift, boolean self) {

        final double initialXMod = origX;
        boolean moved = Math.abs(x) > 0.01 || Math.abs(z) > 0.01 || origX != 0d;
        boolean playSwimSound = false;
        float swimSoundVolume = 0.45f;

        try {

            if(hasControl()) {

                movedInThisTick = true;
                Vector frontPos = player.getEyeLocation().getDirection().clone().setY(0).clone().normalize().multiply(0.7).add(player.getLocation().toVector().clone());
                Block block = player.getWorld().getBlockAt((int) frontPos.getX(), (int) (frontPos.getY() - .3), (int) frontPos.getZ());
                double speedMod = 0.1d;

                Location location = new Location(player.getWorld(), locationVector().getX(), locationVector().getY(), locationVector().getZ());

                boolean inInk = false;
                if (match.isOwnedByTeam(location.getBlock().getRelative(BlockFace.DOWN), team)) {

                    inInk = true;
                    swimFlag = true;
                    lastInkContactTicks = 0;
                    if(moved) {

                        playSwimSound = true;

                    }

                    if (!shift) {

                        speedMod = 0.37d;
                        if(moved) {

                            lastTrailTicks = 0;
                            getMatch().markTrail(location.getBlock().getRelative(BlockFace.DOWN), team);
                            SplatoonServer.broadcastColorizedBreakParticle(player.getWorld(), locationVector().getX(), locationVector().getY(), locationVector().getZ(), getTeam().getColor());

                        }

                    } else {

                        swimSoundVolume = 0.3f;
                        speedMod = 0.13d;

                    }

                } else {

                    lastInkContactTicks++;
                    if (shift) {

                        speedMod = 0.075d;

                    }

                }
                if(fallingTicks > 10 || (!inInk && lastInkContactTicks < 10)) {

                    speedMod*=2.7d;

                }

                if(getEquipment().getSpecialWeapon() != null && getEquipment().getSpecialWeapon() instanceof Jetpack && getEquipment().getSpecialWeapon().isActive()) {

                    speedMod*=0.25;

                }

                x *= speedMod;
                z *= speedMod;

                double yDelta = 0d;

                if (match.isOwnedByTeam(block, team)) {

                    if (player.getLocation().getPitch() < 0) {

                        yDelta = player.getEyeLocation().getDirection().getY() * 0.1f;

                    }

                    if (squidPositionStand.motY < 0) {
                        squidCameraPosition.motY = 0;
                    }

                }

                handleSquidFalling();
                if (squidPositionStand.onGround) {

                    if (space) {

                        if(inInk) {

                            squidVelocityY += 0.5d;

                        } else {

                            squidVelocityY += 0.4d;

                        }

                    }

                    if (squidVelocityY < 0) {
                        squidVelocityY = 0;
                    }

                }

                double targetDeltaY = yDelta + squidVelocityY;
                double targetPositionX = squidPositionStand.locX + x;
                double targetPositionY = squidPositionStand.locY + targetDeltaY;
                double targetPositionZ = squidPositionStand.locZ + z;
                if (targetDeltaY >= 0 && !AABBUtil.hasSpace(player.getWorld(), squidAABB())) {

                    Vector newTargetPos = AABBUtil.resolveWrap(player.getWorld(), new Vector(targetPositionX, (targetPositionY), targetPositionZ), squidAABB());
                    if (newTargetPos != null) {

                        squidVelocityY = 0d;
                        targetPositionX = newTargetPos.getX();
                        targetPositionY = newTargetPos.getY();
                        targetPositionZ = newTargetPos.getZ();

                    } else {

                        targetPositionX = squidPositionStand.locX;
                        targetPositionY = squidPositionStand.locY;
                        targetPositionZ = squidPositionStand.locZ;

                    }

                }

                Vector inFrontHead = locationVector().clone().add(new Vector(0, .1, 0));
                Location currentLoc = player.getLocation().clone();
                currentLoc.setPitch(0f);
                Vector direction = currentLoc.getDirection().clone().normalize();
                inFrontHead = inFrontHead.add(direction);

                Block blockFront = player.getWorld().getBlockAt((int)inFrontHead.getBlockX(), (int)inFrontHead.getBlockY(), (int)inFrontHead.getBlockZ());
                if(match.isOwnedByTeam(blockFront, team)) {

                    fallingTicks = 0;
                    double y = player.getLocation().getDirection().getY();
                    if(y >= 0.1) {

                        squidVelocityY = y;
                        lastInkContactTicks = 0;
                        targetPositionY += (y * 0.21);

                    }

                }

                moveSquidPositionStand(
                        (targetPositionX - squidPositionStand.locX),
                        (targetPositionY - squidPositionStand.locY),
                        (targetPositionZ - squidPositionStand.locZ));

                if(!squidPositionStand.onGround) {

                    fallingTicks++;

                } else {

                    fallingTicks = 0;

                }

                /*
                squidCameraPosition.locX = squidPositionStand.locX;
                squidCameraPosition.locY = squidPositionStand.locY - ARMORSTAND_SQUID_Y_OFFSET;
                squidCameraPosition.locZ = squidPositionStand.locZ;
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(squidCameraPosition));
                */

            } else {

                fallingTicks = 0;
                if (isRidingOnInkRail()) {

                    if (!space) {

                        if (moved) {

                            InkRail.Joint joint = currentInkRail.getLineVectors(getRidingVectorIndex());
                            if (joint != null) {

                                // Richtung von A nach B
                                Vector directionAtoB = joint.start.clone().subtract(joint.end).multiply(-1).normalize();
                                Location location = player.getEyeLocation().clone();
                                Vector ownMovement = location.getDirection().clone();
                                ownMovement = ownMovement.multiply(initialXMod);

                                SplatoonServer.broadcastColorParticle(player.getWorld(),
                                            player.getEyeLocation().getX() + ownMovement.getX(),
                                            player.getEyeLocation().getY() + ownMovement.getY(),
                                            player.getEyeLocation().getZ() + ownMovement.getZ(), team.getColor(), 1f);

                                Vector positiveTarget = directionAtoB.clone();
                                Vector negativeTarget = directionAtoB.clone().multiply(-1);

                                double distPositive = positiveTarget.distance(ownMovement);
                                double distNegative = negativeTarget.distance(ownMovement);
                                boolean found = false;
                                boolean negative = false;

                                if (distPositive < 1.125d) {

                                    found = true;

                                } else if (distNegative < 1.125d) {

                                    negative = true;
                                    found = true;

                                }

                                int indx = getRidingVectorIndex();
                                if (found) {

                                    int increment = 3;
                                    if (shift) { increment = 1; } else {

                                        for (int i = 0; i < 2; i++) {
                                                SplatoonServer.broadcastColorizedBreakParticle(player.getWorld(),
                                                        squidPositionStand.locX,
                                                        squidPositionStand.locY,
                                                        squidPositionStand.locZ,
                                                        team.getColor());
                                            }

                                        }

                                        if (!negative) {

                                            indx += increment;
                                            if (indx >= (currentInkRail.getTrack().size() - 1)) {
                                                indx = currentInkRail.getTrack().size() - 1;
                                            }

                                        } else {

                                            indx -= increment;
                                            if (indx < 0) {
                                                indx = 0;
                                            }

                                        }

                                    }

                                updateRidingIndex(indx);
                                Vector v = currentInkRail.vectorFor(indx);
                                if(v != null) {

                                    forceNMSSquidPosition(v.toLocation(getWorld()));
                                    squidPositionStand.locX = v.getX();
                                    squidPositionStand.locY = v.getY();
                                    squidPositionStand.locZ = v.getZ();
                                    squidCameraPosition.locX = v.getX();
                                    squidCameraPosition.locY = v.getY() - ARMORSTAND_SQUID_Y_OFFSET;
                                    squidCameraPosition.locZ = v.getZ();
                                    getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(
                                            squidCameraPosition
                                    ));
                                    getNMSPlayer().setPosition(v.getX(), v.getY(), v.getZ());


                                }

                            }

                        }

                    } else {

                        ejectFromInkRail(true);

                    }

                } else if(isRidingOnRideRail()) {

                    if(space) {

                        ejectFromRideRail();
                        squidVelocityY = 0.825;

                    }

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            swimSoundTicker++;
            if(swimSoundTicker > 10) {

                swimSoundTicker = 0;
                if (playSwimSound) {

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISH_SWIM, swimSoundVolume, .7f);

                }

            }

        }
        ((CraftPlayer)player).getHandle().setPosition(squidCameraPosition.locX, squidCameraPosition.locY, squidCameraPosition.locZ);

    }

    private int swimSoundTicker = 0;

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().toVector().distance(locationVector());
    }

    @Override
    public boolean isTargetable() {
        return !isSplatted();
    }

    @Override
    public LocationProvider getTargetLocationProvider() {
        return new LocationProvider() {
            @Override
            public Location getLocation() {
                return locationVector().toLocation(getPlayer().getWorld());
            }

            @Override
            public Vector getLastDelta() {
                return lastDelta;
            }
        };
    }

    @Override
    public UUID getUUID() {
        return getPlayer().getUniqueId();
    }

    @Override
    public boolean isHuman() {
        return true;
    }

    @Override
    public int getEntityID() {
        return player.getEntityId();
    }

    @Override
    public void push(double x, double y, double z) {

        getPlayer().setVelocity(player.getVelocity().add(new Vector(x,y,z)));

    }

    @Override
    public Location getLocation() {

        Location location = locationVector().toLocation(getPlayer().getWorld());

        location.setYaw(player.getLocation().getYaw());
        location.setPitch(player.getLocation().getPitch());

        return location;
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

    @Override
    public boolean isDead() {
        return isSplatted();
    }


    public float yaw() { return getPlayer().getLocation().getYaw(); }
    public float pitch() { return getPlayer().getLocation().getPitch(); }

    private double ink = 100d;
    public double getInk() { return ink; }
    public void removeInk(double amount) {

        this.ink-=amount;
        if(ink < 0) { ink = 0d; }
        updateLastInkModification();

    }

    public void addInk(double amount) {

        if(isDebuffed()) { return; }

        this.ink+=amount;
        if(ink > 100d) { ink = 100d; }
        updateLastInkModification();

    }

    private long lastNotEnoughInk = 0;
    public void notEnoughInk() {

        if(lastNotEnoughInk+300 < System.currentTimeMillis()) {

            lastNotEnoughInk = System.currentTimeMillis();
            getPlayer().playSound(getPlayer().getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            getPlayer().sendActionBar("§cNicht genügend Tinte!");

        }

    }

    public boolean isSquid() { return squidPositionStand != null; }


    public Vector locationVector() {

        if(isSquid()) {

            if(inSuperJump()) {

                return new Vector(superJumpRidingStand.getLocation().getX(),
                        superJumpRidingStand.getLocation().getY(),
                        superJumpRidingStand.getLocation().getZ());

            } else {

                return new Vector(squidPositionStand.locX, squidPositionStand.locY, squidPositionStand.locZ);

            }

        } else {

            return player.getLocation().toVector();

        }

    }

    public void dragTowards(Hook hook) {

        this.targetHook = hook;

        if(!isSquid()) {

            enterSquidForm();
            squidFormLocked = true;

        }

    }

    @Override
    public void sendMessage(String s) {

        player.sendMessage(s);

    }

    @Override
    public int heldItemSlot() {
        return player.getInventory().getHeldItemSlot();
    }

    public void hitMark(Location location) {

        getPlayer().playSound(location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, 2f);

    }

    private double specialPoints;
    public void resetSpecialGauge() {

        setSpecialPoints(0);

    }
    public double getSpecialPoints() {

        return specialPoints;

    }

    @Override
    public void setSpecialPoints(double val) {

        boolean before = isSpecialReady();
        this.specialPoints = val;
        if(!before && isSpecialReady()) {

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            player.sendMessage(Chat.SYSTEM_PREFIX + "Deine Spezialwaffe ist aufgeladen. Aktiviere sie auf dem §evierten Slot§7.");

        }

    }

    @Override
    public EntityPlayer getNMSPlayer() {
        return ((CraftPlayer)player).getHandle();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return player;
    }

    public void specialNotReady() {

        if(lastNotEnoughInk+300 < System.currentTimeMillis()) {

            lastNotEnoughInk = System.currentTimeMillis();
            getPlayer().playSound(getPlayer().getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            getPlayer().sendActionBar("§cSpezialwaffe nicht aufgeladen!");

        }

    }

    public void resetLastInteraction() { lastInteraction = 0; }

    public boolean canUseMainWeapon() {

        boolean flag = true;
        if(equipment.getSpecialWeapon() != null) {

            flag = !equipment.getSpecialWeapon().isActive();

        }

        return !isBeingDragged() && !isRidingOnInkRail() && flag;

    }

    public boolean canUseSecondaryWeapon() {

        boolean flag = true;
        if(equipment.getSpecialWeapon() != null) {

            flag = !equipment.getSpecialWeapon().isActive();

        }

        return !isBeingDragged() && !isRidingOnInkRail() && flag;

    }

    public boolean hasEnoughInk(float splatUsage) { return ink >= splatUsage; }

    public void lockSquidForm() { squidFormLocked = true; }

    @Override
    public boolean isOnGround() {
        return player.isOnGround();
    }

    public void unlockSquidForm() { squidFormLocked = false; }

    public String getName() {

        // TODO Nicking
        return player.getName();

    }

    private UserData userData = new UserData(this);
    public UserData getUserData() { return userData; }

    public void endSuperJump() {

        postJumpCall();

    }

    public void refillRecentPlayerPool() {

        spawnedRecentPlayers.clear();
        recentPlayersPool.clear();

        for(int i = 0; i < SplatoonLobby.minRecentPlayerCount(); i++) {

            Color color = Color.getRandomColors(1).get(0);
            AISkin.SkinData skin = AISkin.randomSkin(color);
            RecentPlayer player = new RecentPlayer(color, "Player" + i, UUID.randomUUID(), skin.getValue(), skin.getSignature(), 1, 1, 2, 3);
            recentPlayersPool.add(player);

        }

    }

    private ArrayList<RecentPlayer> recentPlayersPool = new ArrayList<>();
    private ArrayList<RecentPlayer> spawnedRecentPlayers = new ArrayList<>();
    public void resetSpawnedRecentPlayers() {

        spawnedRecentPlayers.clear();

    }
    public RecentPlayer getRandomRecentPlayer() {

        if(!recentPlayersPool.isEmpty()) {

            while (true) {

                ArrayList<RecentPlayer> iteratedPlayers = new ArrayList<>();
                RecentPlayer player = recentPlayersPool.get(new Random().nextInt(recentPlayersPool.size() - 1));
                if(!spawnedRecentPlayers.contains(player)) {

                    return player;

                } else {

                    if(!iteratedPlayers.contains(player)) {

                        iteratedPlayers.add(player);

                    }

                    if(iteratedPlayers.size() == spawnedRecentPlayers.size()) {

                        return null;

                    }

                }

            }

        }
        return null;

    }

    public void markAsSpawned(RecentPlayer player) {

        recentPlayersPool.remove(player);
        spawnedRecentPlayers.add(player);

    }

    private XenyriaSpigotPlayer spigotPlayer;
    public XenyriaSpigotPlayer getXenyriaPlayer() { return spigotPlayer; }

    public long millisSinceLastInteraction() { return System.currentTimeMillis() - lastInteraction; }


    public void forceRespawn() {

        respawnCall();

    }

    private EntityPlayer spectatorEntity;
    private SplatoonPlayer spectatedPlayer = null;
    private boolean spectatorCameraReady = false;
    public void leaveSpectatorMode() {

        if(spectatorEntity != null) {

            Location location = spectatedPlayer.getLocation();
            spectatorCameraMode = null;
            spectatedPlayer = null;
            getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(spectatorEntity.getId()));
            spectatorCameraId = -1;
            spectatorEntity = null;
            spectateInProgress = false;
            spectatorCameraReady = false;
            getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutCamera(getNMSEntity()));
            player.teleport(location);

        }

        spectatorCameraReady = false;
        spectatedPlayer = null;
        spectatorEntity = null;

    }

    public void spectatePlayer(SplatoonPlayer player, SpectatorCameraMode mode) {

        this.spectatorCameraMode = mode;
        Match match = getMatch();
        if(match instanceof BattleMatch) {

            BattleMatch match1 = (BattleMatch) match;
            if(match1.getRemainingGameTicks() < 1) {

                return;

            }

        }

        if(!spectateInProgress) {

            spectatedPlayer = player;
            spectateInProgress = true;

            GameProfile profile = new GameProfile(UUID.randomUUID(), "Kamera");

            spectatorEntity = new EntityPlayer(
                    getMatch().nmsWorld().getMinecraftServer(), ((WorldServer)getMatch().nmsWorld()), profile, new PlayerInteractManager(getMatch().nmsWorld())
            );
            spectatorEntity.locX = getLocation().getX();
            spectatorEntity.locY = getLocation().getY();
            spectatorEntity.locZ = getLocation().getZ();
            spectatorEntity.setInvisible(true);
            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, spectatorEntity);
            getNMSPlayer().playerConnection.sendPacket(packet);
            Match prevMatch = getMatch();
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                Match newMatch = getMatch();
                if(isValid()) {

                    if(newMatch == prevMatch && !newMatch.inOutro() && !newMatch.inIntro()) {

                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(
                                spectatorEntity
                        ));
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(spectatorEntity.getId(), spectatorEntity.getDataWatcher(), false));
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutCamera(spectatorEntity));
                        getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Du schaust nun " + player.coloredName() + " §7zu!");
                        spectatorCameraReady = true;
                        spectatorCameraId = spectatorEntity.getId();
                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, spectatorEntity));

                        },4l);

                    } else {

                        spectatedPlayer = null;
                        spectateInProgress = false;
                        getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, spectatorEntity));
                        spectatorCameraReady = false;

                    }

                } else {

                    spectatedPlayer = null;
                    spectateInProgress = false;

                }

            }, 2l);

        }

    }

    public static enum SpectatorCameraMode {

        ROTATE("Drehend", 0),
        FIRST_PERSON("1. Person", 1),
        FIXED("Fixiert", 2);

        private String name;
        private int slot;

        SpectatorCameraMode(String name, int slot) {

            this.name = name;
            this.slot = slot;

        }

    }

    private boolean spectateInfoPacket = false;
    private boolean spectateInProgress = false;

}
