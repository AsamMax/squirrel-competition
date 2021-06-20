package botsimp.testbot24;

import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.BotControllerFactory;

public class BotControllerFactoryImpl implements BotControllerFactory {
    @Override
    public BotController createMasterBotController() {
        return new LookingMasterBotController();
    }

    @Override
    public BotController createMiniBotController() {
        return new LookingMiniBotController();
    }
}
