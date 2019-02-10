package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class SplattershotJr extends AbstractSplattershot {

    public SplattershotJr() {
        super(21, "Junior-Kleckser", 140);
        setImpulse(.55f);
        setMaxSpray(13f);
        setBaseDamage(18);

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.SPLATTERSHOT_JR;
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.SPLATTERSHOT;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(1.4d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STONE_HOE;
    }

    @Override
    public void shoot() {

    }

    @Override
    public float nextSprayYaw() {
        return 0;
    }

    @Override
    public float nextSprayPitch() {
        return 0;
    }

}
