package botsimp.testbot24;

import botsimp.testbot24.Util24.DefaultDict;
import botsimp.testbot24.astar.Grid;
import botsimp.testbot24.astar.Node;
import botsimp.testbot24.astar.Pathfinding;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
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

public class LookingMasterBotController implements BotController {
    //    private boolean previouslyShotAtMaster = false; // vllt erst dann wieder auf true wenn kein master in einem bestimmten bereich ist

    private final DefaultDict<EntityType, List<XY>> allUnits = new DefaultDict<>(ArrayList.class);
    private XY assumedSize = new XY(0, 0);
    private XY me = null;
    private ControllerContext context;
    private XY ul = null;
    private XY lr = null;

    private Grid g = null;
    private Node start = null;
    private Pathfinding p = null;
    public static final boolean CAN_ESCAPE = false;
    private XY currentTarget;


    @Override
    public void nextStep(ControllerContext contexts) {
        try {
            this.context = contexts;
//            long startTime = System.currentTimeMillis();

            g = null;
            start = null;
            p = null;

            analizeSurroundings();


            XY near = nearestEnemyMini();
            if (near != null && near.distanceFrom(me) < 5) {
                if (spawnMini()) {
                    // System.out.println("Time past with spawn: " + (System.currentTimeMillis() - startTime));
                    return;
                }
            }
            near = nearest(EntityType.MASTER_SQUIRREL);
            if (CAN_ESCAPE && near != null && near.distanceFrom(me) < 4) {
                boolean escaped = tryEscape();

                if (escaped) {
                    return;
                }
            }
            //if (enemyCount > 1 && miniCount < 1 && !masterInRange ) {
            if (allUnits.get(EntityType.MINI_SQUIRREL).size() < 1) {
                if (spawnMini()) {
//                        System.out.println("Time past with spawn: " + (System.currentTimeMillis() - startTime));
                    return;
                }
            }


            context.move(bestMove().minus(me));


//            System.out.println("Time past: " + (System.currentTimeMillis() - startTime));
        } catch (OutOfViewException e) {
            e.printStackTrace();
        }
    }

    private XY bestMove() {
        int minMoves = Integer.MAX_VALUE;
        currentTarget = null;

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
        if (currentTarget == null) {
            currentTarget = me.plus(moveSave());
        }
        return currentTarget;
    }

    private int findPath(int minMoves, XY plant) {
        int direct = directSteps(me, plant);
        // The Lists are ordered by steps
        if (direct >= minMoves) {
            return -1;
        }
        XY nextStep = getDirectPath(plant);
        if (nextStep != null) {
            currentTarget = nextStep;
            minMoves = direct;

            System.out.println("plant straight line");
            return minMoves;
        }

        ensureGrid();
        Node targetNode = g.getNodeAt(plant.minus(ul));
        ArrayList<Node> path = p.findPath(start, targetNode);

        if (path.size() >= 2) {
            Node next = path.get(1);
            if (targetNode.getMoveCount() < minMoves) {
                currentTarget = new XY(next.gridX - start.gridX, next.gridY - start.gridY).plus(me);
                minMoves = targetNode.getMoveCount();
                System.out.println("plant astar");
                System.out.println(path);
            }
        }
        // No Path found
        return minMoves;
    }

