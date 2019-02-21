package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSplatBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSuctionBomb;

public class SuctionBomb extends AbstractSuctionBomb {

    public static final int ID = 12;
    public static final float RADIUS = 4f;
    public static final double MAX_DAMAGE = 180d;
    public static final double IMPULSE = 0.8d;

    public SuctionBomb() {
        super(ID, "Haftbombe", 4f);
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }
}
