package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class InkjetProjectile extends SplatoonProjectile {

    private Item item;
    private float maxDistance;

    public InkjetProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);
        maxDistance = 48;

    }

    private Vector direction;
    private Location origin;
    public void spawn(Location location, Vector direction) {

        this.origin = location.clone();
        this.direction = direction;
        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(new ItemBuilder(getShooter().getTeam().getColor().getClay()).addEnchantment(Enchantment.DURABILITY, 1).create());
        item.setGravity(false);
        item.setCanMobPickup(false);
        item.setPickupDelay(99999);
        item.setVelocity(direction);
        ((CraftItem)item).getHandle().getBoundingBox().setFilter(NMSUtil.filter);

    }

    @Override
    public Location getLocation() {
        return item.getLocation();
    }

    @Override
    public void onRemove() {

        item.remove();
        NMSUtil.broadcastEntityRemovalToSquids(item);

    }

    private float speed = .7f;

    @Override
    public void tick() {

        if(origin.distance(getLocation()) < maxDistance) {

            item.setVelocity(direction.clone().multiply(speed));
            Vec3D cur = new Vec3D(getLocation().getX(), getLocation().getY(), getLocation().getZ());
            Vec3D tar = new Vec3D(getLocation().getX() + (direction.getX()*.2), getLocation().getY() + (direction.getY()*.2), getLocation().getZ() + (direction.getZ()*.2));

            SplatoonServer.broadcastColorizedBreakParticle(getLocation().getWorld(),
                    getLocation().getX(), getLocation().getY(), getLocation().getZ(), getShooter().getTeam().getColor());

            MovingObjectPosition pos = ((CraftWorld)getLocation().getWorld()).getHandle().rayTrace(cur, tar, FluidCollisionOption.NEVER, false, false);
            if(pos != null) {

                detonate();
                return;

            }


            for (HitableEntity entity : getMatch().getHitableEntities()) {

                if (entity.isHit(this)) {

                    detonate();
                    remove();
                    return;


                }

            }

        } else {

            detonate();
            remove();

        }

    }

    private float radius = 4f;
    private float peakDamage = 250;
    public void detonate() {

        BombProjectile projectile = new BombProjectile(getShooter(), getWeapon(), getMatch(), radius, 0, peakDamage, false);
        projectile.spawn(0, getLocation());
        remove();

    }

    @Override
    public AxisAlignedBB aabb() {

        AxisAlignedBB bb = ((CraftItem)item).getHandle().getBoundingBox();
        bb = bb.grow(1d);

        return bb;

    }
}
