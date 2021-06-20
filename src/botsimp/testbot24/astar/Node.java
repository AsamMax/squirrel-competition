package botsimp.testbot24.astar;


import botsimp.testbot24.astar.utility.HeapItem;

public class Node extends HeapItem<Node> {

    public boolean walkable;
    public int gridX;
    public int gridY;
    public int movementPenalty;

    public int gCost;
    public int hCost;
    public Node parent;

    public Node(boolean _walkable, int _gridX, int _gridY, int _penalty) {
        walkable = _walkable;
        gridX = _gridX;
        gridY = _gridY;
        movementPenalty = _penalty;
    }

    public int getfCost() {
        return gCost + hCost;
    }

    public int compareTo(Node nodeToCompare) {
        int compare = Integer.compare(getfCost(), nodeToCompare.getfCost());
        if (compare == 0) {
            compare = Integer.compare(hCost, nodeToCompare.hCost);
        }

        return -compare;
    }

    public int getMoveCount(){
        if(parent == null)
            return 0;
        return 1 + parent.getMoveCount();
    }
}
