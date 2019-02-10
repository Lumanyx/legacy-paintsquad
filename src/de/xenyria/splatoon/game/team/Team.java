package de.xenyria.splatoon.game.team;

import de.xenyria.splatoon.game.color.Color;

public class Team {

    public static final Team DEBUG_TEAM_1 = new Team(Color.Pink);
    public static final Team DEBUG_TEAM_2 = new Team(Color.GREEN);
    public static final Team DEBUG_TEAM_3 = new Team(Color.Pink);
    public static final Team DEBUG_TEAM_4 = new Team(Color.GREEN);

    private Color color;
    public Color getColor() { return color; }

    public Team(Color color) {

        this.color = color;

    }

}
