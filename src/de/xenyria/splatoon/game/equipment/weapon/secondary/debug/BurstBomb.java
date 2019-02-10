package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractBurstBomb;

public class BurstBomb extends AbstractBurstBomb {

    public static final int ID = 20;
    public BurstBomb() {
        super(ID, "Instabombe", 2f, 27);
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

}
