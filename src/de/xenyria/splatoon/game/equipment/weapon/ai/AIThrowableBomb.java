package de.xenyria.splatoon.game.equipment.weapon.ai;

import de.xenyria.math.trajectory.Trajectory;
import org.bukkit.Location;

public interface AIThrowableBomb {

    void throwBomb(Location target, Trajectory trajectory);
    double getImpulse();

}
