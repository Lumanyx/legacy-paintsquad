package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSprinkler;

public class SprinklerSecondary extends AbstractSprinkler {

    public static final int ID = 8;

    public SprinklerSecondary() {
        super(ID, "Protosprinkler", 3f);
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }
}
