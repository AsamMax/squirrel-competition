package botsimp.testbot24;

import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.BotControllerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BotControllerFactoryImpl implements BotControllerFactory {
    private final HashMap<Integer, Long> minis = new HashMap<>();
    private int nextId = 0;

    @Override
    public BotController createMasterBotController() {
        return new LookingMasterBotController(this);
    }

    @Override
    public BotController createMiniBotController() {
        newId();
        minis.put(nextId, -1L);
        return new LookingMiniBotController(this, nextId);
    }

    public void reportAlife(int id, long reainingFrames) {
        minis.put(id, reainingFrames);
    }

    private int newId() {
        return nextId++;
    }

    public void clearMinis(long remainingSteps) {
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : minis.entrySet()) {
            if (entry.getValue() - remainingSteps >= 2) {
                toRemove.add(entry.getKey());
            }
        }
        for (int key : toRemove){
            minis.remove(key);
        }
    }

    public int getMiniCount(){
        return minis.size();
    }
}
