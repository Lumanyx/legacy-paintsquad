package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class Charger extends AbstractCharger {

    public static final int ID = 7;

    public Charger() {
        super(ID, "Kleckskonzentrator", 17f, 1200, 140f);
        range = 12.5f;
        enableStoreCharge(4000);

    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }


    public ResourcePackItemOption getResourcepackOption() {

        return ResourcePackItemOption.DEFAULT_CHARGER;

    }

}
