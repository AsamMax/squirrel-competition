package botsimp.testbot24;

import botsimp.testbot24.astar.Grid;
import botsimp.testbot24.astar.Node;
import botsimp.testbot24.astar.Pathfinding;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;

public class LookingMasterBotController implements BotController {
//    private boolean previouslyShotAtMaster = false; // vllt erst dann wieder auf true wenn kein master in einem bestimmten bereich ist

    private XY estimatedMiddle = new XY(0, 0);
    private XY me = null;

    private final ArrayList<XY> enemies = new ArrayList<>();

    @Override
    public void nextStep(ControllerContext context) {
        try {
            enemies.clear();
//            long startTime = System.currentTimeMillis();
            XY ul = context.getViewUpperLeft();
            XY lr = context.getViewLowerRight();

            me = context.locate();
            int newX = (int) (lr.x / 2f);
            int newY = (int) (lr.y / 2f);
            if (newX + newY > estimatedMiddle.x + estimatedMiddle.y) {
                estimatedMiddle = new XY(newX, newY);
                System.out.println("New estimated middle: " + estimatedMiddle);
            }

            int[][] board = new int[lr.x - ul.x + 1][lr.y - ul.y + 1];

            ArrayList<XY> targets = new ArrayList<>();
            ArrayList<XY> enemyMasters = new ArrayList<>();
            int enemyCount = 0;
            int miniCount = 0;
            boolean masterInRange = false;
            boolean miniInRange = false;

            for (int x = ul.x, i = 0; x <= lr.x; x++, i++) {
                for (int y = ul.y, j = 0; y <= lr.y; y++, j++) {
                    XY loc = new XY(x, y);
                    if (loc.equals(me))
                        continue;

                    EntityType type = context.getEntityAt(loc);
                    switch (type) {
                        //Don't walk there
                        case WALL:
                            board[i][j] = -1;
                            break;
                        case BAD_PLANT:
                            board[i][j] = 100;
                            break;
                        case BAD_BEAST:
                            for (int k = -1; k <= 1; k++) {
                                for (int l = -1; l <= 1; l++) {
                                    int offX = i + k;
                                    int offY = j + k;
                                    if (offX < 0 || offX >= board.length || offY < 0 || offY >= board[0].length)
                                        continue;
                                    board[offX][offY] = 200;
                                }
                            }
                            enemies.add(loc);
                            enemyCount++;
                            break;
                        case MASTER_SQUIRREL:
                            double dist = Math.abs(loc.distanceFrom(me));
                            if (dist < 4) {
                                System.out.println(Math.abs(loc.distanceFrom(me)));
                                masterInRange = true;
                                enemyMasters.add(loc);
                            }
                            enemies.add(loc);
                            enemyCount++;
                            board[i][j] = -1;
                            break;
                        case MINI_SQUIRREL: {
                            if (context.isMine(loc))
                                miniCount++;
                            else {
                                enemyCount++;
                                enemies.add(loc);
                                double dist2 = Math.abs(loc.distanceFrom(me));
                                if (dist2 < 5)
                                    miniInRange = true;
                            }

                            board[i][j] = -1;
                            break;
                        }
                        case GOOD_BEAST:
                        case GOOD_PLANT: {
                            targets.add(loc);
                            board[i][j] = 0;
                            break;
                        }
//                        default:
//                            if (board[i][j] != -1) {
//                                board[i][j] = 0;
//                            }
//                            break;
                    }
                }
            }
            if (miniInRange) {
                spawnMini(context);
//                System.out.println("Time past with spawn: " + (System.currentTimeMillis() - startTime));
                return;
            }
            Grid g = new Grid(board);
            Pathfinding p = new Pathfinding(g);
            Node start = g.getNodeAt(me.x - ul.x, me.y - ul.y);

            Node bestMove = null;
            int minMoves = Integer.MAX_VALUE;

            boolean escaped = false;
            if (masterInRange) {
                escaped = tryEscape(context, enemyMasters, ul, g, start, p);
            }
            if (!escaped) {
                if (//enemyCount > 1 &&
                        miniCount < 1// && !masterInRange
                ) {
                    if (spawnMini(context)) {
//                        System.out.println("Time past with spawn: " + (System.currentTimeMillis() - startTime));
                        return;
                    }
                }
                for (XY pos : targets) {
                    Node target = g.getNodeAt(pos.x - ul.x, pos.y - ul.y);
                    ArrayList<Node> path = p.findPath(start, target);
                    if (path.size() < 2)
                        continue;
                    Node next = path.get(1);
                    if (bestMove == null || target.getMoveCount() < minMoves) {
                        bestMove = next;
                        minMoves = target.getMoveCount();
                    }
                }
                if (bestMove != null)
                    context.move(new XY(bestMove.gridX - start.gridX, bestMove.gridY - start.gridY));
                else //maybe spawn mini
                    moveSave(context);
            }

//            System.out.println("Time past: " + (System.currentTimeMillis() - startTime));
        } catch (OutOfViewException e) {
            e.printStackTrace();
        }
    }

