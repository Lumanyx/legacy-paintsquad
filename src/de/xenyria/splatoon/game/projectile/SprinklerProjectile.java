package de.xenyria.splatoon.game.projectile;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.Sprinkler;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SprinklerProjectile extends SplatoonProjectile {

    public SprinklerProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

    private Item item;

    public void spawn(double impulse, Location location, Vector direction) {

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(new ItemBuilder(getShooter().getTeam().getColor().getGlass()).addEnchantment(Enchantment.DURABILITY, 1).create());
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

        Vector vel = item.getVelocity();
        if(!VectorUtil.isValid(vel)) {

            vel = new Vector(0, -1, 0);

        }

        RayTraceResult result = getLocation().getWorld().rayTraceBlocks(getLocation(), vel.normalize(), 1d);
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

        // Jeden alten Sprinkler des Spielers entfernen
        for(GameObject object : getMatch().getGameObjects()) {

            if(object instanceof Sprinkler) {

                Sprinkler sprinkler = (Sprinkler)object;
                if(sprinkler.getOwner().equals(getShooter())) {

                    sprinkler.remove();

                }

            }

        }

        Sprinkler sprinkler = new Sprinkler(getMatch(), getShooter(), block, face.getOppositeFace());
        getMatch().addGameObject(sprinkler);
        remove();

    }

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)item).getHandle().getBoundingBox();
    }
}
