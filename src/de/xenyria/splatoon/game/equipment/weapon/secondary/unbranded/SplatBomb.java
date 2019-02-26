package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSplatBomb;

public class SplatBomb extends AbstractSplatBomb {

    public static final float RADIUS = 4.5f;
    public static final int EXPLOSION_TICKS = 24;
    public static final double IMPULSE = 0.7d;
    public static final int MAX_DAMAGE = 165;
    public static int ID = 2;

    public SplatBomb() {
        super(ID, "Klecksbombe", EXPLOSION_TICKS, RADIUS);
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

}
