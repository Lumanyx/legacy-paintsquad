package de.xenyria.splatoon.ai.pathfinding.worker;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PathfindingWorker {

    private LinkedBlockingQueue<SquidAStar> remainingRequests = new LinkedBlockingQueue<>();
    public int requestCount() { return remainingRequests.size(); }

    private final Object object = new Object();

    public void addRequest(SquidAStar aStar) {

        remainingRequests.add(aStar);
        synchronized (object) {

            try { object.notify(); } catch (Exception e) { e.printStackTrace(); }

        }

    }

    public PathfindingWorker(int id) {

        this.id = id;
        XenyriaSplatoon.getXenyriaLogger().log("§bAI-Pathfinding-Worker #" + id + " §rist bereit!");

        pathfindingThread.setName("Pathfinding Worker #" + id);
        pathfindingThread.start();

    }
    private int id;

    public Thread pathfindingThread = new Thread(() -> {

        while (true) {

            try {

                Thread.sleep(1);

                SquidAStar astar = null;
                while ((astar = remainingRequests.poll()) != null) {

                    astar.beginProcessing();

                }

            } catch (Exception e) {

                XenyriaSplatoon.getXenyriaLogger().error("§bAI-Pathfinding-Worker #" + id + " §rmeldet einen Ausnahmefehler.", e);

            }

        }

    });

}
