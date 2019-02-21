package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class SplatRoller extends AbstractRoller {

    public static final int ID = 4;

    public SplatRoller() {

        super(ID, "Klecksroller", 0.025f, 0.25f, 3f, 0.2f, 17, 120, 50f);

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

            return ResourcePackItemOption.ROLLER_ROLLING;

        } else {

            return ResourcePackItemOption.ROLLER_IDLE;

        }

    }
}