    private XY getDirectPath(XY targetLocation) {
        int steps = directSteps(me, targetLocation);
        // Diagonal first
        int x, y;

        if (checkDiagonal(targetLocation, steps)) {
            x = me.x + Integer.signum(targetLocation.x - me.x);
            y = me.y + Integer.signum(targetLocation.y - me.y);
            return new XY(x, y);
        }
        if (checkStraight(targetLocation, steps)) {

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

    private boolean checkDiagonal(XY targetLocation, int steps) {
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

    private boolean checkStraight(XY targetLocation, int steps) {
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

    private void ensureGrid() {
        if (g == null || start == null || p == null) {
            g = new Grid(generateBoard());
            start = g.getNodeAt(me.minus(context.getViewUpperLeft()));
            p = new Pathfinding(g);
        }

    }


    private XY nearestEnemyMini() {
        List<XY> l = allUnits.get(EntityType.MINI_SQUIRREL);
        for (XY loc : l) {
            if (!context.isMine(loc)) {
                return loc;
            }
        }
        // Always far enough away. Avoids Null-Errors
        return null;
    }

    private XY nearest(EntityType entityType) {
        List<XY> l = allUnits.get(entityType);
        if (!l.isEmpty()) {
            return l.get(0);
        }
        // Always far enough away. Avoids Null-Errors
        return null;
    }

    private int[][] generateBoard() {
        int[][] board = new int[lr.x - ul.x + 2][lr.y - ul.y + 2];
        for (XY loc : allUnits.get(EntityType.BAD_PLANT)) {
            loc = loc.minus(ul);
            board[loc.x][loc.y] = 100;
        }
        for (XY loc : allUnits.get(EntityType.BAD_BEAST)) {
            loc = loc.minus(ul);

            for (int x = loc.x - 1; x <= loc.x + 1; x++) {
                if (x < 0 || x > lr.x) {
                    continue; // Skip if out of bounds
                }
                for (int y = loc.y - 1; y <= loc.y + 1; y++) {
                    if (y < 0 || y > lr.y) {
                        continue;  // Skip if out of bounds
                    }
                    board[x][y] = 200;
                }
            }
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
            if (!context.isMine(loc)) {
                loc = loc.minus(ul);
                board[loc.x][loc.y] = -1;
            }
        }
        return board;
    }

    private void analizeSurroundings() {

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

    private void detect(int x, int y) {
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

    private XY moveSave() {
        XY me = context.locate();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                XY loc = new XY(me.x + x, me.y + y);
                EntityType type = context.getEntityAt(loc);
                if (type == EntityType.NONE) {
//                    System.out.println("Save move to " + new XY(x, y));
                    return new XY(x, y);
                }
            }
        }
        return me;
    }

    private boolean spawnMini1(ControllerContext context) {
        if (context.getEnergy() > 200) {
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    XY loc = new XY(me.x + x, me.y + y);
                    EntityType type = context.getEntityAt(loc);
                    if (type == EntityType.NONE) {
                        context.spawnMiniBot(new XY(x, y), 100);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean spawnMini() {
        if (context.getEnergy() > 200) {
            for (int i = 0; i < 10; i++) {
                try {
                    // XY dir = XY.randomDirection();
                    XY dir = closestEnemyDir();
                    XY loc = new XY(me.x + dir.x, me.y + dir.y);
                    EntityType type = context.getEntityAt(loc);
                    if (type == EntityType.NONE) {
                        context.spawnMiniBot(dir, 100);
                        System.out.println(allUnits.get(EntityType.MINI_SQUIRREL).size());
                        System.out.println("spaned mini " + context.getEnergy());
                        return true;
                    }
                } catch (OutOfViewException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private XY closestEnemyDir() {
        XY closestLoc = closestEnemy();

        if (closestLoc != null) {
            return closestLoc.minus(me).toDirection();
        } else {
            return XY.randomDirection();
        }
    }

    private XY closestEnemy() {
        List<XY> places = Arrays.asList(
                nearest(EntityType.MASTER_SQUIRREL),
                nearestEnemyMini(),
                nearest(EntityType.BAD_BEAST)
        );

        places.sort(Comparator.comparingDouble((XY loc) -> {
            if (loc == null) {
                return Double.POSITIVE_INFINITY;
            }
            return loc.distanceFrom(me);
        }));

        return places.get(0);
    }

    private boolean tryEscape1(ControllerContext context, XY me, ArrayList<XY> enemyMasterPos, XY ul, Grid g, Node start, Pathfinding p) {
        ArrayList<Node> path;
        XY dir = enemyMasterPos.stream().reduce(XY.ZERO_ZERO, XY::plus).minus(me).toDirection().times(-1);
        if (dir.equals(XY.ZERO_ZERO)) {
            System.out.println("Cant escape1");
            return false;
        }
        do {
            Node target;
            try {
                target = g.getNodeAt(me.minus(ul).plus(dir));
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Cant escape2");
                return false;
            }
            path = p.findPath(start, target);
//                    System.out.println(path.size());
            XY add = dir.toDirection();
            dir = dir.plus(add);
        } while (path.size() < 2);
        context.move(new XY(path.get(1).gridX - start.gridX, path.get(1).gridY - start.gridY));
        System.out.println("Try escape");
        return true;
    }

    private boolean tryEscape() {
        ensureGrid();
        ArrayList<Node> path;

        Node target;
        try {
            XY center = getCenter();
            target = g.getNodeAt(center.x - ul.x, center.y - ul.y);

            // XY dir = allUnits.get(EntityType.MASTER_SQUIRREL).stream().reduce(XY.ZERO_ZERO, XY::plus).minus(me).toDirection().times(-1);
            // target = g.getNodeAt(me.x - ul.x + dir.x, me.y - ul.y + dir.y);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Cant escape2");
            return false;
        }
        path = p.findPath(start, target);

        if (path.size() < 2) {
            System.out.println("Cant escape3");
            return false;
        }
        context.move(new XY(path.get(1).gridX - start.gridX, path.get(1).gridY - start.gridY));
        System.out.println("Try escape");
        return true;
    }

    private XY getCenter() {
        return new XY((int) (assumedSize.x / 2f), (int) (assumedSize.y / 2f));
    }

//    private int realDistanceTo(XY start, XY t){
//        Node target = g.getNodeAt(enemyMasterPos.x - ul.x + dir.x, enemyMasterPos.y - ul.y + dir.y);
//        path = p.findPath(start, target);
//    }
}
