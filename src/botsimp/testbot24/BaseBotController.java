package botsimp.testbot24;

import botsimp.testbot24.astar.Grid;
import botsimp.testbot24.astar.Node;
import botsimp.testbot24.astar.Pathfinding;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static botsimp.testbot24.Util24.directSteps;
import static botsimp.testbot24.Util24.isBetween;

public abstract class BaseBotController implements BotController {
    protected final Util24.DefaultDict<EntityType, List<XY>> allUnits = new Util24.DefaultDict<>(ArrayList.class);

    protected ControllerContext context;

    protected XY me = null;

    protected XY ul = null;
    protected XY lr = null;

    protected XY assumedSize = new XY(0, 0);

    protected Grid g = null;
    protected Node start = null;
    protected Pathfinding p = null;

    protected boolean avoidConforontation = true;
    protected XY currentTarget;

    @Override
    public void nextStep(ControllerContext context){

        this.context = context;
//            long startTime = System.currentTimeMillis();

        g = null;
        start = null;
        p = null;

        analizeSurroundings();

        _nextStep();

//            System.out.println("Time past: " + (System.currentTimeMillis() - startTime));

    }

    protected abstract void _nextStep();

    protected void ensureGrid() {
        if (g == null || start == null || p == null) {
            g = new Grid(generateBoard());
            start = g.getNodeAt(me.minus(context.getViewUpperLeft()));
            p = new Pathfinding(g);
        }

    }

    protected XY nearestEnemyMini() {
        List<XY> l = allUnits.get(EntityType.MINI_SQUIRREL);
        for (XY loc : l) {
            if (!context.isMine(loc)) {
                return loc;
            }
        }
        // Always far enough away. Avoids Null-Errors
        return null;
    }

    protected XY nearest(EntityType entityType) {
        List<XY> l = allUnits.get(entityType);
        if (!l.isEmpty()) {
            return l.get(0);
        }
        // Always far enough away. Avoids Null-Errors
        return null;
    }

    protected int[][] generateBoard() {
        int[][] board = new int[lr.x - ul.x + 2][lr.y - ul.y + 2];
        for (XY loc : allUnits.get(EntityType.BAD_PLANT)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = 100;
        }
        for (XY loc : allUnits.get(EntityType.BAD_BEAST)) {
            loc = loc.minus(ul);

            if (avoidConforontation){
                for (int x = loc.x - 1; x <= loc.x + 1; x++) {
                    if (x < 0 || x >= board.length) {
                        continue; // Skip if out of bounds
                    }
                    for (int y = loc.y - 1; y <= loc.y + 1; y++) {
                        if (y < 0 || y >= board.length) {
                            continue;  // Skip if out of bounds
                        }
                        board[x][y] = 200;
                    }
                }
            }

            board[loc.x][loc.y] = 800;
        }
        for (XY loc : allUnits.get(EntityType.WALL)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = -1;
        }
        for (XY loc : allUnits.get(EntityType.MASTER_SQUIRREL)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = -1;
        }
        for (XY loc : allUnits.get(EntityType.MINI_SQUIRREL)) {

            if (avoidConforontation) {
                if (!context.isMine(loc)) {
                    loc = loc.minus(ul);
                    board[loc.x][loc.y] = -1;
                }
            }else{
                if (context.isMine(loc)) {
                    loc = loc.minus(ul);
                    board[loc.x][loc.y] = -1;
                }
            }
        }
        return board;
    }

    protected void analizeSurroundings() {

        me = context.locate();
        ul = context.getViewUpperLeft();
        lr = context.getViewLowerRight();

        allUnits.clear();
        assumedSize = Util24.checkLowestRightest(assumedSize, lr);

        int maxDistance = (lr.x - ul.x) / 2;

        for (int r = 1; r <= maxDistance; r++) {
            for (int x = me.x - r; x <= me.x + r; x++) {
                detect(x, me.y + r);
                detect(x, me.y - r);
            }
            for (int y = me.y - r + 1; y < me.y + r; y++) {
                detect(me.x + r, y);
                detect(me.x - r, y);
            }
        }
    }

    protected void detect(int x, int y) {
        XY loc = new XY(x, y);
        if (!isBetween(loc, ul, lr)) {
            return;
        }
        EntityType type = context.getEntityAt(loc);
        if (loc.equals(me) || type == EntityType.NONE) {
            return;
        }
        allUnits.get(type).add(loc);
    }

