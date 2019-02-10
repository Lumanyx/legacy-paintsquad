package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSplatBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractSuctionBomb;

public class DebugSuctionBomb extends AbstractSuctionBomb {

    public DebugSuctionBomb() {
        super(12, "Protohaftbombe", 4f);
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }
}
