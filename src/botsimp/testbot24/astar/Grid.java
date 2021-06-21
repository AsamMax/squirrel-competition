package botsimp.testbot24.astar;

import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;

public class Grid {
    public final boolean DIAGONAL_MOVE = true;
    public final boolean CHECK_DIAGONAL = true;

    public double nodeRadius;
    Node[][] grid;

    double nodeDiameter;
    int gridSizeX, gridSizeY;
    public final int maxSize;

    public Grid(int[][] board) {
        nodeDiameter = nodeRadius * 2;
        gridSizeX = board.length;
        gridSizeY = board[0].length;
        maxSize = gridSizeX * gridSizeY;
        CreateGrid(board);
    }

    void CreateGrid(int[][] board) {
        grid = new Node[gridSizeX][gridSizeY];

        for (int x = 0; x < gridSizeX; x++) {
            for (int y = 0; y < gridSizeY; y++) {
                boolean walkable = board[x][y] != -1;
                int movementPenalty = 0;
                if(walkable)
                    movementPenalty = board[x][y];
                grid[x][y] = new Node(walkable, x, y, movementPenalty);
            }
        }
    }

    public ArrayList<Node> getNeighbours(Node node) {
        ArrayList<Node> neighbours = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0)
                    continue;
                int _x = Math.abs(x);
                int _y = Math.abs(y);
                if (!DIAGONAL_MOVE) {
                    if (_x == _y)
                        continue;
                }

                int checkX = node.gridX + x;
                int checkY = node.gridY + y;

                if (checkX >= 0 && checkX < gridSizeX && checkY >= 0 && checkY < gridSizeY) {
                    if (CHECK_DIAGONAL && _x == _y) {
                        if (!grid[checkX][checkY - y].walkable || !grid[checkX - x][checkY].walkable)
                            continue;
                    }
                    neighbours.add(grid[checkX][checkY]);
                }
            }
        }

        return neighbours;
    }

    public Node getNodeAt(int x, int y){
        return grid[x][y];
    }
    public Node getNodeAt(XY xy){
        return getNodeAt(xy.x, xy.y);
    }
}
