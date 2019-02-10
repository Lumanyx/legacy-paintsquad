package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.LocationProvider;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
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

        if(ticksToChange > 0 && !forceDetonation) {

            ticksToChange--;
            if(ticksToChange < 1) {

                if(firework != null) { firework.remove();
                    NMSUtil.broadcastEntityRemovalToSquids(firework); firework = null; }
                Location target = locationProvider.getLocation();

                double mod1 = 1d;
                double mod2 = 1d;
                if (new Random().nextBoolean()) { mod1 = -1d; }
                if (new Random().nextBoolean()) { mod2 = -1d; }

                target = target.add(new Random().nextDouble() * mod1, 8, new Random().nextDouble() * mod2);

                payload = (Item) target.getWorld().spawnEntity(target, EntityType.DROPPED_ITEM);
                payload.setVelocity(new Vector(0, -0.2, 0));
                payload.setPickupDelay(99999);
                payload.setItemStack(new ItemStack(Material.FIREWORK_ROCKET));
                payload.setCanMobPickup(false);

            }

        } else {

            double deltaToLastX = Math.abs(payload.getLocation().getX() - lastX);
            double deltaToLastY = Math.abs(payload.getLocation().getY() - lastY);
            double deltaToLastZ = Math.abs(payload.getLocation().getZ() - lastZ);

            if(Math.abs(deltaToLastX + deltaToLastY + deltaToLastZ) < 0.1 || payload.isOnGround() || forceDetonation) {

                BombProjectile projectile = new BombProjectile(getShooter(), getWeapon(), getMatch(), 2f, 0, 75, false);
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
            });
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
        firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(4);
        meta.addEffect(FireworkEffect.builder().trail(true).withColor(getShooter().getTeam().getColor().getBukkitColor()).build());
        firework.setMetadata("Xenyria", new FixedMetadataValue(XenyriaSplatoon.getPlugin(), true));
        firework.setFireworkMeta(meta);
        ticksToChange = 90;

    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return 40;
    }
}
