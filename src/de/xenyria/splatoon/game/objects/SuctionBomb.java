package de.xenyria.splatoon.game.objects;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.EulerAngle;

public class SuctionBomb extends GameObject implements HitableEntity {

    private SplatoonPlayer player;
    private BlockFace normal, face;
    private Block block;
    private Location spawnPos;

    private ArmorStand stand;

    @Override
    public void onRemove() {

        stand.remove();

    }

    public SuctionBomb(Match match, SplatoonPlayer owner, Block block, BlockFace normal, float radius) {

        super(match);
        this.player = owner;
        this.block = block;
        this.normal = normal;
        face = normal.getOppositeFace();
        Location spawnLoc = new Location(player.getLocation().getWorld(), (int)block.getX(), (int)block.getY(), (int)block.getZ());

        spawnLoc = spawnLoc.add(.5, .5, .5);
        spawnLoc = spawnLoc.add(normal.getDirection().clone().multiply(0.325));
        float[] dir = DirectionUtil.directionToYawPitch(normal.getDirection());
        spawnLoc.setYaw(dir[0]);
        spawnPos = spawnLoc.clone();
        this.radius = radius;

        spawnLoc = spawnLoc.add(0, -1.6, 0);
        explosionTicks = 40;

        stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setCanMove(false);
        stand.setCanTick(false);
        stand.setHelmet(new ItemBuilder(Material.SLIME_SPAWN_EGG).addEnchantment(Enchantment.DURABILITY, 1).create());
        float pitch = 0f;
        if(normal == BlockFace.UP) { pitch = 0f; } else if(normal == BlockFace.DOWN) { pitch = 180f; } else { pitch = 90f; }

        stand.setHeadPose(new EulerAngle(Math.toRadians(pitch), 0, 0));

    }


    @Override
    public ObjectType getObjectType() {
        return ObjectType.SUCTION_BOMB;
    }

    public void remove() {

        stand.getLocation().getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnPos, 0);
        stand.remove();
        getMatch().queueObjectRemoval(this);

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        remove();

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        return false;

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().distance(spawnPos.clone().add(0, 0, 0));
    }

    @Override
    public int getEntityID() {
        return stand.getEntityId();
    }

    @Override
    public Location getLocation() {
        return spawnPos.clone();
    }

    @Override
    public double height() {
        return 0.625;
    }

    public SplatoonPlayer getOwner() { return player; }

    private int explosionTicks = 0;
    private boolean warnSound = false;
    private float radius = 4f;

    @Override
    public AxisAlignedBB aabb() {
        return new AxisAlignedBB(spawnPos.getX() - .3, spawnPos.getY() - .3, spawnPos.getZ() - .3, spawnPos.getX() + .3, spawnPos.getY() + .3, spawnPos.getZ() + .3);
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void onTick() {

        if(explosionTicks > 0) {

            if(explosionTicks < 7 && !warnSound) {

                spawnPos.getWorld().playSound(spawnPos, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.6f);
                warnSound = true;

            }

            explosionTicks--;
            if(explosionTicks < 1) {

                BombProjectile projectile = new BombProjectile(getOwner(), getOwner().getEquipment().getSecondaryWeapon(), getMatch(), radius, 0, 120, true);
                projectile.spawn(0, spawnPos);
                getOwner().getMatch().queueProjectile(projectile);
                spawnPos.getWorld().playSound(spawnPos, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                remove();

            }

        }

    }

    @Override
    public void reset() {

    }
}
