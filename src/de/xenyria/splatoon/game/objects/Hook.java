package de.xenyria.splatoon.game.objects;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class Hook extends GameObject implements HitableEntity {

    private Location target;
    private final double y;
    private int waveTicker = 0, maxWaveTicks = 30;
    private int colorTicks = 0;
    private Material colorMaterial = null;

    public Hook(Match match, Location target1) {

        super(match);
        this.target = target1.clone();
        y = target.getY();
        this.target.setYaw(0);
        this.target.setPitch(0f);
        mainArmorStand = (ArmorStand) target.getWorld().spawnEntity(target, EntityType.ARMOR_STAND);
        decoStand1 = (ArmorStand) target.getWorld().spawnEntity(target, EntityType.ARMOR_STAND);

        mainArmorStand.setCustomNameVisible(true);
        mainArmorStand.setCustomName("§8Zugpunkt");
        mainArmorStand.setVisible(false);
        mainArmorStand.setCanMove(false);
        mainArmorStand.setGravity(false);
        mainArmorStand.setHelmet(new ItemBuilder(Material.BLACK_STAINED_GLASS).create());

        decoStand1.setVisible(false);
        decoStand1.setCanMove(false);
        decoStand1.setGravity(false);
        decoStand1.setHelmet(new ItemBuilder(Material.LEVER).create());

    }

    public void manageArmorStandPositions() {

        double ratio = ((float)waveTicker / (float)maxWaveTicks);
        double yOffset = Math.sin(ratio);
        double finalY = y + yOffset;

        Location mainPos = mainArmorStand.getLocation();
        mainPos.setY(finalY);
        Location decoPos1 = decoStand1.getLocation();
        decoPos1.setY(finalY - 0.8);
        decoPos1.setX(target.getX());
        decoPos1.setZ(target.getZ() + .295);

        mainArmorStand.teleport(mainPos);
        decoStand1.teleport(decoPos1);

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        projectile.remove();
        if(projectile.getShooter() != null) {

            SplatoonSpecialWeapon weapon = projectile.getShooter().getEquipment().getSpecialWeapon();
            if(weapon != null && weapon.isActive()) {

                if(weapon instanceof Baller || weapon instanceof Jetpack) {

                    return;

                }

            }

            if (!projectile.getShooter().isBeingDragged() && projectile.getShooter().hasControl()) {

                colorTicks = 20;
                colorMaterial = projectile.getShooter().getTeam().getColor().getGlass();
                mainArmorStand.setHelmet(new ItemStack(projectile.getShooter().getTeam().getColor().getGlass()));
                projectile.getShooter().getWorld().playSound(target, Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.2f);

                projectile.getShooter().dragTowards(this);

            }

        }

    }

    private ArmorStand mainArmorStand = null;
    private ArmorStand decoStand1 = null;

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }
        double minY = Math.sin((float)waveTicker / (float)maxWaveTicks) + (mainArmorStand.getLocation().getY()-0.5);
        double maxY = minY + 1d;

        AxisAlignedBB bb = new AxisAlignedBB(mainArmorStand.getLocation().getX() - 0.5,
                minY, mainArmorStand.getLocation().getZ() - 0.5, mainArmorStand.getLocation().getX() + .5, maxY, mainArmorStand.getLocation().getZ() + .5);

        return bb.c(projectile.aabb());
    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return projectile.getLocation().distance(mainArmorStand.getLocation());
    }

    @Override
    public int getEntityID() {
        return mainArmorStand.getEntityId();
    }

    @Override
    public Location getLocation() {
        return mainArmorStand.getLocation().clone().add(0, 1.6, 0);
    }

    @Override
    public double height() {
        return 0.625;
    }

    @Override
    public AxisAlignedBB aabb() {

        double ratio = ((float)waveTicker / (float)maxWaveTicks);
        double yOffset = Math.sin(ratio);
        double finalY = y + yOffset + 1.6;


        return new AxisAlignedBB(mainArmorStand.getLocation().getX() - .3, finalY, mainArmorStand.getLocation().getZ() - .3,
                mainArmorStand.getLocation().getX() + .3, finalY + .625, mainArmorStand.getLocation().getZ() + .3);

    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HOOK;
    }

    private boolean incr = false;

    @Override
    public void onTick() {

        if(colorTicks > 0) {

            colorTicks--;
            if(colorTicks < 1) {

                colorMaterial = null;
                mainArmorStand.setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));

            }

        }

        if(!incr) {

            waveTicker--;
            if(waveTicker < 0) {

                waveTicker = 0;
                incr = true;

            }

        } else {

            waveTicker++;
            if(waveTicker > maxWaveTicks) {

                waveTicker = maxWaveTicks;
                incr = false;

            }

        }

        manageArmorStandPositions();

    }

    @Override
    public void reset() {

    }

    @Override
    public void onRemove() {

        mainArmorStand.remove();
        decoStand1.remove();

    }

    public Vector delta(Vector locationVector) {

        Vector curPos = target.toVector();
        return locationVector.clone().subtract(curPos).normalize();

    }

    public double distance(Vector locationVector) {

        return locationVector.distance(target.toVector());

    }

}
