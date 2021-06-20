package botsimp.examplebot24;

import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.BotControllerFactory;

public class BotControllerFactoryImpl implements BotControllerFactory {
    @Override
    public BotController createMasterBotController() {
        //return null;
         return new ZergMasterBotController();
    }

    @Override
    public BotController createMiniBotController() {
        // return null;
        return new ZergMiniBotController();
    }
}
