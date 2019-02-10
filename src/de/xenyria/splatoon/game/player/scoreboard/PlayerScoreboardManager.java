package de.xenyria.splatoon.game.player.scoreboard;

import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;

public class PlayerScoreboardManager {

    private int pointValue = 0;
    public int getPointValue() { return pointValue; }

    private SplatoonHumanPlayer player;
    public PlayerScoreboardManager(SplatoonHumanPlayer player) {

        this.player = player;

    }

    private int incrementTicker = 0;
    public void tick() {

        if(pointValue < player.getPoints()) {

            pointValue++;

        } else if(pointValue > player.getPoints()) {

            pointValue = player.getPoints();

        }

    }

}
