package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrella;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.InstantDamageKnockbackProjectile;
import de.xenyria.splatoon.game.util.AABBUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import de.xenyria.structure.editor.point.Point;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.EnumMoveType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class DetachedBrella extends GameObject implements RemovableGameObject {

    public SplatoonPlayer owner;
    public AbstractBrella brella;

    public DetachedBrella(SplatoonPlayer player, Match match, AbstractBrella brella) {

        super(match);
        this.owner = player;
        location = brella.getPlayer().getLocation().clone();
        this.brella = brella;

        for(Point point : brella.getModel().getSpecialPoints().keySet()) {

            if(point.isSpecial()) {

                BrellaTile tile = new BrellaTile(getMatch(), this, point);
                tiles.add(tile);
                match.addGameObject(tile);

            }

        }

    }


    public Location getLocation() {
        return location;
    }

    private Vector vector;
    private Location location;

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private boolean detached = false;
    private int remainingTicks = 0;
    private EntityArmorStand positionStand;
    private Vector velocity = new Vector();

    public void remove() {

        if(brella.getModel().isActive()) {

            brella.getModel().remove();

        }
        positionStand = null;
        brella.getPlayer().getMatch().queueObjectRemoval(this);

    }

    private float lastYaw;
    public ArrayList<HitableEntity> tiles = new ArrayList<>();

    @Override
    public void onTick() {

        if(brella.destructionPunishTicksActive()) { return; }

        if((brella.isHolding() && brella.millisToBeginHold() > 1000) || positionStand != null) {

            for(HitableEntity entity : getMatch().getHitableEntities()) {

                if(entity != this && !tiles.contains(entity) && entity != brella.getPlayer()) {

                    for(HitableEntity tile : tiles) {

                        if(tile.aabb().c(entity.aabb())) {

                            Vector offset = entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(0.3);
                            if(VectorUtil.isValid(offset)) {

                                InstantDamageKnockbackProjectile projectile = new InstantDamageKnockbackProjectile(
                                        location,
                                        brella.getPlayer(),
                                        brella,
                                        offset,
                                        brella.getDamage() * .2,
                                        brella.getPlayer().getMatch());
                                if (entity.isHit(projectile)) {

                                    velocity = velocity.add(offset.clone().multiply(-1));
                                    entity.onProjectileHit(projectile);

                                }

                            }

                        }

                    }

                }

            }

            if (brella.hp < 0 || brella.getPlayer().isSplatted()) {

                if (brella.getModel().isActive()) {

                    brella.getModel().remove();

                }
                positionStand = null;
                detached = false;
                brella.resetBrella();
                remainingTicks = 0;
                velocity = new Vector();

                return;

            }

            if (!detached) {

                if (brella.hasToDetach()) {

                    brella.registerDetach();
                    vector = brella.getPlayer().getLocation().getDirection().clone();
                    vector.setY(0);
                    remainingTicks = 300;
                    detached = true;
                    location = brella.getPlayer().getLocation();
                    lastYaw = brella.getPlayer().getLocation().getYaw();

                    positionStand = new EntityArmorStand(
                            ((CraftWorld) location.getWorld()).getHandle(),
                            location.getX(), location.getY(), location.getZ()
                    );
                    location.getWorld().playSound(location, Sound.UI_BUTTON_CLICK, 0.6f, 0.2f);
                    positionStand.setPosition(location.getX(), location.getY(), location.getZ());

                } else {

                    location = brella.getPlayer().getLocation();

                }

                if (brella.getModel().isActive()) {

                    brella.getModel().moveToPosition(location.getX(), location.getY(), location.getZ());
                    brella.getModel().rotateTowards(brella.getPlayer().getLocation().getYaw());

                }

            } else {

                velocity = velocity.multiply(0.7);
                if(!VectorUtil.isValid(velocity)) {

                    velocity = new Vector();

                }

                if(velocity.length() < 0.06) {

                    velocity = velocity.add(vector.clone().multiply(.06));

                }

                Vector targetPos = new Vector(
                        positionStand.locX + velocity.getX(),
                        positionStand.locY + (velocity.getY()),
                        positionStand.locZ + velocity.getZ()
                );
                AxisAlignedBB newAABB = new AxisAlignedBB(targetPos.getX() - .3, targetPos.getY(), targetPos.getZ() - .3, targetPos.getX() + .3, targetPos.getY() + 1.8, targetPos.getZ() + .3);
                if(!AABBUtil.hasSpace(getLocation().getWorld(), newAABB)) {

                    Vector vector = AABBUtil.resolveWrap(getLocation().getWorld(), targetPos, newAABB);
                    if(vector != null) {

                        targetPos = vector;

                    }

                }

                positionStand.move(EnumMoveType.SELF, targetPos.getX() - positionStand.locX,
                        targetPos.getY() - positionStand.locY - 0.01, targetPos.getZ() - positionStand.locZ);
                location = positionStand.getBukkitEntity().getLocation();
                Block below = location.getBlock().getRelative(BlockFace.DOWN);
                getMatch().colorSquare(below, brella.getPlayer().getTeam(), brella.getPlayer(), 1);

                if (positionStand.onGround) {

                    if (velocity.getY() < 0) {

                        velocity.setY(0);

                    }

                } else {

                    if (velocity.getY() > -0.8) {

                        velocity.add(new Vector(0, -0.08, 0));

                    } else {

                        velocity.setY(-0.8);

                    }

                }
                if (brella.getModel().isActive()) {

                    brella.getModel().moveToPosition(positionStand.locX, positionStand.locY, positionStand.locZ);

                }

                remainingTicks--;
                if (remainingTicks < 1) {

                    brella.removeBrella();
                    brella.resetBrella();
                    remainingTicks = 0;
                    detached = false;
                    positionStand = null;
                    velocity = new Vector();

                }

            }

        }

    }

    @Override
    public void reset() {

    }

    public void handleDamage(Location location, float damage) {

        if(location != null) {

            Vector target = location.toVector().subtract(location.toVector()).normalize().multiply(0.1);
            velocity.add(target);

        }
        brella.hp-=damage;

    }

}
