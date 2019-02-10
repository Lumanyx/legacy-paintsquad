package de.xenyria.splatoon.ai.pathfinding.worker;

import de.xenyria.splatoon.ai.pathfinding.SquidAStar;

import java.util.ArrayList;

public class PathfindingManager {

    private PathfindingWorker[] availableWorkers = null;
    public PathfindingWorker getFreeWorker() {

        PathfindingWorker last = null;
        int lowest = 0;

        for(int i = 0; i < availableWorkers.length; i++) {

            int newVal = availableWorkers[i].requestCount();
            if(last == null || newVal < lowest) {

                last = availableWorkers[i];
                lowest = newVal;

            }

        }
        return last;

    }

    private static PathfindingManager instance;
    public static PathfindingManager getInstance() { return instance; }

    public static void queueRequest(SquidAStar aStar) {

        instance.getFreeWorker().addRequest(aStar);

    }

    public PathfindingManager() {

        int workerAmount = 4;
        availableWorkers = new PathfindingWorker[workerAmount];
        instance = this;

        for(int i = 0; i < workerAmount; i++) {

            availableWorkers[i] = new PathfindingWorker(i + 1);

        }

    }

}
