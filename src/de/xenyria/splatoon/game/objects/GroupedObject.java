package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import org.bukkit.Location;

import java.util.ArrayList;

public interface GroupedObject {

    ArrayList<HitableEntity> allObjects();
    default HitableEntity getNearestObject(HitableEntity initial, Location location) {

        ArrayList<HitableEntity> objects = allObjects();

        HitableEntity lowest = initial;
        double dist = initial.getLocation().toVector().distance(location.toVector());
        for(HitableEntity entity : objects) {

            double newDist = entity.getLocation().distance(location);
            if(newDist < dist) {

                lowest = entity;
                dist = newDist;

            }

        }

        return lowest;

    }
    public GameObject getRoot();

}
