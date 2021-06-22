package botsimp.testbot24;

import botsimp.testbot24.astar.Grid;
import botsimp.testbot24.astar.Node;
import botsimp.testbot24.astar.Pathfinding;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.core.entities.MasterSquirrelBot;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class BaseBotController implements BotController {
    protected BotControllerFactoryImpl factory;

    protected final DefaultDict<EntityType, List<XY>> allUnits = new DefaultDict<>(ArrayList.class);

    protected ControllerContext context;

    protected XY me = null;

    protected XY ul = null;
    protected XY lr = null;

    protected static XY assumedSize = new XY(0, 0);

    protected Grid g = null;
    protected Node start = null;
    protected Pathfinding p = null;

    protected boolean avoidConforontation = true;
    protected XY currentTarget;

    public static  final boolean DEBUG = true;

    public BaseBotController(BotControllerFactoryImpl botControllerFactory) {
        factory = botControllerFactory;
        g = new Grid();
    }

    public static int directSteps(XY loc1, XY loc2) {
        XY vector = loc2.minus(loc1);
        int deltaX = Math.abs(vector.x);
        int deltaY = Math.abs(vector.y);

        return Math.max(deltaX, deltaY);
    }

    public static XY checkLowestRightest(XY lowestRightest, XY lr) {
        int x1 = Math.max(lowestRightest.x, lr.x);
        int y1 = Math.max(lowestRightest.y, lr.y);

        if (x1 != lowestRightest.x || y1 != lowestRightest.y) {
            lowestRightest = new XY(x1, y1);
            if (DEBUG){
                System.out.println("New estimated border: " + lowestRightest);
            }

        }
        return lowestRightest;
    }

    @Override
    public void nextStep(ControllerContext context) {

//            long startTime = System.currentTimeMillis();

        this.context = context;

        start = null;
        p = null;

        try {
            analizeSurroundings();
            context.getRemainingSteps();

            _nextStep();
        }catch (Exception e){
            context.doNothing();
        }

//            System.out.println("Time past: " + (System.currentTimeMillis() - startTime));

    }

    protected abstract void _nextStep();

    protected void ensureGrid() {
        if (start == null || p == null) {
            g.updateGrid(generateBoard(), ul);
            start = g.getNodeAt(me);
            p = new Pathfinding(g);
        }

    }

    protected XY nearestEnemySquirrel(EntityType type) {
        List<XY> l = allUnits.get(type);
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
        for (int[] row : board) {
            Arrays.fill(row, 5);
        }

        for (XY loc : allUnits.get(EntityType.GOOD_BEAST)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = 3;
        }
        for (XY loc : allUnits.get(EntityType.GOOD_PLANT)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = 1;
        }
        for (XY loc : allUnits.get(EntityType.BAD_PLANT)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = 100;
        }
        for (XY loc : allUnits.get(EntityType.BAD_BEAST)) {
            loc = loc.minus(ul);

            if (avoidConforontation) {
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
            board[loc.x][loc.y] = -2;
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
            } else {
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
        XY lowest = checkLowestRightest(assumedSize, lr);
        if (!lowest.equals(assumedSize)){
            assumedSize = lowest;
        }

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
        if (!inSight(loc)) {
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
        if (nextStep != null && direct > 0) {
            currentTarget = nextStep;
            return direct;
        }

        ensureGrid();
        Node targetNode = g.getNodeAt(plant);
        ArrayList<Node> path = p.findPath(start, targetNode);
        if (DEBUG && this instanceof LookingMasterBotController){
            p.printPath(path, g);
            System.out.println(path.size());
            System.out.println(targetNode.getMoveCount());
        }
        if (path.size() > 1) {
            if (targetNode.getMoveCount() < maxMoves) {
                Node next = path.get(1);
                currentTarget = new XY(next.gridX, next.gridY);
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

    protected XY getCorner(int x, int y) {
        return new XY(assumedSize.x * x, assumedSize.y * y);
    }

    protected void bestGoodTarget() {
        int minMoves = Integer.MAX_VALUE;
        for (XY plant : allUnits.get(EntityType.GOOD_PLANT)) {
            int length = findPath(minMoves, plant);
            if (length >= 0) { // negative int for errors or useless checks
                minMoves = length;
            } else {
                break;
            }
        }

        for (XY beast : allUnits.get(EntityType.GOOD_BEAST)) {
            int length = findPath(minMoves, beast);
            if (length >= 0) { // negative int for errors or useless checks
                minMoves = length;
            } else {
                break;
            }
        }
    }

    protected XY closestEnemy() {
        List<XY> places = Arrays.asList(
                nearest(EntityType.BAD_BEAST),
                nearestEnemySquirrel(EntityType.MASTER_SQUIRREL),
                nearestEnemySquirrel(EntityType.MINI_SQUIRREL)
        );

        places.sort(Comparator.comparingDouble((XY loc) -> {
            if (loc == null) {
                return Double.POSITIVE_INFINITY;
            }
            double dist = loc.distanceFrom(me);
            if (dist == 0) {
                return Double.POSITIVE_INFINITY;
            }
            return dist;
        }));

        return places.get(0);
    }

    public boolean inSight(int x, int y) {
        return x >= ul.x && x <= lr.x && y >= ul.y && y <= lr.y;
    }

    public boolean inSight(XY xy) {
        return inSight(xy.x, xy.y);
    }
}
