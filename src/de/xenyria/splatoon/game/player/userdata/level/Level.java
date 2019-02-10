package de.xenyria.splatoon.game.player.userdata.level;

public class Level {

    private int id;
    public int getID() { return id; }

    private int start;
    public int getStart() { return start; }

    private int levelExperience;
    public int getLevelExperience() { return levelExperience; }

    public Level(int id, int start, int levelExperience) {

        this.id = id;
        this.start = start;
        this.levelExperience = levelExperience;

    }

}
