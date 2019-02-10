package de.xenyria.splatoon.game.equipment.gear.level;

import java.util.ArrayList;
import java.util.HashMap;

public class GearLevelManager {

    private static HashMap<Integer, ArrayList<GearLevel>> levels = new HashMap<Integer, ArrayList<GearLevel>>();
    static {

        addLevel(1, new GearLevel(1, 0, 999));
        addLevel(1, new GearLevel(2, 1000, 1999));
        addLevel(1, new GearLevel(3, 2000, 2999));
        addLevel(2, new GearLevel(1, 0, 1999));
        addLevel(2, new GearLevel(2, 2000, 3999));
        addLevel(2, new GearLevel(3, 4000, 5999));
        addLevel(3, new GearLevel(1, 0, 2999));
        addLevel(3, new GearLevel(2, 3000, 5999));
        addLevel(3, new GearLevel(3, 6000, 8999));

    }

    public static ArrayList<GearLevel> getLevels(int maxSubs) {

        ArrayList<GearLevel> clone = (ArrayList<GearLevel>) levels.getOrDefault(maxSubs, new ArrayList<>()).clone();
        while (clone.size() != maxSubs) {

            clone.remove(clone.size() - 1);

        }
        return clone;

    }
    public static GearLevel getCurrentLevel(int stars, int experience) {

        ArrayList<GearLevel> levels = getLevels(stars);
        for(GearLevel level : levels) {

            if(experience >= level.getStart() && experience <= (level.getStart()+level.getExperience())) {

                return level;

            }

        }
        return levels.get(0);

    }
    public static GearLevel getNextLevel(int stars, int experience) {

        GearLevel level = getCurrentLevel(stars, experience);
        ArrayList<GearLevel> levels = getLevels(stars);
        int index = levels.indexOf(level)+1;

        int size = levels.size() - 1;
        if(index > size || (index + 1) > stars) {

            return null;

        } else {

            return levels.get(index);

        }

    }

    private static void addLevel(int starCount, GearLevel level) {

        if(!levels.containsKey(starCount)) {

            ArrayList<GearLevel> levels1 = new ArrayList<>();
            levels1.add(level);
            levels.put(starCount, levels1);

        } else {

            levels.get(starCount).add(level);

        }

    }

}
