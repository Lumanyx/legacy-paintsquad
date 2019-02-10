package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class HeroCharger extends AbstractCharger {

    public static int ID = 27;

    public HeroCharger() {

        super(ID, "Heldenkonzentrator", 17f, 1200, 140f);

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.HEROCHARGER;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
