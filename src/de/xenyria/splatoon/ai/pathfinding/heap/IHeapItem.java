package de.xenyria.splatoon.ai.pathfinding.heap;

public interface IHeapItem extends Comparable {

    int getIndex();
    void setIndex(int i);

}
