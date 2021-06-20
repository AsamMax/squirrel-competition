package botsimp.testbot24.astar;


import botsimp.testbot24.astar.utility.Heap;

import java.util.ArrayList;
import java.util.Collections;

public class Pathfinding {

    public static final int DIAGONAL = 10;
    public static final int GERADE = 10;

    Grid grid;

    public Pathfinding(Grid grid) {
        this.grid = grid;
    }

    public ArrayList<Node> findPath(Node start, Node target) {
        Heap<Node> open = new Heap<>(Node.class, grid.maxSize);
        ArrayList<Node> closed = new ArrayList<>();

        open.add(start);

        while (open.count() > 0) {
            Node current = open.removeFirst();
            closed.add(current);

//            if(current.getMoveCount() > MAX_MOVE_COUNT && MAX_MOVE_COUNT != -1)
//                continue;

            if (current == target)
                return retracePath(start, target);

            for (Node neighbour : grid.getNeighbours(current)) {
                if (!neighbour.walkable || closed.contains(neighbour))
                    continue;

                int newMovementCostToNeighbour = current.gCost + distanceTo(current, neighbour) + neighbour.movementPenalty;
                if (newMovementCostToNeighbour < neighbour.gCost || !open.contains(neighbour)) {
                    neighbour.gCost = newMovementCostToNeighbour;
                    neighbour.hCost = distanceTo(neighbour, target);
                    neighbour.parent = current;

                    if (!open.contains(neighbour))
                        open.add(neighbour);
                    else
                        open.updateItem(neighbour);
                }
            }

        }
        return retracePath(start, target.parent = start);
    }

    public ArrayList<Node> retracePath(Node start, Node target) {
        ArrayList<Node> path = new ArrayList<>();
        Node current = target;

        while (current != start) {
            path.add(current);
            current = current.parent;
        }
        path.add(current);
        Collections.reverse(path);

        return path;
    }

    public int distanceTo(Node nodeA, Node nodeB) {
        int dstX = Math.abs(nodeA.gridX - nodeB.gridX);
        int dstY = Math.abs(nodeA.gridY - nodeB.gridY);

        if (dstX > dstY)
            return 14 * dstY + 10 * (dstX - dstY);
        return 14 * dstX + 10 * (dstY - dstX);
    }

    public void printPath(ArrayList<Node> path, Grid g) {
        if (path == null)
            System.out.println("There is no path");

        StringBuilder sb = new StringBuilder();
//        sb.append("\u035F".repeat(g.gridSizeY + 3));
        sb.append("\n");
        for (Node[] nodes : g.grid) {
            sb.append("|");
            for (Node node : nodes) {
                if (path != null) {
                    if (path.get(0) == node || path.get(path.size() - 1) == node)
                        sb.append("X");
                    else if (path.contains(node))
                        //Path
                        sb.append("\u25CA");
                    else if (!node.walkable)
                        //Wall
                        sb.append("\u2588");
                    else
                        sb.append(" ");
                } else {
                    if (!node.walkable)
                        //Wall
                        sb.append("\u2588");
                    else
                        sb.append(" ");
                }
            }
            sb.append("|").append("\n");
        }

//        sb.append("\u035E".repeat(g.gridSizeY + 3));

        System.out.println(sb.toString());
    }

    public static int distanceTo1(Node start, Node end) {
        return distanceTo1(start.gridX, start.gridY, end.gridX, end.gridY);
    }

    public static int distanceTo1(int xStart, int yStart, int xEnd, int yEnd) {
        int distance = 0;

        int xDir = (xEnd - xStart < 0) ? -1 : 1;
        int yDir = (yEnd - yStart < 0) ? -1 : 1;


        while (true) {
            if (xStart == xEnd) {
                xDir = 0;
            }
            if (yStart == yEnd) {
                yDir = 0;
            }
            if (xDir == 0 && yDir == 0)
                return distance;

            xStart += xDir;
            yStart += yDir;
            if (xDir != 0 && yDir != 0) {
                distance += DIAGONAL;
            } else {
                distance += GERADE;
            }
        }
    }
}
