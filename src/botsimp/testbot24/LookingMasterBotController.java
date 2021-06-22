package botsimp.testbot24;

import botsimp.testbot24.astar.Node;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.entities.MasterSquirrelBot;
import de.hsa.games.fatsquirrel.core.entities.MiniSquirrelBot;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.hsa.games.fatsquirrel.core.entities.EntityType.*;

public class LookingMasterBotController extends BaseBotController {
    //    private boolean previouslyShotAtMaster = false; // vllt erst dann wieder auf true wenn kein master in einem bestimmten bereich ist

    public static final boolean CAN_ESCAPE = false;

    public static byte nextDefaultSpot = 0;

    public LookingMasterBotController(BotControllerFactoryImpl factory) {
        super(factory);
    }


    @Override
    protected void _nextStep() {
        // factory.clearMinis(context.getRemainingSteps());

        try {

            XY near = nearestEnemySquirrel(MINI_SQUIRREL);
            if (near != null && near.distanceFrom(me) < 5) {
                if (spawnMini()) {
                    return;
                }
            }
            near = nearest(MASTER_SQUIRREL);
            if (CAN_ESCAPE && near != null && near.distanceFrom(me) < 4) {
                boolean escaped = tryEscape();

                if (escaped) {
                    return;
                }
            }

            if (targetsForMini() > allUnits.get(MINI_SQUIRREL).size()) {
                if (spawnMini()) {
                    return;
                }
            }

            XY move = bestMove();
            this.context.move(move.minus(me));
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
            int moves = moveSave();
            if (moves > 0 && moves < 9999) {
                return currentTarget;
            } else {
                return me.plus(XY.randomDirection());
            }
        }
        return currentTarget;
    }


    private int moveSave() {
        XY target;
        int i = 0;
        do {
            target = defaultSpot();
            if (inSight(target)) { //TODO: vllt kleiner
                nextDefaultSpot++;
                nextDefaultSpot %= 8;
                target = null;
            }
        } while (target == null && i++ < 10);
        target = ensureInSight(target);
        return findPath(10000, target);
    }

    private XY defaultSpot() {
        switch (nextDefaultSpot) {
            default:
                return getCenter();
            case 1:
                return getCorner(0, 0);
            case 3:
                return getCorner(1, 0);
            case 5:
                return getCorner(1, 1);
            case 7:
                return getCorner(0, 1);
        }
    }

    private boolean spawnMini() {
        if (context.getEnergy() > 200) {
            for (int i = 0; i < 10; i++) {
                try {
                    // XY dir = XY.randomDirection();
                    XY dir = closestMiniTargetDir();
                    if (dir == null)
                        return false;
                    XY loc = new XY(me.x + dir.x, me.y + dir.y);
                    if (context.getEntityAt(loc) == NONE) {
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

    private XY closestMiniTargetDir() {
        XY closestLoc = closestEnemy();

        if (closestLoc != null) {
            return closestLoc.minus(me).toDirection();
        } else {
            bestGoodTarget();
            return currentTarget.minus(me).toDirection();
        }
    }

    private boolean tryEscape() {
        ensureGrid();
        ArrayList<Node> path;

        Node target;
        try {
            XY center = getCenter();
            target = g.getNodeAt(center.x - ul.x, center.y - ul.y);

            // XY dir = allUnits.get(MASTER_SQUIRREL).stream().reduce(XY.ZERO_ZERO, XY::plus).minus(me).toDirection().times(-1);
            // target = g.getNodeAt(me.x - ul.x + dir.x, me.y - ul.y + dir.y);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
        path = p.findPath(start, target);

        if (path.size() < 2) {
            return false;
        }
        XY dir = new XY(path.get(1).gridX - start.gridX, path.get(1).gridY - start.gridY);
        context.move(dir);
        return true;
    }

    private int targetsForMini() {
        ArrayList<XY> list = new ArrayList<>();
        list.addAll(allUnits.get(GOOD_BEAST));
        list.addAll(allUnits.get(GOOD_PLANT));
        list.addAll(allUnits.get(BAD_BEAST));
//        list.addAll(allUnits.get(BAD_PLANT));
        list.addAll(allUnits.get(MASTER_SQUIRREL));
        list.addAll(allUnits.get(MINI_SQUIRREL));

        List<XY> filtered = list.stream().filter(loc -> {
            return !context.isMine(loc) && BaseBotController.directSteps(me, loc) < MiniSquirrelBot.MAXIMUM_SIGHT.x - 3;
        }).collect(Collectors.toList());
        return filtered.size();
    }


    public XY ensureInSight(XY xy) {
        if (inSight(xy)) {
            return xy;
        }
        XY vector = xy.minus(me);
        int x = Integer.signum(xy.x) * Math.min(Math.abs(vector.x), MasterSquirrelBot.MAXIMUM_SIGHT.x - 1);
        int y = Integer.signum(xy.y) * Math.min(Math.abs(vector.y), MasterSquirrelBot.MAXIMUM_SIGHT.y - 1);

        return me.plus(new XY(x, y));
    }
}
