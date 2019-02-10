package de.xenyria.splatoon.ai.pathfinding.heap;

public class Heap {

    private IHeapItem[] items;
    private int currentItemCount;

    public Heap(int maxHeapSize) {

        //this.currentItemCount = maxHeapSize;
        this.items = new IHeapItem[maxHeapSize];

    }

    public void add(IHeapItem item) {

        item.setIndex(currentItemCount);
        items[currentItemCount] = item;
        sortUp(item);
        currentItemCount++;

    }

    public IHeapItem removeFirst() {

        IHeapItem firstItem = items[0];
        currentItemCount--;
        items[0] = items[currentItemCount];
        items[0].setIndex(0);
        sortDown(items[0]);
        return firstItem;

    }

    public void updateItem(IHeapItem item) {

        sortUp(item);

    }

    public int count() { return currentItemCount; }
    public boolean contains(IHeapItem item) { return items[item.getIndex()].equals(item); }
    public void sortDown(IHeapItem item) {

        while (true) {

            int childIndexLeft = item.getIndex() * 2 + 1;
            int childIndexRight = item.getIndex() * 2 + 2;
            int swapIndex = 0;

            if (childIndexLeft < currentItemCount) {
                swapIndex = childIndexLeft;

                if (childIndexRight < currentItemCount) {
                    if (items[childIndexLeft].compareTo(items[childIndexRight]) < 0) {
                        swapIndex = childIndexRight;
                    }
                }

                if (item.compareTo(items[swapIndex]) < 0) {

                    swap(item,items[swapIndex]);

                } else {

                    return;

                }

            } else { return; }

        }

    }

    public void sortUp(IHeapItem item) {

        int parentIndex = (item.getIndex() - 1) / 2;
        while (true) {

            IHeapItem item1 = items[parentIndex];
            if(item.compareTo(item1) > 0) {

                swap(item, item1);

            } else {

                break;

            }

            parentIndex = (item.getIndex() - 1) / 2;

        }

    }

    public void swap(IHeapItem itemA, IHeapItem itemB) {

        items[itemA.getIndex()] = itemB;
        items[itemB.getIndex()] = itemA;
        int itemAIndex = itemA.getIndex();
        itemA.setIndex(itemB.getIndex());
        itemB.setIndex(itemAIndex);

    }

    public int size() { return items.length; }

    public boolean isEmpty() { return currentItemCount == 0; }

    public IHeapItem[] array() { return items; }
}
