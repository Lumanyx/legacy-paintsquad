package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.ai.entity.VelocityProcessor;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.WeaponModel;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.projectile.RayProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.structure.editor.point.Point;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

public class Gusher extends GameObject implements HitableEntity {

    private Block block;
    private Vector pos;

    private BlockFace face;
    private WeaponModel model;
    private double impulse;

    public void onRemove() {

        model.removeForcefully();

    }

    public Gusher(Match match, Block mountedBlock, BlockFace face, double impulse) {

        super(match);

        block = mountedBlock;
        this.pos = mountedBlock.getLocation().toVector().add(new Vector(.5,0,.5));
        this.face = face;
        this.impulse = impulse;

        if(face == BlockFace.UP) {

            model = new WeaponModel(null, "gusher_down", mountedBlock.getWorld(), pos.toLocation(getMatch().getWorld())) {
                @Override
                public void onTick() {

                }

                @Override
                public double yOffset() {
                    return 0;
                }

                @Override
                public void handleSpecialPoint(Point point, ArmorStand stand) {

                }

            };

        }

        fountainAABB = new AxisAlignedBB(
                pos.getX() - .5, pos.getY(), pos.getZ() - .5,
                pos.getX() + .5, pos.getY() + (impulse * 5), pos.getZ() + .5
        );

        model.spawn();

    }

    private int remainingTicks = 0;
    public int getRemainingTicks() { return remainingTicks; }

    private Team lastShootingTeam = null;

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(remainingTicks < 1) {

            lastShootingTeam = projectile.getTeam();
            remainingTicks = 250;

        }
        projectile.remove();

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof InkProjectile && projectile.getShooter() == null) { return false; }

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }

        return aabb().c(projectile.aabb());

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return getLocation().distance(projectile.getLocation());
    }

    @Override
    public int getEntityID() {
        return -1;
    }

    @Override
    public Location getLocation() {
        return pos.toLocation(getMatch().getWorld());
    }

    @Override
    public double height() {
        return 1;
    }

    @Override
    public AxisAlignedBB aabb() {

        return new AxisAlignedBB(
                getLocation().getX() - .5, getLocation().getY(), getLocation().getZ() - .5,
                getLocation().getX() + .5, getLocation().getY() + 1, getLocation().getZ() + .5
        );

    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.GUSHER;
    }

    private AxisAlignedBB fountainAABB = null;

    @Override
    public void onTick() {

        if(remainingTicks > 0) {

            remainingTicks--;
            float yaw = new Random().nextFloat() * 360f;
            float pitch = -(85f + (new Random().nextFloat() * 5f));
            InkProjectile projectile = new InkProjectile(null, null, getMatch());
            projectile.setTeam(lastShootingTeam);
            projectile.spawn(getLocation().clone().add(0, 1.2, 0), yaw, pitch, (float) ((float) impulse * .5));
            getMatch().queueProjectile(projectile);

            if(remainingTicks < 1) {

                lastShootingTeam = null;

            }

        }

        for(SplatoonPlayer player : getMatch().getAllPlayers()) {

            if(!player.isSplatted() && player.getTeam() == lastShootingTeam) {

                if(player.specialActive()) {

                    SplatoonSpecialWeapon weapon = player.getEquipment().getSpecialWeapon();
                    if(weapon instanceof Jetpack || weapon instanceof Splashdown || weapon instanceof Baller) {

                        continue;

                    }

                }

                if(player.aabb().c(fountainAABB)) {

                    if(!player.isSquid()) {

                        if(player.getVelocity().getY() < 0.2) {

                            player.setVelocity(player.getVelocity().add(new Vector(0, 0.25, 0)));

                        }

                    } else {

                        if(player.getSquidVelocityY() < 0.8) {

                            player.addSquidVelocity(.4);

                        }

                    }

                }

            }

        }

    }

    @Override
    public void reset() {

        remainingTicks = 0;

    }

    public Team getOwningTeam() { return lastShootingTeam; }
    public double computeHeight() {

        VelocityProcessor processor = new VelocityProcessor();
        processor.setVelocity(new Vector(0, impulse - 0.01, 0));
        double gainedHeight = 0d;
        while (processor.getVelocity().getY() > 0) {

            gainedHeight+=processor.getVelocity().getY();
            processor.process();

        }
        return getLocation().getY() + gainedHeight;

    }

}
