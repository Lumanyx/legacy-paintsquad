package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class CarbonRoller extends AbstractRoller {

    public static final int ID = 32;

    public CarbonRoller() {

        // super(4, "Protoroller", 0.03f, 0.22f, 2.5f, 0.2f, 17, 120, 50f);
        super(ID, "Karbonroller", 0.05f, 0.25f, 3, 0.18f, 16, 73, 72);

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {

        if(isRolling()) {

            return ResourcePackItemOption.CARBON_ROLL;

        } else {

            return ResourcePackItemOption.CARBON_IDLE;

        }

    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }
}
