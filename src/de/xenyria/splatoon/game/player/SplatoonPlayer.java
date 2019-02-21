package de.xenyria.splatoon.game.player;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.entity.PushableEntity;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.Equipment;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrush;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractDualies;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.Hook;
import de.xenyria.splatoon.game.objects.InkRail;
import de.xenyria.splatoon.game.objects.RideRail;
import de.xenyria.splatoon.game.player.superjump.SuperJump;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

public abstract class SplatoonPlayer implements HitableEntity, TentaMissleTarget, PushableEntity {

    public static double BASE_INK_CHARGE_VALUE = 1.65d, HUMAN_INK_CHARGE_VALUE = 0.2d;

    public abstract Equipment getEquipment();
    public abstract double getHealth();
    public abstract Location getEyeLocation();
    public World getWorld() { return getLocation().getWorld(); }

    public boolean isHighlighted(SplatoonPlayer player) {

        return remainingHighlightTicks.containsKey(player);

    }

    public double getInkRechargeValue() {

        return BASE_INK_CHARGE_VALUE;

    }

    private double armorHealth = 0d;
    public double getArmorHealth() { return armorHealth; }

    @Override
    public AxisAlignedBB aabb() {

        if(isSquid()) {

            return squidAABB();

        } else {

            // TODO Baller / Jetpack
            if(getEquipment().getSpecialWeapon() != null && getEquipment().getSpecialWeapon().isActive()) {

                if(getEquipment().getSpecialWeapon() instanceof Jetpack) {

                    return ((Jetpack)getEquipment().getSpecialWeapon()).jetpackAABB();

                }

            }

            return new AxisAlignedBB(
                    getLocation().getX() - .3, getLocation().getY(), getLocation().getZ() - .3,
                    getLocation().getX() + .3, getLocation().getY() + 1.8, getLocation().getZ() + .3
            );

        }

    }

    public AxisAlignedBB squidAABB() {

        Vector vector = getLocation().toVector();
        return new AxisAlignedBB(vector.getX() - .45, vector.getY(), vector.getZ() - .45, vector.getX() + .45, vector.getY() + 1.25, vector.getZ() + .45);

    }

    public String coloredName() {

        return getTeam().getColor().prefix() + getName();

    }

    public abstract void notEnoughInk();
    public abstract boolean canUseMainWeapon();
    public abstract boolean canUseSecondaryWeapon();

    public abstract boolean isSquid();
    public abstract Location getLocation();

    public Location getShootingLocation(boolean left) {

        return getShootingLocation(getEyeLocation().toVector(), left);

    }

    public Location getShootingLocation(Vector base, boolean left) {

        if(getEquipment().getPrimaryWeapon() != null) {

            if(getEquipment().getPrimaryWeapon().getPrimaryWeaponType() == PrimaryWeaponType.ROLLER) {

                return getEyeLocation();

            }

        }

        Vector dir = getLocation().getDirection();
        Vector start = base.clone();
        Location location = getEyeLocation().clone();
        location.setYaw(location.getYaw() + 90f);
        Vector sideways = location.getDirection().clone();
        location.setPitch(location.getPitch() - 90f);
        Vector upwards = location.getDirection().clone();

        double leftValue = .25;
        if(!left) { leftValue*=-1; }

        Vector target = start.clone().add(dir.clone().multiply(.35)).add(sideways.clone().multiply(leftValue)).add(upwards.clone().multiply(-.05));
        Location location1 = target.toLocation(getLocation().getWorld());
        location1.setPitch(getLocation().getPitch());
        location1.setYaw(getLocation().getYaw());
        return location1;

    }

    public Color getColor() { return (getTeam() == null) ? null : getTeam().getColor(); }

    public void beginRidingInkRail(InkRail rail) {

        updateRidingIndex(0);
        if(rail.getOwningTeam() != null && rail.getOwningTeam() == getTeam()) {

            rail.moveToNearestPosition(this);
            if(this instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer player = (SplatoonHumanPlayer) this;
                player.getPlayer().setAllowFlight(true);
                player.sendActionBar("§e§lFormwechsel/Leertaste §7zum verlassen der Schiene.");

            }

            currentInkRail = rail;
            SplatoonServer.broadcastColorParticleExplosion(getWorld(), getLocation().getX(), getLocation().getY(), getLocation().getZ(), getColor());
            SplatoonServer.broadcastColorParticleExplosion(getWorld(), getLocation().getX(), getLocation().getY(), getLocation().getZ(), getColor());
            SplatoonServer.broadcastColorParticleExplosion(getWorld(), getLocation().getX(), getLocation().getY(), getLocation().getZ(), getColor());
            getWorld().playSound(getLocation(), Sound.ENTITY_FISH_SWIM, 1f, 1f);

        }

    }

