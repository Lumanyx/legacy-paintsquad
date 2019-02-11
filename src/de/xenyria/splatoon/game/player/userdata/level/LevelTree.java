package de.xenyria.splatoon.game.player.userdata.level;

import de.xenyria.splatoon.XenyriaSplatoon;

import java.util.ArrayList;

public class LevelTree {

    public LevelTree() {

        addLevel(1000);
        addLevel(2000);
        addLevel(3000);
        addLevel(4000);
        addLevel(5000);
        addLevel(6000);
        XenyriaSplatoon.getXenyriaLogger().log("Levelbaum initialisiert - §e" + levels.size() + " Stufe(n)");

    }

    public int maxExperience() {

        Level level = levels.get(levels.size() - 1);
        return level.getStart() + level.getLevelExperience();

    }

    public Level fromExperience(int xp) {

        for(Level level : levels) {

            if(xp >= level.getStart() && xp <= (level.getStart() + level.getLevelExperience())) {

                return level;

            }

        }
        return levels.get(levels.size() - 1);

    }

    private ArrayList<Level> levels = new ArrayList<>();

    public void addLevel(int duration) {

        Level prev = null;
        int start = 0;
        if(!levels.isEmpty()) {

            prev = levels.get(levels.size() - 1);
            start = prev.getStart() + prev.getLevelExperience() + 1;

        }

        Level level = new Level(levels.size() + 1, start, duration);
        levels.add(level);

    }

    public Level lastLevel() { return levels.get(levels.size() - 1); }

    public Level[] allLevels() { return levels.toArray(new Level[]{}); }
}
