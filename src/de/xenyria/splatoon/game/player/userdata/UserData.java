package de.xenyria.splatoon.game.player.userdata;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.InventoryItem;
import de.xenyria.splatoon.game.player.userdata.level.Level;
import de.xenyria.splatoon.lobby.SplatoonLobby;

public class UserData {

    private SplatoonHumanPlayer player;
    public UserData(SplatoonHumanPlayer player) {

        this.player = player;

    }

    private int coins;
    public int getCoins() { return coins; }
    public void updateCoins(int coins) {

        this.coins = coins;

        if(player.getMatch() instanceof SplatoonLobby) {

            ((SplatoonLobby)player.getMatch()).updateScoreboard(player.getPlayer());

        }

    }

    private int experience;
    public int getExperience() { return experience; }
    public void updateExperience(int exp) {

        this.experience = exp;

        if(player.getMatch() instanceof SplatoonLobby) {

            ((SplatoonLobby)player.getMatch()).updateScoreboard(player.getPlayer());

        }

    }

    public int currentLevel() {

        return XenyriaSplatoon.getLevelTree().fromExperience(experience).getID();

    }
    public Level targetLevel() {

        Level current = XenyriaSplatoon.getLevelTree().fromExperience(experience);
        int end = current.getStart() + current.getLevelExperience() + 1;

        return XenyriaSplatoon.getLevelTree().fromExperience(end);

    }

    public double currentLevelPercentage() {

        Level level = XenyriaSplatoon.getLevelTree().fromExperience(experience);
        int start = level.getStart();
        int end = level.getStart() + level.getLevelExperience();
        int current = experience;
        int relStart = 0;
        int relEnd = end - start;
        int relExp = current - start;

        double result = ((double)relExp / (double)relEnd) * 100d;
        if(result <= 0) { result = 0d; } else if(result >= 100d) { result = 100d; }
        return result;

    }


    public void subtractCoins(int cost) {

        updateCoins(getCoins()-cost);

    }

}
