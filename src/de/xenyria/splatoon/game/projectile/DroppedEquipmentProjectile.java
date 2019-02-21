package de.xenyria.splatoon.game.projectile;

import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class DroppedEquipmentProjectile extends SplatoonProjectile {

    private Item item;
    public DroppedEquipmentProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match, ItemStack stack, Location location) {

        super(shooter, weapon, match);
        item = (Item) match.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        float yaw = new Random().nextFloat()*360f;
        float pitch = -80f;

        item.setVelocity(DirectionUtil.yawAndPitchToDirection(yaw, pitch).multiply(0.4));
        item.setPickupDelay(99999);
        item.setItemStack(stack);
        item.setCanMobPickup(false);

    }
    private int ticksToLive = 45;

    @Override
    public Location getLocation() {
        return new Location(getMatch().getWorld(),0,0,0);
    }

    @Override
    public void onRemove() {

        if(item != null) {

            item.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE, item.getLocation(), 0);
            item.remove();

        }

    }

    @Override
    public void tick() {

        ticksToLive--;
        if(ticksToLive < 1 || item.isOnGround()) {

            remove();

        }

    }

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(-1,-1,-1,-1,-1,-1);
    }
}
