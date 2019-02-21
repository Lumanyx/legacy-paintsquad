package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.AbstractCurlingBomb;
import org.bukkit.Location;

public class CurlingBomb extends AbstractCurlingBomb {

    public static final int ID = 5;
    public static final float RADIUS = 4.5f;
    public static final float MAX_DAMAGE = 120f;

    public CurlingBomb() {
        super(ID, "Curlingbombe", RADIUS);
    }

    @Override
    public Brand getBrand() {
        return Brand.NOT_BRANDED;
    }

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

    }

    @Override
    public double getImpulse() {
        return 0;
    }
}
