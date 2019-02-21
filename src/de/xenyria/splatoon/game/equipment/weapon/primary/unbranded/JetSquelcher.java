package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class JetSquelcher extends AbstractSplattershot {

    public static final int ID = 29;
    public JetSquelcher() {

        super(ID, "Platscher", 260);
        setImpulse(1.2f);
        setMaxSpray(4f);
        setBaseDamage(21);

    }

    
    @Override
    public float range() {
        return 9f;
    }

    @Override
    public float getMovementSpeedOffset() {

        return -.12f;

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

        setNextInkUsage(1.6);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STONE_HOE;
    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.JET_SQUELCHER;
    }

    @Override
    public void shoot() {

    }
}
