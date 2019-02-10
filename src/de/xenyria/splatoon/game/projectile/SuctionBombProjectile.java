package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Sprinkler;
import de.xenyria.splatoon.game.objects.SuctionBomb;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SuctionBombProjectile extends SplatoonProjectile {

    public SuctionBombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

    private Item item;

    public void spawn(double impulse, Location location, Vector direction) {

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(new ItemBuilder(getShooter().getTeam().getColor().getClay()).addEnchantment(Enchantment.DURABILITY, 1).create());
        item.setVelocity(direction.clone().multiply(impulse));
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

    @Override
    public void tick() {

        if(item.getLocation().getY() < 0) {

            remove();
            return;

        }

        RayTraceResult result = getLocation().getWorld().rayTraceBlocks(getLocation(), item.getVelocity().clone().normalize(), 1.5d);
        if(result != null) {

            if(result.getHitBlock() != null && canMountTo(result.getHitBlock())) {

                mount(getLocation().getWorld().getBlockAt(
                        (int)result.getHitBlock().getX(),
                        (int)result.getHitBlock().getY(),
                        (int)result.getHitBlock().getZ()
                        ), result.getHitBlockFace().getOppositeFace());

            }

        } else if(item.isOnGround()) {

            if(canMountTo(item.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                mount(item.getLocation().getBlock().getRelative(BlockFace.DOWN), BlockFace.DOWN);

            }

        }

    }

    public boolean canMountTo(Block block) {

        return block.getType().isSolid() && !AABBUtil.isPassable(block.getType());

    }

    public void mount(Block block, BlockFace face) {

        SuctionBomb bomb = new SuctionBomb(getMatch(), getShooter(), block, face.getOppositeFace(), 3.5f);
        getMatch().addGameObject(bomb);
        remove();

    }

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }
}
