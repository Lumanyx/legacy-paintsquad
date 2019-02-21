package de.xenyria.splatoon.game.team;

import de.xenyria.splatoon.game.color.Color;

public class Team {

    private Color color;
    public Color getColor() { return color; }

    public Team(int id, Color color) {

        this.id = (byte) id;
        this.color = color;

    }

    private byte id;
    public byte getID() { return id; }
}