    private void moveSave(ControllerContext context) {
        XY me = context.locate();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (x == 0 && y == 0)
                    continue;
                XY loc = new XY(me.x + x, me.y + y);
                EntityType type = context.getEntityAt(loc);
                if (type == EntityType.NONE) {
                    context.move(new XY(x, y));
//                    System.out.println("Save move to " + new XY(x, y));
                    return;
                }
            }
        }
    }

    private boolean spawnMini1(ControllerContext context) {
        if (context.getEnergy() > 200) {
            for (int x = -1; x < 2; x++) {
                for (int y = -1; y < 2; y++) {
                    if (x == 0 && y == 0)
                        continue;
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

    private boolean spawnMini(ControllerContext context) {
        if (context.getEnergy() > 200) {
            for (int i = 0; i < 10; i++) {

//                XY dir = XY.randomDirection();
                XY dir = closestEnemyDir();
                XY loc = new XY(me.x + dir.x, me.y + dir.y);
                EntityType type = context.getEntityAt(loc);
                if (type == EntityType.NONE) {
                    context.spawnMiniBot(dir, 100);
                    return true;
                }
            }
        }
        return false;
    }

    private XY closestEnemyDir() {
        double closest = Double.MAX_VALUE;
        XY closestLoc = null;
        for (XY loc : enemies) {
            double dist = Math.abs(me.distanceFrom(loc));
            if(dist < closest){
                closest = dist;
                closestLoc = loc;
            }
        }
        if(closestLoc == null){
            closestLoc = XY.randomDirection();
        }
        return closestLoc.minus(me).toDirection();
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
                target = g.getNodeAt(me.x - ul.x + dir.x, me.y - ul.y + dir.y);
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

    private boolean tryEscape(ControllerContext context, ArrayList<XY> enemyMasterPos, XY ul, Grid g, Node start, Pathfinding p) {
        ArrayList<Node> path;

        XY dir = enemyMasterPos.stream().reduce(XY.ZERO_ZERO, XY::plus).minus(me).toDirection().times(-1);
        Node target;
        try {
//            target = g.getNodeAt(estimatedMiddle.x - ul.x, estimatedMiddle.y - ul.y);
            target = g.getNodeAt(me.x - ul.x + dir.x, me.y - ul.y + dir.y);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Cant escape2");
            return false;
        }
        path = p.findPath(start, target);

        if (path.size() < 2)
            return false;
        context.move(new XY(path.get(1).gridX - start.gridX, path.get(1).gridY - start.gridY));
        System.out.println("Try escape");
        return true;
    }

//    private int realDistanceTo(XY start, XY t){
//        Node target = g.getNodeAt(enemyMasterPos.x - ul.x + dir.x, enemyMasterPos.y - ul.y + dir.y);
//        path = p.findPath(start, target);
//    }
}
