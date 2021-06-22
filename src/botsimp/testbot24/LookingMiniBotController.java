package botsimp.testbot24;


import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

public class LookingMiniBotController extends BaseBotController {

    public static final boolean CAN_ESCAPE = false;
    private XY currentTarget;

    @Override
    protected void _nextStep() {
        try {

            int r = implosionRadius();
            if (r > 0) {
                context.implode(r);
                return;
            }

            XY move = BestMove();
            if (move != null){
                context.move(move.minus(me));
                return;
            }

        } catch (OutOfViewException e) {
            e.printStackTrace();
        }
        System.out.println("Do nothing");
        context.doNothing();
    }

    private XY BestMove() {

        XY enemy = closestEnemy();
        if (enemy != null) {
            int steps = findPath(40, enemy);
            if (steps > 0) {
                return currentTarget;
            }
        }
        bestGoodTarget(); // This sets current Target

        if (currentTarget == null) {
            currentTarget = me.plus(moveSave());
        }
        return currentTarget;

    }

    private int implosionRadius() {

        XY enemy = closestEnemy();
        if (enemy != null && enemy.distanceFrom(me) <= 3) {
            return getBestRadius(context);
        }

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
            return bestRadius;
        }
        return -1;
    }

    private XY moveSave() {
        XY me = context.locate();
        XY dir;
        do {
            dir = XY.randomDirection();
        } while (context.getEntityAt(me.plus(dir)) != EntityType.NONE);

        return dir;
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
                        double impactArea = (double) (r * r) * Math.PI;
                        int energyLoss = (int) (-200.0D * ((double) energy / impactArea) * (1.0D - distance / (double) r));
                        int entityEnergy;
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
                            default:
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
        return x >= ul.x && x <= lr.x && y >= ul.y && y <= lr.y;
    }

}
