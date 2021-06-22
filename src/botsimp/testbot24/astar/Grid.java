package botsimp.testbot24.astar;

import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;

public class Grid {

    public final boolean DIAGONAL_MOVE = true;
    public final boolean CHECK_DIAGONAL = false;

    public double nodeRadius;
    public ArrayList<ArrayList<Node>> grid;

    double nodeDiameter;

    public Grid() {
        nodeDiameter = nodeRadius * 2;
        grid = new ArrayList<>();
    }

    public void updateGrid(int[][] board, XY ul) {
        for (ArrayList<Node> row : grid) {
            for (Node node : row) {
                if (node == null) {
                    continue;
                }

                if (node.movementPenalty != -2) {
                    node.parent = null;
                    node.walkable = true;
                    node.movementPenalty = 5;
                }
            }
        }

        for (int xBoard = 0, realX = ul.x; xBoard < board.length; xBoard++, realX++) {
            for (int yBoard = 0, realY = ul.y; yBoard < board[xBoard].length; yBoard++, realY++) {
                boolean walkable = board[xBoard][yBoard] >= 0;
                int movementPenalty = board[xBoard][yBoard];
                grid.get(realX).set(realY, new Node(walkable, realX, realY, movementPenalty));
            }
        }
    }

    public void ensureSize(XY lowestRight) {
        lowestRight = lowestRight.plus(XY.RIGHT_DOWN);
        for (int i = grid.size(); i <= lowestRight.x; i++) {
            grid.add(new ArrayList<>());
        }

        for (int x = 0; x < grid.size(); x++) {
            ArrayList<Node> row = grid.get(x);

            int highest;
            if (x != 0) {
                highest = Math.max(lowestRight.y, grid.get(0).size());
            } else {
                highest = Math.max(lowestRight.y, grid.get(1).size());
            }

            for (int y = row.size(); y <= highest; y++) {
                row.add(new Node(true, x, y, 5));
            }
        }

    }

    public ArrayList<Node> getNeighbours(Node node) {
        ArrayList<Node> neighbours = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                int _x = Math.abs(x);
                int _y = Math.abs(y);
                if (!DIAGONAL_MOVE) {
                    if (_x == _y) {
                        continue;
                    }
                }

                int checkX = node.gridX + x;
                int checkY = node.gridY + y;

                if (checkX >= 0 && checkX < grid.size() && checkY >= 0 && checkY < grid.get(0).size()) {
                    if (CHECK_DIAGONAL && _x == _y) {
                        if (!grid.get(checkX).get(checkY - y).walkable || !grid.get(checkX - x).get(checkY).walkable) {
                            continue;
                        }
                    }
                    Node here = grid.get(checkX).get(checkY);
//                    if(here == null)
//                        here = new Node(true, checkX, checkY, 1);
                    neighbours.add(here);
                }
            }
        }

        return neighbours;
    }

    public int getMaxSize() {
        return grid.size() * grid.get(0).size();
    }

    public Node getNodeAt(int x, int y) {
        return grid.get(x).get(y);
    }

    public Node getNodeAt(XY pos) {
        try {
            return grid.get(pos.x).get(pos.y);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }
}
