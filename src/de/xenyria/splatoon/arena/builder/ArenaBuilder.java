package de.xenyria.splatoon.arena.builder;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.arena.ArenaProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class ArenaBuilder {

    private LinkedBlockingQueue<ArenaProvider.ArenaGenerationTask> tasks = new LinkedBlockingQueue<>();
    public void queueTask(ArenaProvider.ArenaGenerationTask task) {

        tasks.add(task);

    }

    public ArenaBuilder() {

        XenyriaSplatoon.getXenyriaLogger().log("ArenaBuilder bereit!");
        worker.setName("ArenaBuilder Worker");
        worker.start();

    }

    private Thread worker = new Thread(() -> {

        while (true) {

            try { Thread.sleep(1); } catch (Exception e) { e.printStackTrace(); }
            if(!tasks.isEmpty()) {

                ArenaProvider.ArenaGenerationTask task = tasks.peek();
                task.work();
                if (task.isDone()) {

                    XenyriaSplatoon.getXenyriaLogger().log("ArenaBuilder hat eine Arena eingefügt. Dauerte " + task.elapsedTime() + " ms");
                    tasks.poll();

                }

            }

        }

    });

}
