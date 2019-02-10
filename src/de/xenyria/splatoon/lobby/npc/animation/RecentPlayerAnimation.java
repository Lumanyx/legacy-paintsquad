package de.xenyria.splatoon.lobby.npc.animation;

import de.xenyria.splatoon.lobby.npc.RecentPlayerNPC;

public abstract class RecentPlayerAnimation {

    private RecentPlayerNPC npc;
    public RecentPlayerNPC getNPC() { return npc; }
    public void assign(RecentPlayerNPC npc) {

        this.npc = npc;

    }

    public abstract void tick();

}