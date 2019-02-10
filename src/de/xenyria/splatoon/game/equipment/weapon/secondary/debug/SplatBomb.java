package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSplatBomb;

public class SplatBomb extends AbstractSplatBomb {

    public static int ID = 2;

    public SplatBomb() {
        super(ID, "Klecksbombe", 24, 4.5f);
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }

}
