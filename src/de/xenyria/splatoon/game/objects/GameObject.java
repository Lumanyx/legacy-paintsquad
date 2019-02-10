package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.match.Match;

public abstract class GameObject {

    private int id = Match.nextObjectID();
    public int getID() { return id; }

    public abstract ObjectType getObjectType();
    public abstract void onTick();
    public abstract void reset();

    private Match match;
    public Match getMatch() { return match; }

    public GameObject(Match match) {

        this.match = match;

    }

}
