package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractSplatling;

public class Splatling extends AbstractSplatling {

    public Splatling() {

        super(16, "Splatling", 26, 1400, 23);

    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }
}
