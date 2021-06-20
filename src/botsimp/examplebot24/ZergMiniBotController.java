package botsimp.examplebot24;

import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.HashMap;
import java.util.List;

import static botsimp.examplebot24.XYSupport.nearest;


public class ZergMiniBotController implements BotController {
    @Override
    public void nextStep(ControllerContext context) {
        try {
            HashMap<EntityType, List<XY>> e = ZergMasterBotController.collect(context);

            XY master = nearest(context.locate(), e.get(EntityType.MASTER_SQUIRREL));
            if (master != null && !context.isMine(master)) {
                double distance = master.distanceFrom(context.locate());
                if (distance < 3){
                    context.implode((int) Math.ceil(distance));
                }else{
                    context.move(XYSupport.getVectorTo(context.locate(),master));
                }
                return;
            }
            context.move(XYSupport.randomDirection());
        } catch (OutOfViewException outOfViewException) {
            outOfViewException.printStackTrace();
        }
    }
}
