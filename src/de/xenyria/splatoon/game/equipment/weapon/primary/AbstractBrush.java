package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.BrushWeaponModel;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.RollerWeaponModel;
import de.xenyria.splatoon.game.projectile.DamageReason;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import net.minecraft.server.v1_13_R2.PacketPlayOutAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public abstract class AbstractBrush extends AbstractRoller {

    public AbstractBrush(int id, String name, float peakRollSpeed, float minRollSpeed, float thickness, float rollUsage, float splatUsage, float impactDamage, float splatDamage) {
        super(id, name, peakRollSpeed, minRollSpeed, thickness, rollUsage, splatUsage, impactDamage, splatDamage);
        splatTicks = 3;

    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.BRUSH;
    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {

        if(isRolling()) {

            return ResourcePackItemOption.INKBRUSH_SURF;

        } else {

            return ResourcePackItemOption.INKBRUSH_IDLE;

        }

    }

    public Material getRepresentiveMaterial() {

        return Material.DIAMOND_SHOVEL;

    }

    private boolean leftHand = false;
    private int ticksSinceSplat = 0;

    public void syncTick() {

        ticksSinceSplat++;
        ticksSinceSwing++;
        if(ticksSinceSplat > 15) {

            leftHand = false;

        }
        super.syncTick();

    }

    public void fireSplat(Trajectory[] trajectory, Location[] plannedHitLocation) {

        ticksSinceSplat=0;
        splatPhase = false;

        float range = splatRange;
        float beginYaw = getPlayer().yaw();
        int i = 0;
        for(float f = 0; f < range; f+=10f) {

            float endYaw = beginYaw;
            if(!leftHand) {

                endYaw+=f;

            } else {

                endYaw-=f;

            }

            InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
            projectile.withDamage(splatDamage);
            projectile.withReason(DamageReason.WEAPON);
            projectile.setDrippingRatio(2);
            if(trajectory == null || trajectory[i] == null) {

                projectile.spawn(getPlayer().getEyeLocation(), endYaw, getPlayer().pitch() - ROLLER_PITCH_OFFSET, getImpulse());

            } else {

                EntityNPC npc = (EntityNPC) getPlayer();
                projectile.spawn(trajectory[i], npc.getShootingLocation(leftHand), plannedHitLocation[i]);

            }
            i++;

        }

        rolling = true;
        rollTicks = 0;
        getPlayer().enableWalkSpeedOverride();
        getPlayer().setOverrideWalkSpeed(minRollSpeed);

        for(Player player : getPlayer().getWorld().getPlayers()) {

            if(player.getLocation().distance(getPlayer().getLocation()) <= 64D) {

                ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutAnimation(
                        getPlayer().getNMSEntity(), 0
                ));

            }

        }

        leftHand = !leftHand;

    }

    public void createModel() {

        setModel(new BrushWeaponModel(getPlayer(), getPlayer().getWorld(), getPlayer().getLocation()));

    }

    private int swingTicks = 5;
    private int ticksSinceSwing = 0;
    public void swing() {

        float usage = splatUsage;
        if(getPlayer().hasEnoughInk(usage)) {

            if(!isRolling()) {

                if (ticksSinceSwing >= swingTicks) {

                    ticksSinceSwing = 0;
                    fireSplat(null, null);
                    getPlayer().removeInk(usage);

                }

            }

        } else {

            getPlayer().notEnoughInk();

        }

    }

}
