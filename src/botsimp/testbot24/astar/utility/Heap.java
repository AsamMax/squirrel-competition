package botsimp.testbot24.astar.utility;

import java.lang.reflect.Array;

public class Heap<T extends HeapItem<T>> {
    T[] items;
    int currentItemCount;

    public Heap(Class<T> c, int maxHeapSize) {
        items = (T[]) Array.newInstance(c, maxHeapSize);
    }

    public void add(T item) {
        item.heapIndex = currentItemCount;
        items[currentItemCount] = item;
        sortUp(item);
        currentItemCount++;
    }

    public T removeFirst() {
        T firstItem = items[0];
        currentItemCount--;
        items[0] = items[currentItemCount];
        items[0].heapIndex = 0;
        sortDown(items[0]);
        return firstItem;
    }

    public void updateItem(T item) {
        sortUp(item);
    }

    public int count() {
        return currentItemCount;
    }

    public boolean contains(T item) {
        return item.equals(items[item.heapIndex]);
    }

    void sortDown(T item) {
        while (true) {
            int childIndexLeft = item.heapIndex * 2 + 1;
            int childIndexRight = item.heapIndex * 2 + 2;
            int swapIndex = 0;

            if (childIndexLeft < currentItemCount) {
                swapIndex = childIndexLeft;

                if (childIndexRight < currentItemCount) {
                    if (items[childIndexLeft].compareTo(items[childIndexRight]) < 0) {
                        swapIndex = childIndexRight;
                    }
                }

                if (item.compareTo(items[swapIndex]) < 0) {
                    swap(item, items[swapIndex]);
                } else {
                    return;
                }

            } else {
                return;
            }

        }
    }

    void sortUp(T item) {
        int parentIndex = (item.heapIndex - 1) / 2;

        while (true) {
            T parentItem = items[parentIndex];
            if (item.compareTo(parentItem) > 0) {
                swap(item, parentItem);
            } else {
                break;
            }

            parentIndex = (item.heapIndex - 1) / 2;
        }
    }

    void swap(T itemA, T itemB) {
        items[itemA.heapIndex] = itemB;
        items[itemB.heapIndex] = itemA;
        int itemAIndex = itemA.heapIndex;
        itemA.heapIndex = itemB.heapIndex;
        itemB.heapIndex = itemAIndex;
    }
}
