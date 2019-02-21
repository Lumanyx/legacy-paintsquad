package de.xenyria.splatoon.game.match;

import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;

import java.util.ArrayList;

public interface MatchControlInterface {

    ArrayList<JumpPoint> getJumpPoints(Team team);
    void playerAdded(SplatoonPlayer player);
    void playerRemoved(SplatoonPlayer player);
    void objectAdded(GameObject object);
    void objectRemoved(GameObject object);
    void teamAdded(Team team);
    void addGUIItems(SplatoonPlayer player);
    void handleSplat(SplatoonPlayer player, SplatoonPlayer shooter, SplatoonProjectile projectile);
    void teamChanged(SplatoonPlayer splatoonHumanPlayer, Team oldTeam, Team team);

}
