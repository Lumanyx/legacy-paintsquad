package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class Squiffer extends AbstractCharger {

    public static final int ID = 26;

    public Squiffer() {

        super(ID, "Sepiator", 13f, 750, 105);
        enableStoreCharge(4000);
        range = 10f;

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.SQUIFFER;
    }
    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
