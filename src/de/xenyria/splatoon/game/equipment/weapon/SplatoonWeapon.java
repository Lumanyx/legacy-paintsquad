package de.xenyria.splatoon.game.equipment.weapon;

import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import net.minecraft.server.v1_13_R2.Resource;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class SplatoonWeapon {

    public SplatoonWeapon(int id, String name) {

        this.id = id;
        this.name = name;

    }

    public void reset() {

        if(this instanceof SplatoonPrimaryWeapon) {

            player.getEquipment().resetPrimaryWeapon();

        } else if(this instanceof SplatoonSecondaryWeapon) {

            player.getEquipment().resetSecondaryWeapon();

        } else if(this instanceof SplatoonSpecialWeapon) {

            player.getEquipment().resetSpecialWeapon();

        }
        cleanUp();
        player = null;

    }
    public void cleanUp() {}

    private int id;
    public int getID() { return id; }

    private String name;
    public String getName() { return name; }

    public abstract void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player);
    public abstract void syncTick();
    public abstract void asyncTick();

    private float movementSpeedOffset = 0f;
    public void setMovementSpeedOffset(float movementSpeedOffset) { this.movementSpeedOffset = movementSpeedOffset; }
    public float getMovementSpeedOffset() {

        return movementSpeedOffset;

    }

    private SplatoonPlayer player;
    public SplatoonPlayer getPlayer() { return player; }

    public abstract boolean canUse();

    private double nextInkUsage = 0d;
    public double getNextInkUsage() { return nextInkUsage; }
    public void setNextInkUsage(double usage) { this.nextInkUsage = usage; }
    public abstract void calculateNextInkUsage();

    public abstract Material getRepresentiveMaterial();
    public abstract ItemStack asItemStack();
    public ResourcePackItemOption getResourcepackOption() { return ResourcePackItemOption.NONE; }

    public boolean isSelected() {

        if(this instanceof SplatoonPrimaryWeapon) { return getPlayer().heldItemSlot() == 0; }
        if(this instanceof SplatoonSecondaryWeapon) { return getPlayer().heldItemSlot() == 1; }
        if(this instanceof SplatoonSpecialWeapon) { return getPlayer().heldItemSlot() == 3; }
        return false;

    }

    public void use() {

        if(canUse()) {

            if(player.getInk() >= nextInkUsage) {

                player.removeInk(nextInkUsage);
                shoot();

            } else {

                player.notEnoughInk();

            }

        }

    }
    // Wird nur von Nicht-Automatik-Waffen benutzt
    public abstract void shoot();

    public void assign(SplatoonPlayer player) {

        this.player = player;
        calculateNextInkUsage();

    }
    public boolean isInitialized() { return player != null; }

    public void uninitialize() {

        player = null;

    }

}
