package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractBurstBomb;

public class BurstBomb extends AbstractBurstBomb {

    public static final int ID = 20;
    public static final double IMPULSE = .6d;
    public static final float MAX_DAMAGE = 27;
    public static final float RADIUS = 2.5f;

    public BurstBomb() {
        super(ID, "Instabombe", RADIUS, MAX_DAMAGE);
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public double getImpulse() {
        return IMPULSE;
    }
}
