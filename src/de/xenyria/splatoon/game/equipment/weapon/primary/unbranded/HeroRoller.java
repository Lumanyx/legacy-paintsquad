package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class HeroRoller extends AbstractRoller {

    public static final int ID = 41;

    public HeroRoller() {

        super(ID, "Heldenroller Replik", 0.025f, 0.25f, 2.65f, 0.2f, 17, 120, 50f);

    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {

        if(isRolling()) {

            return ResourcePackItemOption.HEROROLLER_ROLLING;

        } else {

            return ResourcePackItemOption.HEROROLLER_IDLE;

        }

    }
}
