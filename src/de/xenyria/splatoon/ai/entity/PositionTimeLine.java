package de.xenyria.splatoon.ai.entity;

import org.bukkit.Location;

import java.util.ArrayList;

public class PositionTimeLine {

    public static final int TICKS_PER_TIMESTAMP = 50;

    private ArrayList<Location> locations = new ArrayList<Location>();
    private EntityNPC npc;

    public PositionTimeLine(EntityNPC npc) {

        this.npc = npc;

    }

    public ArrayList<Location> last(int amount) {

        ArrayList<Location> locs = new ArrayList<>();
        if(!locations.isEmpty()) {

            if(locations.size() <= amount) {

                return locs;

            } else {

                for(int x = (locations.size() - 1); x >= ((locations.size() - 1) - amount); x--) {

                    locs.add(locations.get(x));

                }
                return locs;

            }

        } else {

            return locs;

        }

    }

    private int ticker = 0;
    public void tick() {

        ticker++;
        if(ticker > TICKS_PER_TIMESTAMP) {

            locations.add(npc.getLocation().clone());
            ticker = 0;

        }

    }

    public void reset() {

        locations.clear();

    }

}
