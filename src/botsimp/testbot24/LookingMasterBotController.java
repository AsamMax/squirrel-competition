package botsimp.testbot24;

import botsimp.testbot24.astar.Node;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;

public class LookingMasterBotController extends BaseBotController {
    //    private boolean previouslyShotAtMaster = false; // vllt erst dann wieder auf true wenn kein master in einem bestimmten bereich ist

    public static final boolean CAN_ESCAPE = false;

    public static byte nextCorner = 0;


    @Override
    protected void _nextStep() {
        try {

            XY near = nearestEnemyMini();
            if (near != null && near.distanceFrom(me) < 5) {
                if (spawnMini()) {
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
                    return;
                }
            }

            this.context.move(bestMove().minus(me));
            return;


        } catch (OutOfViewException e) {
            e.printStackTrace();
        }
        System.out.println("Do nothing");
        context.doNothing();
    }

    private XY bestMove() {
        currentTarget = null;

        bestGoodTarget(); // This sets current Target

        if (currentTarget == null) {
            currentTarget = me.plus(moveSave());
        }
        return currentTarget;
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
                    return new XY(x, y);
                }
            }
        }
        return me;
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
            return false;
        }
        path = p.findPath(start, target);

        if (path.size() < 2) {
            return false;
        }
        context.move(new XY(path.get(1).gridX - start.gridX, path.get(1).gridY - start.gridY));
        return true;
    }

}
