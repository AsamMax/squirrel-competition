package botsimp.testbot24;

import botsimp.testbot24.astar.Node;
import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import static de.hsa.games.fatsquirrel.core.entities.EntityType.*;
import de.hsa.games.fatsquirrel.core.entities.MiniSquirrelBot;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;
import java.util.stream.Stream;

public class LookingMasterBotController extends BaseBotController {
    //    private boolean previouslyShotAtMaster = false; // vllt erst dann wieder auf true wenn kein master in einem bestimmten bereich ist

    public static final boolean CAN_ESCAPE = false;

    public static byte nextDefaultSpot = 0;

    public LookingMasterBotController(BotControllerFactoryImpl botControllerFactory) {
        super(botControllerFactory);
    }


    @Override
    protected void _nextStep() {
        // factory.clearMinis(context.getRemainingSteps());

        try {

            XY near = nearestEnemyMini();
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
        XY target;
        do {
            target = defaultSpot();
            if (inSight(target)){
                nextDefaultSpot++;
                nextDefaultSpot %= 5;
                target = null;
            }
        }while (target == null);
        return target.minus(me).toDirection();
    }

    private XY defaultSpot() {
        switch (nextDefaultSpot){
            default:
            case 0:// center
                return getCenter();
            case 1:
                return getCorner(0,0);
            case 2:
                return getCorner(1,0);
            case 3:
                return getCorner(1,1);
            case 4:
                return getCorner(0,1);
        }
    }

    private boolean spawnMini() {
        if (context.getEnergy() > 200) {
            for (int i = 0; i < 10; i++) {
                try {
                    // XY dir = XY.randomDirection();
                    XY dir = closestEnemyDir();
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

            // XY dir = allUnits.get(MASTER_SQUIRREL).stream().reduce(XY.ZERO_ZERO, XY::plus).minus(me).toDirection().times(-1);
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

    private int targetsForMini() {
        ArrayList<XY> list = new ArrayList<>();
        list.addAll(allUnits.get(GOOD_BEAST));
        list.addAll(allUnits.get(GOOD_PLANT));
        list.addAll(allUnits.get(BAD_BEAST));
        list.addAll(allUnits.get(BAD_PLANT));
        list.addAll(allUnits.get(MASTER_SQUIRREL));
        list.addAll(allUnits.get(MINI_SQUIRREL));

        Stream<XY> filtered = list.stream().filter(loc -> {
            return !context.isMine(loc) && Util24.directSteps(me, loc) <= MiniSquirrelBot.MAXIMUM_SIGHT.x;
        });
        return (int) filtered.count();
    }
}
