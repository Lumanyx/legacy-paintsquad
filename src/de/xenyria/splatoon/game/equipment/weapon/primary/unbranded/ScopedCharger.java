package de.xenyria.splatoon.game.equipment.weapon.primary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractCharger;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;

public class ScopedCharger extends AbstractCharger {

    public static final int ID = 25;

    public ScopedCharger() {

        super(ID, "Kleckszielkonzentrator", 17f, 1200, 142);
        enableZoom(100f);
        range = 13.5f;

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.SCOPED_CHARGER;
    }
    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
