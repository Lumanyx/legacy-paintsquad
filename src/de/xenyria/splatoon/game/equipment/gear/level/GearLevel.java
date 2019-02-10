package de.xenyria.splatoon.game.equipment.gear.level;

public class GearLevel {

    private int id;
    public int getID() { return id; }

    private int start;
    public int getStart() { return start; }

    private int experience;
    public int getExperience() { return experience; }

    public GearLevel(int id, int start, int experience) {

        this.id = id;
        this.start = start;
        this.experience = experience;

    }

}
