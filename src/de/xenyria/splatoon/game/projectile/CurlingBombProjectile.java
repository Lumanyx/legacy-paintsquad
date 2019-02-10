package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.NMSUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CurlingBombProjectile extends SplatoonProjectile implements DamageDealingProjectile {

    private Item bomb;

    public CurlingBombProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        super(shooter, weapon, match);

    }

    private Vector direction = new Vector();

    public void spawn(Location location) {

        bomb = location.getWorld().dropItem(location, new ItemStack(Material.FLINT));
        bomb.setPickupDelay(99999);
        bomb.setCanMobPickup(false);

        Location cl = location.clone();
        cl.setPitch(0f);
        direction = cl.getDirection();

    }

    @Override
    public Location getLocation() {
        return bomb.getLocation();
    }

    @Override
    public void onRemove() {

        bomb.remove();
        NMSUtil.broadcastEntityRemovalToSquids(bomb);

    }

    private int noCollisionTicks = 0;
    private double speed = 0.34;
    private int moveTicks;
    private boolean soundPlayed = false;
    private int soundTicks = 0;

    @Override
    public void tick() {

        soundTicks++;
        if(soundTicks > 5) {

            soundTicks = 0;
            getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_MINECART_RIDING, 0.3f, 1.3f);

        }
        Vec3D oldPos = new Vec3D(bomb.getLocation().getX(), bomb.getLocation().getY(), bomb.getLocation().getZ());
        Vec3D newPos = new Vec3D(bomb.getLocation().getX(), bomb.getLocation().getY(), bomb.getLocation().getZ());
        newPos = newPos.add(direction.getX() * 1.15, 0, direction.getZ() * 1.15);

        boolean wrapInstead = false;
        AxisAlignedBB targetBB = new AxisAlignedBB(newPos.x - .125, newPos.y, newPos.z - .125, newPos.x + .125, newPos.y + .125, newPos.z + .125);
        if(!AABBUtil.hasSpace(getLocation().getWorld(), targetBB)) {

            Vector newTargetPos = AABBUtil.resolveWrap(getLocation().getWorld(), new Vector(
                    newPos.x, newPos.y, newPos.z
            ), targetBB);
            if(newTargetPos != null) {

                wrapInstead = true;
                newPos = new Vec3D(newTargetPos.getX(), newTargetPos.getY(), newTargetPos.getZ());

            }

        }

        moveTicks++;

        MovingObjectPosition pos = ((CraftWorld)bomb.getWorld()).getHandle().rayTrace(
                oldPos, newPos, FluidCollisionOption.NEVER, true, true
        );

        getMatch().paint(bomb.getLocation().clone().add(0, -0.5, 0).toVector(), getShooter());
        getMatch().paint(bomb.getLocation().clone().add(0.2, -0.5, 0).toVector(), getShooter());
        getMatch().paint(bomb.getLocation().clone().add(-0.2, -0.5, 0).toVector(), getShooter());
        getMatch().paint(bomb.getLocation().clone().add(0, -0.5, 0.2).toVector(), getShooter());
        getMatch().paint(bomb.getLocation().clone().add(0, -0.5, -0.2).toVector(), getShooter());

        if(noCollisionTicks>0) { noCollisionTicks--; }
        if(!wrapInstead && pos != null && noCollisionTicks < 1) {

            Block block = getLocation().getWorld().getBlockAt((int)pos.pos.x, (int)pos.pos.y, (int)pos.pos.z).getRelative(BlockFace.valueOf(pos.direction.name()));
            if(pos.type == MovingObjectPosition.EnumMovingObjectType.BLOCK && !AABBUtil.isPassable(block.getType())) {

                //if(!AABBUtil.isPassable(block.getType())) {

                    EnumDirection direction1 = pos.direction;
                    BlockFace face = BlockFace.valueOf(direction1.name());
                    BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

                    int indexOfFace = 0;
                    for (int i = 0; i < faces.length; i++) {

                        if (faces[i] == face) {
                            indexOfFace = i;
                            break;
                        }

                    }
                    if (indexOfFace > 3) {
                        indexOfFace = 0;
                    }
                    if (indexOfFace < 0) {
                        indexOfFace = 3;
                    }
                    BlockFace normalFace = faces[indexOfFace];

                    Vector normal = new Vector(normalFace.getModX(), normalFace.getModY(), normalFace.getModZ());

                    Vector r = direction.clone().subtract(
                            (direction.clone().multiply(normal)).multiply(2).multiply(normal)
                    );

                    // Getroffenen Block einfärben
                    if (getMatch().isPaintable(getShooter().getTeam(), getLocation().getWorld().getBlockAt(
                            (int) pos.pos.x, (int) pos.pos.y, (int) pos.pos.z
                    ))) {

                        getMatch().paint(new Vector(pos.pos.x, pos.pos.y, pos.pos.z), getShooter());

                    }
                    SplatoonServer.broadcastColorParticleExplosion(getLocation().getWorld(), getLocation().getX(), getLocation().getY(), getLocation().getZ(), getShooter().getTeam().getColor());
                    getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_ANVIL_HIT, 0.3f, 1.4f);

                    noCollisionTicks = 5;
                    r = r.normalize();
                    direction = r;

                //}

            } else {

                double velocityY = bomb.getVelocity().getY();
                velocityY-=0.1;
                if(velocityY < -0.4) { velocityY = -.4; }

                bomb.setVelocity(direction.clone().multiply(speed).add(new Vector(0, velocityY, 0)));

            }

        } else {

            if(wrapInstead) {

                bomb.teleport(new Location(getLocation().getWorld(), newPos.x, newPos.y, newPos.z));

            }
            direction.setY(-0.125);
            bomb.setVelocity(direction.clone().multiply(speed));

        }

        if(moveTicks > 70) {

            if(!soundPlayed) {

                getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.6f);
                soundPlayed = true;

            }

            speed-=0.032;
            if(speed < 0.1) {

                BombProjectile projectile = new BombProjectile(getShooter(), getWeapon(), getMatch(), 2.5f, 0, 135, false);
                projectile.spawn(0, bomb.getLocation());
                projectile.getShooter().getMatch().queueProjectile(projectile);
                bomb.getLocation().getWorld().playSound(bomb.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

                remove();

            }

        }

        for(HitableEntity entity : getMatch().getHitableEntities()) {

            if(entity.isHit(this)) {

                entity.onProjectileHit(this);

            }

        }

    }

    @Override
    public AxisAlignedBB aabb() {
        return ((CraftItem)bomb).getHandle().getBoundingBox();
    }

    @Override
    public boolean dealsDamage() {
        return true;
    }

    @Override
    public float getDamage() {
        return 12f;
    }

}
