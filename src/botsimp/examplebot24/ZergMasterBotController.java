package botsimp.examplebot24;

import de.hsa.games.fatsquirrel.core.actions.OutOfViewException;
import de.hsa.games.fatsquirrel.core.actions.SpawnException;
import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static botsimp.examplebot24.XYSupport.nearest;

public class ZergMasterBotController implements BotController {
    @Override
    public void nextStep(ControllerContext context) {
        try {
            HashMap<EntityType, List<XY>> e = collect(context);

            XY mini = nearest(context.locate(), e.get(EntityType.MINI_SQUIRREL));
            if (mini != null && XYSupport.getDistance(context.locate(), mini) < 3 && !context.isMine(mini)) {
                context.move(XYSupport.getVectorTo(context.locate(),mini));
                return;
            }

            XY goodB = nearest(context.locate(), e.get(EntityType.GOOD_BEAST));
            if (goodB != null && XYSupport.getDistance(context.locate(), goodB) < 3) {
                context.move(XYSupport.getVectorTo(context.locate(),goodB));
                return;
            }

            if (context.getEnergy() > 1000) {
                context.spawnMiniBot(XYSupport.randomDirection(), 1000);
                return;
            }

            context.move(XYSupport.randomDirection());
        } catch (OutOfViewException | SpawnException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<EntityType, List<XY>> collect(ControllerContext context) throws OutOfViewException {
        HashMap<EntityType, List<XY>> map = new HashMap<>();
        for (EntityType et : EntityType.values()) {
            map.put(et, new ArrayList<>());
        }
        XY ll = context.getViewUpperLeft();
        XY ur = context.getViewLowerRight();

        for (int x = ll.x; x < ur.x; x++) {
            for (int y = ll.y; y < ur.y; y++) {
                XY loc = new XY(x, y);
                map.get(context.getEntityAt(loc)).add(loc);
            }
        }

        return map;
    }
}
