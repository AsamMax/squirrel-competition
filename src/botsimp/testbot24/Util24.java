package botsimp.testbot24;

import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.HashMap;

public class Util24 {

    public static boolean isBetween(XY loc, XY ul, XY lr) {
        return loc.x >= ul.x && loc.x <= lr.x &&
                loc.y >= ul.y && loc.y <= lr.y;
    }

    public static int directSteps(XY loc1, XY loc2) {
        XY vector = loc2.minus(loc1);
        int deltaX = Math.abs(vector.x);
        int deltaY = Math.abs(vector.y);

        return Math.max(deltaX, deltaY);
    }

    public static XY checkLowestRightest(XY lowestRightest, XY lr) {
        int x1 = Math.max(lowestRightest.x, lr.x);
        int y1 = Math.max(lowestRightest.y, lr.y);

        if (x1 != lowestRightest.x || y1 != lowestRightest.y) {
            lowestRightest = new XY(x1, y1);
            System.out.println("New estimated border: " + lowestRightest);
        }
        return lowestRightest;
    }

    public static class DefaultDict<K, V> extends HashMap<K, V> {
        Class<V> klass;

        @SuppressWarnings("unchecked")
        public DefaultDict(Class<?> klass) {
            this.klass = (Class<V>) klass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(Object key) {
            V returnValue = super.get(key);
            if (returnValue == null) {
                try {
                    returnValue = klass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                this.put((K) key, returnValue);
            }
            return returnValue;
        }
    }
}