    protected int findPath(int maxMoves, XY plant) {
        int direct = directSteps(me, plant);
        // The Lists are ordered by steps
        if (direct >= maxMoves) {
            return -1;
        }
        XY nextStep = getDirectPath(plant);
        if (nextStep != null) {
            currentTarget = nextStep;
            return direct;
        }

        ensureGrid();
        Node targetNode = g.getNodeAt(plant.minus(ul));
        ArrayList<Node> path = p.findPath(start, targetNode);

        if (path.size() >= 2) {
            Node next = path.get(1);
            if (targetNode.getMoveCount() < maxMoves) {
                currentTarget = new XY(next.gridX - start.gridX, next.gridY - start.gridY).plus(me);
                maxMoves = targetNode.getMoveCount();
            }
        }
        // No Path found
        return maxMoves;
    }

    private XY getDirectPath(XY targetLocation) {
        int steps = directSteps(me, targetLocation);
        // Diagonal first
        int x, y;

        if (checkDiagonalFirst(targetLocation, steps)) {
            x = me.x + Integer.signum(targetLocation.x - me.x);
            y = me.y + Integer.signum(targetLocation.y - me.y);
            return new XY(x, y);
        }
        if (checkStraightFirst(targetLocation, steps)) {

            int deltax = targetLocation.x - me.x;
            int deltay = targetLocation.y - me.y;
            if (Math.abs(deltax) == Math.abs(deltay)) {
                x = me.x + Integer.signum(targetLocation.x - me.x);
                y = me.y + Integer.signum(targetLocation.y - me.y);
            } else if (Math.abs(deltax) > Math.abs(deltay)) {
                x = me.x + Integer.signum(targetLocation.x - me.x);
                y = me.y;
            } else {
                x = me.x;
                y = me.y + Integer.signum(targetLocation.y - me.y);
            }
            return new XY(x, y);
        }
        return null;
    }

    private boolean checkDiagonalFirst(XY targetLocation, int steps) {
        int x = me.x;
        int y = me.y;
        for (int i = 0; i <= steps; i++) {
            x += Integer.signum(targetLocation.x - x);
            y += Integer.signum(targetLocation.y - y);
            EntityType e = context.getEntityAt(new XY(x, y));
            if (e == EntityType.BAD_BEAST || e == EntityType.BAD_PLANT || e == EntityType.MINI_SQUIRREL || e == EntityType.WALL || e == EntityType.MASTER_SQUIRREL) {
                return false; // Try something else
            }
        }
        return true;
    }

    private boolean checkStraightFirst(XY targetLocation, int steps) {
        int x = me.x;
        int y = me.y;
        for (int i = 0; i <= steps; i++) {
            int deltax = targetLocation.x - x;
            int deltay = targetLocation.y - y;
            if (Math.abs(deltax) == Math.abs(deltay)) {
                x += Integer.signum(deltax);
                y += Integer.signum(deltay);
            } else if (Math.abs(deltax) > Math.abs(deltay)) {
                x += Integer.signum(deltax);
            } else {
                y += Integer.signum(deltay);
            }
            EntityType e = context.getEntityAt(new XY(x, y));
            if (e == EntityType.BAD_BEAST || e == EntityType.BAD_PLANT || e == EntityType.MINI_SQUIRREL || e == EntityType.WALL || e == EntityType.MASTER_SQUIRREL) {
                return false; // Try something else
            }
        }
        return true;
    }

    protected XY getCenter() {
        return new XY((int) (assumedSize.x / 2f), (int) (assumedSize.y / 2f));
    }

    protected void bestGoodTarget() {
        int minMoves = Integer.MAX_VALUE;
        for (XY plant : allUnits.get(EntityType.GOOD_PLANT)) {
            int length =  findPath(minMoves, plant);
            if (length >= 0){ // negative int for errors or usless checks
                minMoves = length;
            }else {
                break;
            }
        }

        for (XY beast : allUnits.get(EntityType.GOOD_BEAST)) {
            int length =  findPath(minMoves, beast);
            if (length >= 0){ // negative int for errors or usless checks
                minMoves = length;
            }else {
                break;
            }
        }
    }

    protected XY closestEnemy() {
        List<XY> places = Arrays.asList(
                nearest(EntityType.BAD_BEAST),
                nearest(EntityType.MASTER_SQUIRREL),
                nearestEnemyMini()
        );

        places.sort(Comparator.comparingDouble((XY loc) -> {
            if (loc == null) {
                return Double.POSITIVE_INFINITY;
            }
            return loc.distanceFrom(me);
        }));

        return places.get(0);
    }
}
