package botsimp.testbot24;

import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.Hashtable;
import java.util.Map;

public class TargetManagement {
    protected static final Map<XY, EntityType> targets = new Hashtable<>();

    public static boolean hasEntry(XY loc){
        return targets.containsKey(loc);
    }

    public static void addEntry(XY loc, EntityType type){
        targets.put(loc, type);
    }

    public static void newFrame(){
        targets.clear();
    }
}
