package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;

public class Hologram extends GameObject {

    private Location location;
    private double radius;
    private ArrayList<String> strings = new ArrayList<>();
    private ArrayList<ArmorStand> stands = new ArrayList<>();
    public Hologram(Match match, Location location, double activationRadius, String... lines) {

        super(match);
        this.location = location;
        this.radius = activationRadius;
        Location cursor = location.clone().add(0, (lines.length * .25) / 2f, 0);
        for(String s : lines) {

            strings.add(s);
            if(s.isEmpty()) { s = "§0"; }
            ArmorStand stand = (ArmorStand) match.getWorld().spawnEntity(cursor, EntityType.ARMOR_STAND);
            cursor = cursor.add(0, -.25, 0);
            stand.setCanMove(false);
            stand.setCanTick(false);
            stand.setVisible(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(false);
            stand.setCustomName(s);
            stands.add(stand);

        }

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    @Override
    public void onTick() {

        boolean visible = false;
        if(radius == -1) {

            visible = true;

        } else {

            for(SplatoonPlayer player : getMatch().getAllPlayers()) {

                if(player.getLocation().distance(location) <= radius) {

                    visible = true;
                    break;

                }

            }

        }
        if(visible) {

            for(ArmorStand stand : stands) {

                if(!stand.isCustomNameVisible()) {

                    stand.setCustomNameVisible(true);

                }

            }

        } else {

            for(ArmorStand stand : stands) {

                if(stand.isCustomNameVisible()) {

                    stand.setCustomNameVisible(false);

                }

            }

        }

    }

    @Override
    public void reset() {

    }

}
