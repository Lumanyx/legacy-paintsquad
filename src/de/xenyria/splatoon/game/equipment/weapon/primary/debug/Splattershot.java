package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class Splattershot extends AbstractSplattershot {

    public static final int ID = 1;

    public Splattershot() {
        super(ID, "Protokleckser", 160);
        setImpulse(0.7f);
        setMaxSpray(12f);
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.SPLATTERSHOT;
    }

    @Override
    public boolean canUse() { return !getPlayer().inSuperJump() &&
            !getPlayer().isSquid() && !getPlayer().isSplatted() && !getPlayer().isBeingDragged(); }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(1.2d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STONE_HOE;
    }

    @Override
    public void shoot() {

    }

    @Override
    public Brand getBrand() { return Brand.PROTO; }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() { return PrimaryWeaponType.SPLATTERSHOT; }

}
