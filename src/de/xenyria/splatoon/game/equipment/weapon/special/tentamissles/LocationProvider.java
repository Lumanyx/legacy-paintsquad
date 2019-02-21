package de.xenyria.splatoon.game.equipment.weapon.special.tentamissles;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface LocationProvider {

    Location getLocation();
    Vector getLastDelta();

}