    public InkRail currentInkRail;
    public RideRail currentRideRail;
    public SuperJump currentJump;

    public void beginRidingRideRail(RideRail rail) {

        if(rail.getOwningTeam() != null && rail.getOwningTeam() == getTeam()) {

            getLocation().getWorld().playSound(getLocation(), Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 0.7f);
            rail.moveToNearestPosition(this);

            if(!isSquid()) {

                teleport(rail.nearestLocation(getLocation()));

            } else {

                Vector pos = rail.nearestLocation(getLocation()).toVector();
                forceSquidMovement(pos.getX(), pos.getY(), pos.getZ(), true);

            }

            sendActionBar("§e§lSHIFT §7zum verlassen der Schiene.");
            this.currentRideRail = rail;

            if(this instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer player = (SplatoonHumanPlayer) this;
                player.getPlayer().setGameMode(GameMode.ADVENTURE);
                player.getPlayer().setAllowFlight(true);
                player.getPlayer().setFlying(true);
                player.getPlayer().setFlySpeed(0f);
                player.getPlayer().setWalkSpeed(0f);

            }

        }

    }

    public abstract boolean isShooting();
    public abstract boolean isSplatted();

    public boolean isRidingOnInkRail() { return currentInkRail != null; }
    public boolean isRidingOnRideRail() { return currentRideRail != null; }

    public abstract boolean canRideRideRail();
    public abstract boolean canRideInkRail();
    public abstract boolean hasControl();
    public abstract boolean inSuperJump();
    public abstract void tick();

