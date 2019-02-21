package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.BlockUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Predicate;

public class TentaMissleRocket extends SplatoonProjectile implements DamageDealingProjectile {

    public TentaMissleRocket(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {
        super(shooter, weapon, match);
    }

    private Firework firework;
    private Item payload;

    @Override
    public Location getLocation() {

        if(firework != null) {

            return firework.getLocation();

        } else {

            return payload.getLocation();

        }

    }

    @Override
    public void onRemove() {

        if(payload != null && !payload.isDead()) {

            payload.remove();

        }
        if(firework != null && !firework.isDead()) {

            firework.remove();

        }

    }


    private double lastX, lastY, lastZ;
    private boolean forceDetonation = false;
    private ArrayList<BombProjectile> projectiles = new ArrayList<>();

    @Override
    public void tick() {


        if(payload != null && !payload.isDead()) {

            ((CraftItem)payload).getHandle().getBoundingBox().setFilter(NMSUtil.filter);
            ((CraftItem)payload).getHandle().move(EnumMoveType.SELF, 0, -.34, 0);
            payload.setVelocity(new Vector());
            for(Player player : Bukkit.getOnlinePlayers()) {

                if(player.getLocation().distance(payload.getLocation()) <= 96D) {

                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(((CraftItem)payload).getHandle()));
                    ((CraftItem)payload).getHandle().motY = -.34;
                    ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityVelocity(((CraftItem)payload).getHandle()));
                    ((CraftItem)payload).getHandle().motY = 0;

                }

            }

        }

        if(ticksToChange > 0 && !forceDetonation) {

            if(firework != null) {

                Location location = firework.getLocation();
                SplatoonServer.broadcastColorParticle(location.getWorld(), location.getX(), location.getY(), location.getZ(), getTeam().getColor(), 0.2f);

            }

            ticksToChange--;
            if(ticksToChange < 1) {

                if(firework != null) { firework.remove();
                    NMSUtil.broadcastEntityRemovalToSquids(firework); firework = null; }
                Location target = locationProvider.getLocation().clone().add(locationProvider.getLastDelta()).add(0, 5, 0);


                Vector offset = new Vector();
                int tries = 0;
                while (tries < 10) {

                    double mod1 = 1d;
                    double mod2 = 1d;
                    if (new Random().nextBoolean()) { mod1 = -1d; }
                    if (new Random().nextBoolean()) { mod2 = -1d; }
                    offset.add(new Vector(.45*mod1, 0, .45*mod2));

                    Vector position = target.toVector().add(offset);
                    Location location = position.toLocation(getMatch().getWorld());
                    Block block = location.getBlock();
                    Block block1 = BlockUtil.ground(location, 8);
                    if(block1.isEmpty() || Math.abs(block1.getY()-locationProvider.getLocation().getY()) <= 1.5) {

                        target = location;
                        break;

                    }

                    tries++;

                }

                //target = target.add(new Random().nextDouble() * mod1, 8, new Random().nextDouble() * mod2);
                payload = (Item) target.getWorld().spawnEntity(target, EntityType.DROPPED_ITEM);
                payload.setVelocity(new Vector(0, 0, 0));
                payload.setGravity(false);
                payload.setPickupDelay(99999);
                payload.setItemStack(new ItemStack(Material.FIREWORK_ROCKET));
                payload.setCanMobPickup(false);
                ((CraftItem)payload).getHandle().getBoundingBox().setFilter(NMSUtil.filter);

            }

        } else {

            double deltaToLastX = Math.abs(payload.getLocation().getX() - lastX);
            double deltaToLastY = Math.abs(payload.getLocation().getY() - lastY);
            double deltaToLastZ = Math.abs(payload.getLocation().getZ() - lastZ);

            if(payload.isOnGround() || forceDetonation) {

                BombProjectile projectile = new BombProjectile(getShooter(), getWeapon(), getMatch(), 3.25f, 0, 45, false);
                projectile.spawn(0, payload.getLocation());
                projectiles.add(projectile);
                remove();
                payload.remove();
                NMSUtil.broadcastEntityRemovalToSquids(payload);

            }

            lastX = payload.getLocation().getX();
            lastY = payload.getLocation().getY();
            lastZ = payload.getLocation().getZ();

        }

        if(payload != null && !payload.isDead() && !forceDetonation) {

            Vector cur = payload.getLocation().toVector();
            Vector tar = cur.clone().add(payload.getVelocity());
            Vector dir = tar.clone().subtract(cur).normalize();
            RayProjectile projectile = new RayProjectile(
                    getShooter(), getWeapon(), getMatch(), cur.toLocation(getMatch().getWorld()),
                    dir, 40f
            );
            HitableEntity nearestEntity = projectile.getHitEntity(payload.getVelocity().length() + .2, new Predicate<HitableEntity>() {
                @Override
                public boolean test(HitableEntity hitableEntity) {
                    return hitableEntity != getShooter() && !(hitableEntity instanceof TentaMissleRocket);
                }
            }, true, true);
            if(nearestEntity != null) {

                forceDetonation = true;

            }

        }

    }

    @Override
    public AxisAlignedBB aabb() {

        if(firework == null) {

            return ((CraftItem) payload).getHandle().getBoundingBox();

        } else {

            return new AxisAlignedBB(0,0,0,0,0,0);

        }

    }

    private int ticksToChange = 0;
    private LocationProvider locationProvider;

    public void launch(Location location, LocationProvider provider) {

        this.locationProvider = provider;

        net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(new ItemStack(Material.FIREWORK_ROCKET));
        EntityFireworks fireworks = new EntityFireworks(((CraftWorld)location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), stack);
        fireworks.locX = location.getX();
        fireworks.locY = location.getY();
        fireworks.locZ = location.getZ();

        firework = (Firework) fireworks.getBukkitEntity();
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(4);
        meta.addEffect(FireworkEffect.builder().withColor(getShooter().getTeam().getColor().getBukkitColor()).build());
        firework.setMetadata("Xenyria", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), true));
        firework.setFireworkMeta(meta);
        ticksToChange = 60 + (int)(new Random().nextFloat()*140);

        for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

            player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutSpawnEntity(fireworks, 76, 22));
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                if(player.isValid()) {

                    player.getNMSPlayer().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(fireworks.getId()));

                }

            }, ticksToChange);

        }

    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return 45;
    }
}
