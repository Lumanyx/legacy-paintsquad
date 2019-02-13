package de.xenyria.splatoon.game.match.turfwar;

import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.gui.StaticItems;
import de.xenyria.splatoon.game.match.BattleMatch;
import de.xenyria.splatoon.game.match.MatchControlInterface;
import de.xenyria.splatoon.game.match.MatchType;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.team.Team;

import java.util.ArrayList;
import java.util.HashMap;

public class TurfWarMatch extends BattleMatch {

    public TurfWarMatch() {



    }

    @Override
    public MatchType getMatchType() {
        return MatchType.TURF_WAR;
    }
}
