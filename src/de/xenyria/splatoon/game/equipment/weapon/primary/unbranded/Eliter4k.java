package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class Eliter4k extends AbstractCharger {

    public static int ID = 28;
    public Eliter4k() {

        super(ID, "Eliter-4k", 24f, 2000, 170f);
        range = 24f;
        enableStoreCharge(4000);

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.ELITER;
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
