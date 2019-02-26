package de.xenyria.splatoon.game.objects.tutorial;

import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;

public class Checkpoint extends GameObject {

    public Checkpoint(Match match, Location location) {

        super(match);
        this.center = location;

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private Location center = null;

    @Override
    public void onTick() {

        for(SplatoonHumanPlayer player : getMatch().getHumanPlayers()) {

            if(player.getLocation().distance(center) <= 2.75) {

                if(player.getSpawnPoint().distance(center) >= 0.01d) {

                    player.setSpawnPoint(center);
                    player.getPlayer().sendMessage(" §8" + Characters.ARROW_RIGHT_FROM_TOP + " §a" + Characters.OKAY + " §7Checkpoint erreicht!");
                    player.getPlayer().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 0.5f, 0.5f);

                }

            }

        }

    }

    @Override
    public void reset() {

    }

    @Override
    public void onRemove() {

    }
}
