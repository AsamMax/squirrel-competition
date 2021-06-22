package botsimp.testbot24;


import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import static de.hsa.games.fatsquirrel.core.entities.EntityType.*;
import de.hsa.games.fatsquirrel.utilities.XY;

public class LookingMiniBotController extends BaseBotController {

    private boolean goHome = false;

    private final int id;

    public LookingMiniBotController(BotControllerFactoryImpl botControllerFactory, int id) {
        super(botControllerFactory);
        this.id = id;
    }

    @Override
    protected void _nextStep() {
        factory.reportAlife(id, context.getRemainingSteps());
        try {

            int r = implosionRadius();
            if (r > 0) {
                context.implode(r);
                return;
            }

            if (goHome || context.getEnergy() > 1000) {
                goHome = true;

                XY dir = findParent();
                if (dir != null) {
                    context.move(dir.minus(me));
                }
            }

            XY move = BestMove();
            if (move != null) {
                XY dir = move.minus(me);
                context.move(dir);
                return;
            }

        } catch (OutOfViewException e) {
            e.printStackTrace();
        }
        if (DEBUG){
            System.out.println("Do nothing");
        }
        context.doNothing();
    }

    private XY BestMove() {
        currentTarget = null;

        XY enemy = closestEnemy();
        if (enemy != null) {
            int steps = findPath(10000, enemy);
            if (steps > 0 && steps < 50 && currentTarget != null) {
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
        return -1;
    }

    private XY moveSave() {

        XY dir = findParent();
        if (dir != null) {
            return dir.minus(me);
        }
        do {
            dir = XY.randomDirection();
        } while (context.getEntityAt(me.plus(dir)) != NONE);

        return dir;
    }

    private XY findParent() {
        XY ms = nearest(MASTER_SQUIRREL);
        if (ms != null) {
            int moves = findPath(Integer.MAX_VALUE, ms);
            if (moves > 0) {
                return ms;
            }
        }
        ms = me.plus(context.directionOfMaster());
        if (context.getEntityAt(ms) != WALL) {
            return ms;
        }
        return null;
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
        int energy = context.getEnergy();
        if (maxEnergy < Math.min(energy + 100, energy * 1.2)) {
            return -1;
        }
        return bestRadius;
    }

    private int simulateImplosion(ControllerContext context, int r) {
        XY lowerRight = me.plus(new XY(r, r));
        XY upperLeft = me.minus(new XY(r, r));
        int energy = context.getEnergy();
        int totalEnergyLoss = 0;

        for (int y = upperLeft.y; y <= lowerRight.y; ++y) {
            for (int x = upperLeft.x; x < lowerRight.x; ++x) {
                if (inSight(x, y)) {
                    XY loc = new XY(x, y);
                    double distance = loc.minus(me).length();
                    if (distance < (double) r) {
                        double impactArea = (double) (r * r) * Math.PI;
                        int energyLoss = (int) (-200.0D * ((double) energy / impactArea) * (1.0D - distance / (double) r));
                        int entityEnergy;
                        switch (context.getEntityAt(loc)) {
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

}
