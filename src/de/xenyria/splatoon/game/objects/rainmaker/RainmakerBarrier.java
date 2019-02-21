package de.xenyria.splatoon.game.objects.rainmaker;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.RainMaker;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissleTarget;
import de.xenyria.splatoon.game.equipment.weapon.util.PlayerDamageCooldownMap;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.DataWatcher;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityDestroy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class RainmakerBarrier extends GameObject implements TentaMissleTarget {

    private Vector position;

    private ArrayList<ArmorStand> armorStands = new ArrayList<>();
    private ArrayList<HitableEntity> hitboxes = new ArrayList<>();

    private double generatedRadius = 0;
    private double damage = 0d;
    private Team lastDamageTeam = null;
    private SplatoonPlayer lastShooter = null;

    public void destroyBarrier() {

        for(HitableEntity hitbox : hitboxes) {

            getMatch().queueObjectRemoval((GameObject) hitbox);

        }

        if(!armorStands.isEmpty()) {

            int[] ids = new int[armorStands.size()];
            int i = 0;
            for (ArmorStand stand : armorStands) {

                ids[i] = stand.getEntityId();
                stand.remove();
                i++;

            }
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (player.getWorld().equals(getMatch().getWorld()) && player.getLocation().toVector().distance(position) < 96) {

                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(ids));

                }

            }
            armorStands.clear();

        }

    }

    public void generateSphere(double radius) {

        destroyBarrier();

        ArrayList<Vector> vectors = new ArrayList<>();
        double minX = Math.min(position.getX() - radius, position.getX() + radius);
        double minY = Math.min(position.getY() - radius, position.getY() + radius);
        double minZ = Math.min(position.getZ() - radius, position.getZ() + radius);
        double maxX = Math.max(position.getX() - radius, position.getX() + radius);
        double maxY = Math.max(position.getY() - radius, position.getY() + radius);
        double maxZ = Math.max(position.getZ() - radius, position.getZ() + radius);
        for(int x = (int) minX; x <= maxX; x++) {

            for(int y = (int) minY; y <= maxY; y++) {

                for(int z = (int) minZ; z <= maxZ; z++) {

                    Vector vector = new Vector(position.getX() - x, position.getY() - y, position.getZ() - z);
                    double distance = vector.distance(new Vector(0,0,0));
                    double minDistance = radius - .51;
                    double maxDistance = radius + .51;
                    if(distance >= minDistance && distance <= maxDistance) {

                        vectors.add(vector);

                    }

                }

            }

        }

        for(Vector vector : vectors) {

            Location location1 = position.clone().add(vector.clone().multiply(.625)).toLocation(getMatch().getWorld());
            ArmorStand stand = (ArmorStand) location1.getWorld().spawnEntity(location1, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999, 2, false, false, false));
            stand.setGravity(false);
            stand.setCanTick(false);
            if(lastDamageTeam == null) {

                stand.setHelmet(new ItemStack(Material.GLASS));

            } else {

                double percentage = (damage / maxDamage) * 100;
                if(RandomUtil.random((int) percentage)) {

                    stand.setHelmet(new ItemStack(lastDamageTeam.getColor().getGlass()));

                } else {

                    stand.setHelmet(new ItemStack(Material.GLASS));

                }

            }

            armorStands.add(stand);
            RainmakerBarrierHitbox hitbox = new RainmakerBarrierHitbox(getMatch(), this, stand);
            hitboxes.add(hitbox);
            getMatch().addGameObject(hitbox);

        }

        this.generatedRadius = radius;

    }

    public RainmakerBarrier(Match match, Location location) {

        super(match);

        location = location.clone();
        location.setPitch(0f);
        location.setYaw(0f);

        stand = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(.5, .5, .5), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCanTick(false);
        stand.setCanMove(false);
        stand.setHelmet(new ItemStack(Material.GOLDEN_HORSE_ARMOR));

        position = location.clone().getBlock().getLocation().toVector().clone().add(new Vector(.5, .5, .5));
        generateSphere(minSize);
        targetRadius = minSize;

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private int growTicker = 0;
    private double targetRadius = 3d;
    private boolean incr = true;

    private RainMaker dummyRainMaker = new RainMaker();
    private int exposedTicks = 0;

    @Override
    public void onTick() {

        if(queuedExplosion) {

            queuedExplosion = false;
            exploded = true;
            BombProjectile projectile = new BombProjectile(lastShooter, dummyRainMaker, getMatch(), 9f, 0, 300, false);
            projectile.spawn(0, position.toLocation(getMatch().getWorld()));
            destroyBarrier();

        } else {

            if(exploded) {

                exposedTicks++;
                if(exposedTicks > 300) {

                    exposedTicks = 0;
                    queuedExplosion = false;
                    exploded = false;
                    targetRadius = minSize;
                    damage = 0d;
                    lastShooter = null;
                    lastDamageTeam = null;
                    generateSphere(minSize);

                }

            } else {

                damageTicker++;
                if(damageTicker > 10) {

                    damageTicker = 0;

                    for(HitableEntity object : getMatch().getHitableEntities()) {

                        double dist = object.getLocation().distance(position.toLocation(getMatch().getWorld()));
                        if(dist <= generatedRadius) {

                            Vector target = object.getLocation().toVector();
                            Vector begin = position.clone();
                            Vector direction = target.clone().subtract(begin).clone().multiply(.2);

                            if(VectorUtil.isValid(direction)) {

                                InstantDamageKnockbackProjectile projectile = new InstantDamageKnockbackProjectile(
                                        target.toLocation(getMatch().getWorld()),
                                        null, dummyRainMaker, direction, 12d, getMatch()
                                );
                                projectile.setTeam(lastDamageTeam);
                                if (object.isHit(projectile)) {

                                    object.onProjectileHit(projectile);

                                }

                            }

                        }

                    }

                }

                if(generatedRadius != targetRadius) {

                    generateSphere(targetRadius);

                }

            }

        }

    }

    @Override
    public void reset() {

        damage = 0d;
        targetRadius = minSize;
        this.generateSphere(targetRadius);

    }

    private boolean exploded = false;
    public boolean hasExploded() { return exploded; }

    private PlayerDamageCooldownMap map = new PlayerDamageCooldownMap();

    public void handleProjectile(SplatoonProjectile projectile) {

        if(map.lastDamage(projectile.getShooter()) > 50) {

            if (projectile instanceof DamageDealingProjectile && projectile.getShooter() != null) {

                DamageDealingProjectile projectile1 = (DamageDealingProjectile) projectile;
                if (projectile1.dealsDamage()) {

                    projectile.remove();
                    lastShooter = projectile.getShooter();
                    map.registerDamage(lastShooter);

                    if (lastDamageTeam != null) {

                        boolean isFromLeadingTeam = lastDamageTeam == projectile.getShooter().getTeam();
                        if (isFromLeadingTeam) {

                            damage += ((DamageDealingProjectile) projectile).getDamage();

                        } else {

                            damage -= ((DamageDealingProjectile) projectile).getDamage();
                            if (damage < 0) {

                                lastDamageTeam = projectile.getShooter().getTeam();
                                damage += ((DamageDealingProjectile) projectile).getDamage();

                            }

                        }

                    } else {

                        lastDamageTeam = projectile.getShooter().getTeam();
                        damage += ((DamageDealingProjectile) projectile).getDamage();

                    }
                    handleDamage();

                }

            }

        }

    }

    private boolean queuedExplosion = false;
    private double maxSize = 3.5d;
    private double maxDamage = 700d;
    private double minSize = 1.5d;
    private int damageTicker = 0;

    public void handleDamage() {

        if(damage >= maxDamage) {

            queuedExplosion = true;

        } else {

            double size = minSize + (damage / maxDamage) * maxSize;
            size*=2;
            size = Math.floor(size);
            size/=2;
            if(size != generatedRadius) { targetRadius = size; }

        }

    }

    @Override
    public boolean isTargetable() {
        return !exploded;
    }

    @Override
    public LocationProvider getTargetLocationProvider() {
        return new LocationProvider() {
            @Override
            public Location getLocation() {
                return position.toLocation(getMatch().getWorld());
            }

            @Override
            public Vector getLastDelta() {
                return new Vector();
            }
        };
    }

    @Override
    public UUID getUUID() {
        return stand.getUniqueId();
    }

    private ArmorStand stand;

    @Override
    public int getEntityID() {
        return stand.getEntityId();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Team getTeam() {
        return null;
    }

    @Override
    public DataWatcher getDataWatcher() {
        return ((CraftArmorStand)stand).getHandle().getDataWatcher();
    }

    @Override
    public Entity getNMSEntity() {
        return ((CraftArmorStand)stand).getHandle();
    }

    public ArrayList<HitableEntity> getHitboxes() { return hitboxes; }
}
