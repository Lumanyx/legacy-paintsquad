package de.xenyria.splatoon.game.equipment.weapon.primary.ai.tutorial;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class TutorialOctoshot extends AbstractSplattershot {

    public TutorialOctoshot() {
        super(23, "(Tutorial) Oktokleckser", 140);
        setMaxSpray(14f);
        setBaseDamage(8d);
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
        return true;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(1d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STONE_HOE;
    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.OCTOSHOT;
    }

    @Override
    public void shoot() {

    }

    @Override
    public float range() {
        return 3.2f;
    }
}
