package de.xenyria.splatoon.ai.task;

import de.xenyria.splatoon.ai.entity.EntityNPC;

public abstract class AITask {

    private EntityNPC npc;
    public EntityNPC getNPC() { return npc; }

    private static int lastID;
    public static int nextID() { lastID++; return lastID + 1; }
    public AITask(EntityNPC npc) {

        this.npc = npc;
        id = nextID();

    }

    public boolean isDone() {

        return doneCheck() || skipFlag;

    }

    public abstract TaskType getTaskType();

    public abstract boolean doneCheck();
    public abstract void tick();
    public abstract void onInit();
    public abstract void onExit();

    public void end() {

        skipFlag = true;

    }
    private boolean skipFlag = false;
    public void skip() {

        skipFlag = true;

    }

    private int id;
    public int getID() { return id; }
}
