package de.xenyria.splatoon.ai.navigation.target;

import org.bukkit.Location;

public class NavigationTarget {

    public static enum TargetType {

        BLOCK;

    }

    private Location location;
    public NavigationTarget(Location location) {

        this.location = location;

    }

}
