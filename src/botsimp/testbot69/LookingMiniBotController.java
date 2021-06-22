package botsimp.testbot69;


import botsimp.testbot69.astar.Grid;
import botsimp.testbot69.astar.Node;
import botsimp.testbot69.astar.Pathfinding;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;

public class LookingMiniBotController implements BotController {
    private static int BAD_BEAST = 1;
    private static int BAD_PLANT = 2;
    private static int GOOD_BEAST = 3;
    private static int GOOD_PLANT = 4;
    private static int MASTER_SQUIRREL = 5;
    private static int MINI_SQUIRREL = 6;
    private static int WALL = 7;
    private static int NONE = 8;

    @Override
    public void nextStep(ControllerContext context) {
        try {
            XY ul = context.getViewUpperLeft();
            XY lr = context.getViewLowerRight();

            int[][] board = new int[lr.x - ul.x + 1][lr.y - ul.y + 1];

            ArrayList<XY> goodTargets = new ArrayList<>();
            ArrayList<XY> badTargets = new ArrayList<>();
            ArrayList<XY> badEnemyTargets = new ArrayList<>();
            ArrayList<XY> enemyMasters = new ArrayList<>();
            XY myMaster = null;

            for (int x = ul.x, i = 0; x < lr.x; x++, i++) {
                for (int y = ul.y, j = 0; y < lr.y; y++, j++) {
                    XY loc = new XY(x, y);
                    EntityType type = context.getEntityAt(loc);
                    switch (type) {
                        //Don't walk there
                        case WALL:
                            board[i][j] = -1;
                            break;
                        case BAD_PLANT:
                            board[i][j] = 100;
                            break;
                        case BAD_BEAST: //immer 7 explosion
                            badTargets.add(loc);
                            board[i][j] = -1;

                            break;
                        case MASTER_SQUIRREL:
                            if (context.isMine(loc)) {
                                myMaster = loc;
                            } else {
                                enemyMasters.add(loc);
                                badTargets.add(loc);
                                badEnemyTargets.add(loc);
                            }
                            board[i][j] = -1;
                            break;
                        case MINI_SQUIRREL:
                            if (context.isMine(loc)) {
                                board[i][j] = -1;
                                break;
                            }
                            board[i][j] = 0;

                            badEnemyTargets.add(loc);
                            break;
                        case GOOD_BEAST:
                            goodTargets.add(loc);
                            board[i][j] = 0;
                            break;
                        case GOOD_PLANT:
                            goodTargets.add(loc);
                            board[i][j] = 0;
                            break;
                    }
                }
            }
            Grid g = new Grid(board);
            Pathfinding p = new Pathfinding(g);
            Node start = g.getNodeAt(context.locate().x - ul.x, context.locate().y - ul.y);

            Node bestMove = null;
            int minMoves = Integer.MAX_VALUE;
            boolean moveFound = false;

            if (badTargets.size() > 0) {
                for (XY pos : badTargets) {
                    if (myMaster != null && context.getEntityAt(pos) == EntityType.MASTER_SQUIRREL && pos.distanceFrom(myMaster) < 7)
                        continue;
                    double dist = pos.distanceFrom(context.locate());
                    if (dist < 3) {
                        context.implode(getBestRadius(context));
                        return;
                    }
                    Node target = g.getNodeAt(pos.x - ul.x, pos.y - ul.y);
                    ArrayList<Node> path = p.findPath(start, target);
                    if (path.size() < 2)
                        continue;
                    Node next = path.get(1);
                    if (bestMove == null || target.getMoveCount() < minMoves) {
                        bestMove = next;
                        minMoves = target.getMoveCount();
                        moveFound = true;
                    }
                }
            }
            if (badEnemyTargets.size() > 0 && !moveFound) {
                for (XY pos : badEnemyTargets) {
                    if (context.getEntityAt(pos) == EntityType.MASTER_SQUIRREL && pos.distanceFrom(myMaster) < 5)
                        continue;
                    double dist = pos.distanceFrom(context.locate());
                    if (dist < 4) {
                        context.implode(getBestRadius(context));
                        return;
                    }
                    Node target = g.getNodeAt(pos.x - ul.x, pos.y - ul.y);
                    ArrayList<Node> path = p.findPath(start, target);
                    if (path.size() < 2)
                        continue;
                    Node next = path.get(1);
                    if (bestMove == null || target.getMoveCount() < minMoves) {
                        bestMove = next;
                        minMoves = target.getMoveCount();
                        moveFound = true;
                    }
                }
            }
            if (!moveFound) {
                int maxEnergy = -1;
                int bestRadius = 0;
                for (int r = 2; r <= 10; r++) {
                    int e = simulateImplosion(context, r);
                    if (e > maxEnergy) {
                        maxEnergy = e;
                        bestRadius = r;
                    }
                }
                if (bestRadius != 0 && maxEnergy > context.getEnergy() + 100) {
                    context.implode(bestRadius);
                    return;
                } else {
                    for (XY pos : goodTargets) {
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
                }
            }
            if (bestMove != null)
                context.move(new XY(bestMove.gridX - start.gridX, bestMove.gridY - start.gridY));
            else {
                if (context.getEnergy() < 20) {
                    int bestRadius = getBestRadius(context);
                    context.implode(bestRadius == 0 ? 10 : bestRadius);
                }
                moveSave(context);

            }
//                context.doNothing();
        } catch (OutOfViewException|NullPointerException e) {
            e.printStackTrace();

            context.doNothing();
        }
    }

    private void moveSave1(ControllerContext context) {
        XY me = context.locate();
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                if (x == 0 && y == 0)
                    continue;
                XY loc = new XY(me.x + x, me.y + y);
                EntityType type = context.getEntityAt(loc);
                if (type == EntityType.NONE) {
                    context.move(new XY(x, y));
                    return;
                }
            }
        }
    }

    private void moveSave(ControllerContext context) {
        XY me = context.locate();
        XY dir;
        do {
            dir = XY.randomDirection();
        } while (context.getEntityAt(me.plus(dir)) != EntityType.NONE);

        context.move(dir);
    }

    private int getBestRadius(ControllerContext context) {
        int bestRadius = 2;
        int maxEnergy = -1;
        for (int r = 2; r <= 10; r++) {
            int e = simulateImplosion(context, r);
            if (e > maxEnergy) {
                maxEnergy = e;
                bestRadius = r;
            }
        }
        return bestRadius;
    }

    private int simulateImplosion(ControllerContext context, int r) {
        XY me = context.locate();
        XY lowerRight = context.locate().plus(new XY(r, r));
        XY upperLeft = context.locate().minus(new XY(r, r));
        int energy = context.getEnergy();
        int totalEnergyLoss = 0;

        for (int y = upperLeft.y; y <= lowerRight.y; ++y) {
            for (int x = upperLeft.x; x < lowerRight.x; ++x) {
                if (inSight(context, x, y)) {
                    XY loc = new XY(x, y);
                    EntityType entity = context.getEntityAt(loc);
                    double distance = loc.minus(me).length();
                    if (distance < (double) r) {
                        double impactArea = (double) (r * r) * 3.141592653589793D;
                        int energyLoss = (int) (-200.0D * ((double) energy / impactArea) * (1.0D - distance / (double) r));
                        int entityEnergy = 0;
                        switch (entity) {
                            case BAD_BEAST:
                                entityEnergy = -150;
                                break;
                            case BAD_PLANT:
                                entityEnergy = -100;
                                break;
                            case GOOD_BEAST:
                                entityEnergy = 200;
                                break;
                            case GOOD_PLANT:
                                entityEnergy = 100;
                                break;
                            case MASTER_SQUIRREL:
                                entityEnergy = context.isMine(loc) ? 0 : 10000;
                                break;
                            case MINI_SQUIRREL:
                                entityEnergy = context.isMine(loc) ? 0 : 100;
                                break;
                            case WALL:
                            case NONE:
                                entityEnergy = 0;
                                break;
                        }
                        int actualEnergyLoss = (int) (Math.abs(energyLoss) > Math.abs(entityEnergy) ? (float) (-entityEnergy) : (float) energyLoss * Math.signum((float) entityEnergy));

                        totalEnergyLoss += Math.abs(actualEnergyLoss);
                    }
                }
            }
        }
        return totalEnergyLoss;
    }

    private boolean inSight(ControllerContext context, int x, int y) {
        XY ul = context.getViewUpperLeft();
        XY lr = context.getViewLowerRight();

        return x >= ul.x && x <= lr.x && y >= ul.y && y <= lr.y;
    }
}
