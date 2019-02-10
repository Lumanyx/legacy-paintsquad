package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractCurlingBomb;

public class DebugCurlingBomb extends AbstractCurlingBomb {

    public DebugCurlingBomb() {
        super(5, "Protocurlingbombe", 3.5f);
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }
}
