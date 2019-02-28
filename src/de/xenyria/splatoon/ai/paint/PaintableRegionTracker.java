package de.xenyria.splatoon.ai.paint;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.task.paint.PaintableRegion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaintableRegionTracker {

    private ConcurrentHashMap<PaintableRegion, Integer> visitCounts = new ConcurrentHashMap<>();
    public ConcurrentHashMap<PaintableRegion, Integer> getVisitCounts() { return visitCounts; }

    private EntityNPC npc;
    public PaintableRegionTracker(EntityNPC npc) {

        this.npc = npc;

    }

    private int ticker = 0;
    public void tick() {

        ticker++;
        if(ticker > 80) {

            Iterator<Map.Entry<PaintableRegion, Integer>> iterator = visitCounts.entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<PaintableRegion, Integer> entry = iterator.next();
                if(entry.getKey().getCenter().distance(npc.getLocation().toVector()) >= 25D) {

                    int newCount = entry.getValue() - 1;
                    if (newCount < 1) {

                        iterator.remove();

                    } else {

                        entry.setValue(newCount);

                    }

                }

            }

        }

    }

    public int getVisitCounts(PaintableRegion region) {

        return visitCounts.getOrDefault(region, 0);

    }

    public int getVisitCounts(PaintableRegion.Coordinate coordinate) {

        for(PaintableRegion region : visitCounts.keySet()) {

            if(region.getCoordinate().equals(coordinate)) {

                return visitCounts.get(region);

            }

        }
        return 0;

    }

    public boolean hasRegion(PaintableRegion region) {

        return visitCounts.containsKey(region);

    }
    public boolean hasCoordinate(PaintableRegion.Coordinate coordinate) {

        for(PaintableRegion region : visitCounts.keySet()) {

            if(region.getCoordinate().equals(coordinate)) {

                return true;

            }

        }
        return false;

    }

    public void reset() {

        this.visitCounts.clear();

    }

    public void addVisitCount(PaintableRegion.Coordinate coordinate1) {

        PaintableRegion region = npc.getMatch().getAIController().getPaintableRegion(coordinate1);
        if(region != null) {

            if (!visitCounts.containsKey(region)) {

                visitCounts.put(region, 1);

            } else {

                visitCounts.put(region, visitCounts.get(region) + 1);

            }

        }

    }

}