    public void tickHighlights() {

        Iterator<Map.Entry<TentaMissleTarget, Integer>> iterator = remainingHighlightTicks.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<TentaMissleTarget, Integer> entry = iterator.next();
            int newVal = entry.getValue()-1;
            if(newVal < 1) {

                iterator.remove();

            } else {

                entry.setValue(newVal);

            }

        }

    }

    public void tickInkArmor() {

        if(armorHealth > 0d) {

            armorHealth -= 0.7;

            if (armorHealth < 0d) {

                armorHealth = 0d;
                armorApplied = false;

                if(this instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer)this).getPlayer().playSound(getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.5f);

                }

            }

        }

        if(!armorApplied) {

            updateEquipment();
            armorApplied = true;

        }

    }

    public void setArmorHealth(double health, boolean b) {

        this.armorHealth = health;
        if(b) {

            armorApplied = false;

        }

    }

    private boolean armorApplied = true;

    public abstract void updateEquipment();

    public void updateLastSafe() {

        if(isOnGround() && hasControl()) {

            lastSafe = getLocation().clone();

        }

    }

    public abstract void enableWalkSpeedOverride();
    public abstract void disableWalkSpeedOverride();
    public abstract boolean doOverrideWalkspeed();
    public abstract void setOverrideWalkSpeed(float walkSpeed);
    public abstract Vector getLastDelta();
    public abstract double getSquidVelocityY();

    public boolean specialActive() {

        if(getEquipment().getSpecialWeapon() != null) {

            return getEquipment().getSpecialWeapon().isActive();

        }
        return false;

    }

    private int ridingVectorIndex;
    public int getRidingVectorIndex() { return ridingVectorIndex; }
    public void updateRidingIndex(int indx) {

        ridingVectorIndex = indx;

    }

    public abstract void forceNMSSquidPosition(Location location);

    public abstract void joinMatch(Match match);
    public abstract void leaveMatch();
    public abstract long millisSinceLastDamage();

    public void processRegeneration() {

        if(millisSinceLastDamage() > 1200) {

            if(getHealth() < 100d) {

                double health = getHealth();
                health+=0.25d;
                if(health >= 100d) {

                    health = 100d;

                }
                setHealth(health);

                double ratio = (health / 100d);
                double hp = 0.1 + (19.9 * ratio);

                if(this instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer)this).getPlayer().setHealth(hp);

                }

            }

        }

    }

    public abstract boolean superJump(Location location, int ticks);
    public abstract boolean superJump(Location location, Runnable runnable);
    public abstract boolean superJump(Location location, int ticks, Runnable runnable);

    public abstract String getName();
    public abstract UUID getUUID();
    public abstract boolean isHuman();

    public float yaw() { return getLocation().getYaw(); }
    public float pitch() { return getLocation().getPitch(); }
    public Vector direction() { return getLocation().getDirection().clone(); }

    public abstract boolean isBeingDragged();

    public abstract void setSpawnPoint(Location location);
    public abstract Location getSpawnPoint();

    public abstract BattleStatistic getStatistic();
    public abstract Match getMatch();
    public abstract Team getTeam();

    public abstract Vector getVelocity();
    public abstract void setVelocity(Vector vector);

    public boolean hasEnoughInk(float dodgeUsage) {

        return getInk() >= dodgeUsage;

    }

    public abstract void addSquidVelocity(double v);
    public abstract void sendActionBar(String s);
    public abstract void teleport(Location location);
    public abstract void forceSquidMovement(double x, double y, double z, boolean b);
    public abstract void dragTowards(Hook hook);
    public abstract void sendMessage(String s);
    public abstract int heldItemSlot();

    public abstract void specialNotReady();
    public abstract void resetSpecialGauge();
    public abstract void resetLastInteraction();
    public abstract double getSpecialPoints();
    public abstract void setSpecialPoints(double val);

    private int debuffTicks = 0;
    public boolean isDebuffed() { return debuffTicks > 0; }
    public void setDebuffTicks(int debuffTicks) { this.debuffTicks = debuffTicks; }
    public void tickDebuff() {

        if(debuffTicks > 0) {

            debuffTicks--;
            if(getInk() >= 35d) {

                removeInk(0.21d);

            }

            double offsetX = new Random().nextDouble() * .8;
            double offsetY = new Random().nextDouble() * 2;
            double offsetZ = new Random().nextDouble() * .8;

            Vector pos = new Vector(getLocation().getX()-.4+offsetX,getLocation().getY()+offsetY, getLocation().getZ()-.4+offsetZ);
            World world = getLocation().getWorld();
            world.spawnParticle(Particle.SPELL_MOB_AMBIENT, pos.getX(), pos.getY(), pos.getZ(), 0);

        }

    }
    public static final double DEBUFF_SPEED_MOD = 0.35d;

    public abstract EntityPlayer getNMSPlayer();
    public abstract Entity getBukkitEntity();

    public abstract void setTeam(Team team);
    public abstract void hitMark(Location location);

    public abstract void unlockSquidForm();

    public abstract void leaveSquidForm();

    public abstract void lockSquidForm();

    public abstract boolean isOnGround();

    public void splat(Color color, @Nullable SplatoonPlayer splatter, @Nullable SplatoonProjectile projectile, int splatTicks) {

        int points = (int) getSpecialPoints();
        if(points != 0) {

            points/=2d;
            setSpecialPoints(points);

        }

        getStatistic().setDeaths(getStatistic().getDeaths()+1);
        onSplat(color, splatter, projectile, splatTicks);

    }
    public abstract void onSplat(Color color, @Nullable SplatoonPlayer splatter, @Nullable SplatoonProjectile projectile, int splatTicks);
    public void splat(Color color, @Nullable SplatoonPlayer splatter, @Nullable SplatoonProjectile projectile) {

        splat(color, splatter, projectile, 100);

    }

    public void burst(@Nullable SplatoonPlayer killer, Color color) {

        if(armorHealth != 0d) {

            armorHealth = 0d;
            armorApplied = true;
            updateEquipment();

        }

        ArrayList<ItemStack> toDrop = new ArrayList<>();
        if(getEquipment().getPrimaryWeapon() != null) { toDrop.add(getEquipment().getPrimaryWeapon().asItemStack()); }
        if(getEquipment().getSecondaryWeapon() != null) { toDrop.add(getEquipment().getSecondaryWeapon().asItemStack()); }
        if(getEquipment().getSpecialWeapon() != null) { toDrop.add(getEquipment().getSpecialWeapon().asItemStack()); }
        if(getEquipment().getHeadGear() != null) { toDrop.add(getEquipment().getHeadGear().asItemStack(getColor())); }
        if(getEquipment().getBodyGear() != null) { toDrop.add(getEquipment().getBodyGear().asItemStack(getColor())); }
        if(getEquipment().getFootGear() != null) { toDrop.add(getEquipment().getFootGear().asItemStack(getColor())); }
        if(!toDrop.isEmpty()) {

            for(ItemStack stack : toDrop) {

                DroppedEquipmentProjectile equipmentProjectile = new DroppedEquipmentProjectile(
                        this, getEquipment().getPrimaryWeapon(), getMatch(), stack, getLocation()
                        );
                getMatch().queueProjectile(equipmentProjectile);

            }

        }

        float radius = 3f;

        ArrayList<Vector> positions = new ArrayList<>();
        ArrayList<Vector> directions = new ArrayList<>();
        for(float yaw = 0f; yaw < 360f; yaw+=32.5f) {

            for(float pitch = -90f; pitch < 90f; pitch+=30f) {

                Location location = new Location(getLocation().getWorld(), 0,0,0, yaw, pitch);
                Vector origDir = location.getDirection().clone();
                Vector origPos = getLocation().toVector();
                origPos = origPos.add(origDir.clone().multiply(radius / 1.33f));

                positions.add(origPos);
                directions.add(location.getDirection().clone().multiply(radius));

            }

        }

        for(int i = 0; i < directions.size(); i++) {

            Vector direction = directions.get(i);
            Vector position = positions.get(i);

            double offsetRatio = 0.08;
            double offsetX = new Random().nextDouble() * offsetRatio;
            double offsetY = new Random().nextDouble() * offsetRatio;
            double offsetZ = new Random().nextDouble() * offsetRatio;
            if(new Random().nextBoolean()) { offsetX *= -1; }
            if(new Random().nextBoolean()) { offsetY *= -1; }
            if(new Random().nextBoolean()) { offsetZ *= -1; }

            SplatoonServer.broadcastColorizedBreakParticle(getWorld(),
                    position.getX() + offsetX,
                    position.getY() + offsetY,
                    position.getZ() + offsetZ,
                    direction.getX(),
                    direction.getY(),
                    direction.getZ(),
                    color);

        }

        Vector min = new Vector(getLocation().getX() - radius, getLocation().getY() - radius, getLocation().getZ() - radius);
        Vector max = new Vector(getLocation().getX() + radius, getLocation().getY() + radius, getLocation().getZ() + radius);
        double minX = Math.min(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxX = Math.max(min.getX(), max.getX());
        double maxY = Math.max(min.getY(), max.getY());
        double maxZ = Math.max(min.getZ(), max.getZ());

        for(int x = (int)minX; x <= maxX; x++) {

            for(int y = (int)minY; y <= maxY; y++) {

                for(int z = (int)minZ; z <= maxZ; z++) {

                    Vector vec = new Vector(x+.5, y+.5,z+.5);
                    float dist = (float) vec.distance(getLocation().toVector());
                    int distInt = (int) dist;
                    if(dist < radius) {

                        final int fX = x;
                        final int fY = y;
                        final int fZ = z;

                        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                            if(killer != null) {

                                if (getMatch().isPaintable(killer.getTeam(), fX, fY, fZ)) {

                                    getMatch().paint(killer, new Vector(fX, fY, fZ), killer.getTeam());

                                }

                            }

                        }, distInt * 2);


                    }

                }

            }

        }

    }

    public abstract void setHealth(double health);
    public abstract void setLastDamageTicks(int amount);
    public abstract ArrayList<SplatoonHumanPlayer.DamageHistory> getLastDamageHistory();
    public abstract void setLastDamage(long l);

    public SplatoonHumanPlayer.DamageHistory getDamageHistory(SplatoonPlayer player) {

        for(SplatoonHumanPlayer.DamageHistory history : getLastDamageHistory()) {

            if(history.player == player) { return history; }

        }

        return null;

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if (!isSplatted()) {

            if (this instanceof EntityNPC) {

                EntityNPC npc = (EntityNPC) this;
                npc.handleProjectileHit(projectile);

            }

        }

        if(projectile instanceof KnockbackProjectile) {

            KnockbackProjectile knockbackProjectile = (KnockbackProjectile) projectile;
            setVelocity(getVelocity().clone().add(knockbackProjectile.getKnockback()));

        }

        if(projectile instanceof DamageDealingProjectile) {

            DamageDealingProjectile projectile1 = (DamageDealingProjectile)projectile;
            if(projectile1.dealsDamage()) {

                SplatoonPlayer shooter = projectile.getShooter();

                double health = getHealth();
                boolean hasArmor = getArmorHealth() > 0d;
                double dmg = ((DamageDealingProjectile) projectile).getDamage();
                if(hasArmor) {

                    double newArmorHealth = getArmorHealth()-dmg;
                    if(newArmorHealth <= 0d) {

                        setArmorHealth(0d, true);
                        if(this instanceof SplatoonHumanPlayer) {

                            ((SplatoonHumanPlayer)this).getPlayer().playSound(getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.5f);

                        }

                    } else {

                        setArmorHealth(newArmorHealth, false);

                    }

                }

                if(!hasArmor && (health - dmg) <= 0) {

                    health = 0d;
                    setHealth(health);
                    setLastDamageTicks(3);

                    if(this instanceof SplatoonHumanPlayer) {

                        SplatoonHumanPlayer player = ((SplatoonHumanPlayer)this);
                        player.getPlayer().setHealth(20d);

                    }
                    getMatch().getMatchController().handleSplat(this, shooter, projectile);
                    splat(projectile.getColor(), projectile.getShooter(), projectile);

                    if(shooter != null) {

                        shooter.getStatistic().setSplats(shooter.getStatistic().getSplats() + 1);

                    }
                    for(SplatoonHumanPlayer.DamageHistory history : getLastDamageHistory()) {

                        boolean addAssist = true;
                        if(shooter != null && history.player == shooter) { addAssist = false; }
                        if(addAssist) {

                            history.player.getStatistic().setAssists(history.player.getStatistic().getAssists() + 1);

                        }

                    }
                    getLastDamageHistory().clear();

                } else {

                    if(!hasArmor) {

                        health -= dmg;
                        double ratio = (health / 100d);
                        double hp = 0.1 + (19.9 * ratio);
                        if (this instanceof SplatoonHumanPlayer) {

                            SplatoonHumanPlayer player = ((SplatoonHumanPlayer) this);
                            player.getPlayer().setHealth(hp);

                        }
                        getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
                        setLastDamage(System.currentTimeMillis());

                        if (projectile.getShooter() != null) {

                            SplatoonHumanPlayer.DamageHistory history = getDamageHistory(projectile.getShooter());
                            if (history == null) {

                                history = new SplatoonHumanPlayer.DamageHistory(shooter, dmg);
                                getLastDamageHistory().add(history);

                            }
                            history.recalculate(dmg);

                        }
                        setHealth(health);

                    }

                }

            }

        }

    }

    public abstract boolean isValid();

    public double getHeight() {

        return aabb().maxY-aabb().minY;

    }

    public abstract boolean isSubmergedInInk();
    public abstract boolean isVisibleByTrail();

    public GameProfile getGameProfile() {

        if(this instanceof SplatoonHumanPlayer) {

            SplatoonHumanPlayer player = (SplatoonHumanPlayer)this;
            GameProfile profile = new GameProfile(UUID.randomUUID(), getName());
            GameProfile orig = ((SplatoonHumanPlayer)this).getNMSPlayer().getProfile();
            if(!player.getXenyriaPlayer().hasNickname()) {

                profile.getProperties().putAll(orig.getProperties());

            }

            profile.getProperties().put("textures", new Property("textures", player.getXenyriaPlayer().getNicknameTexture(), player.getXenyriaPlayer().getNicknameSignature()));

            return profile;

        } else {

            EntityNPC npc = (EntityNPC)this;
            GameProfile profile = npc.getProfile().toGameProfile();
            return profile;

        }

    }

    private Location lastSafe;
    public Location getLastSafePosition() {

        return lastSafe;

    }

    public boolean isSpecialActive() {

        SplatoonSpecialWeapon weapon = getEquipment().getSpecialWeapon();
        if(weapon != null) {

            return weapon.isActive();

        }
        return false;

    }

    public boolean isSpecialReady() {

        SplatoonSpecialWeapon weapon = getEquipment().getSpecialWeapon();
        if(weapon != null) {

            if(getSpecialPoints() >= getRequiredSpecialPoints()) {

                return true;

            }

        }
        return false;

    }

    private double points;
    public double getPoints() { return points; }
    public void incrementPoints(double val) {

        points+=val;
        updateSpecialPoints((getSpecialPoints() + val));

    }

    private int requiredSpecialPoints = 0;
    public int getRequiredSpecialPoints() { return requiredSpecialPoints; }
    public void setRequiredSpecialPoints(int requiredSpecialPoints) { this.requiredSpecialPoints = requiredSpecialPoints; }

    private void updateSpecialPoints(double points) {

        setSpecialPoints(points);

    }

    public void handleLeftClick() {

        SplatoonPrimaryWeapon weapon = getEquipment().getPrimaryWeapon();
        if(weapon != null && weapon.isSelected()) {

            if (weapon instanceof AbstractDualies) {

                ((AbstractDualies) weapon).dodge();

            } else if (weapon instanceof AbstractBrush) {

                ((AbstractBrush) weapon).swing();

            }

        }

    }

    private HashMap<TentaMissleTarget, Integer> remainingHighlightTicks = new HashMap<>();

    public void highlight(TentaMissleTarget target, int i) {

        remainingHighlightTicks.put(target, i);
        handleHighlight(target, i);

    }
    public abstract void handleHighlight(TentaMissleTarget target, int i);

    public abstract int getVisibleEntityID();

    private int specialUseCounter;
    public int getSpecialUseCounter() { return specialUseCounter; }
    public void resetSpecialUseCounter() { specialUseCounter = 0; }
    public void incrementSpecialUseCounteR() { specialUseCounter++; }

    public static class DamageHistory {

        public double dealtDamage;
        public SplatoonPlayer player;
        public int remainingTicks;

        public DamageHistory(SplatoonPlayer player, double dealtDamage) {

            this.player = player;
            this.dealtDamage = dealtDamage;

        }

        public void recalculate(double dmg) {

            this.dealtDamage = dmg;
            double realDamage = dealtDamage;
            if(realDamage > 100d) { realDamage = 100d; }
            remainingTicks = (int) ((realDamage / 100d) * 120);

        }

    }

    public abstract boolean isSpectator();

    public abstract int lastDamageTicker();

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(isSpectator()) { return false; }
        SplatoonSpecialWeapon weapon = getEquipment().getSpecialWeapon();
        if(getMatch() instanceof BattleMatch) {

            BattleMatch match = (BattleMatch) getMatch();
            Vector center = match.centeredTeamSpawnVector(getTeam());
            if(center.distance(getLocation().toVector()) <= 5d) {

                return false;

            }

        }

        if(lastDamageTicker() != 0) { return false; }
        if(weapon != null && weapon.isActive()) {

            if(weapon.isSelected() && weapon instanceof Baller) { return false; }

        }

        if(projectile instanceof InstantDamageKnockbackProjectile) { return true; }
        if(isSplatted()) { return false; }

        if(projectile.getShooter() != null && projectile.getShooter().getTeam() != getTeam() && projectile.getShooter() != this) {

            if(projectile instanceof RayProjectile) {

                return true;

            }

            AxisAlignedBB bb = projectile.aabb();
            if(bb != null) {

                return projectile.aabb().c(aabb());

            }
            return false;

        } else {

            return false;

        }

    }

    public static class BattleStatistic {

        private int inkedTurf;
        public int getInkedTurf() { return inkedTurf; }
        public void setInkedTurf(int inkedTurf) { this.inkedTurf = inkedTurf; }

        private int splats;
        public int getSplats() { return splats; }
        public void setSplats(int splats) { this.splats = splats; }

        private int assists;
        public int getAssists() { return assists; }
        public void setAssists(int assists) { this.assists = assists; }

        private int deaths;
        public int getDeaths() { return deaths; }
        public void setDeaths(int deaths) { this.deaths = deaths; }

    }

    public void resetLastInkModification() { lastInkModification = 0; }
    private long lastInkModification;
    public long getLastInkModification() { return lastInkModification; }
    public void updateLastInkModification() { lastInkModification = System.currentTimeMillis(); }
    public int elapsedSecondsSinceLastInkModification() {

        return (int) ((System.currentTimeMillis() - (lastInkModification)) / 1000);

    }

    public abstract double getInk();
    public abstract void addInk(double amount);
    public abstract void removeInk(double amount);

}
