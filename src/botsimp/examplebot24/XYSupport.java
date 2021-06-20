package botsimp.examplebot24;

import de.hsa.games.fatsquirrel.utilities.XY;

import java.util.List;
import java.util.Random;

public class XYSupport {
    public static XY getVectorTo(XY loc1, XY loc2) {
        return loc2.minus(loc1);
    }

    public static int getDistance(XY loc1, XY loc2) {
        XY vector = getVectorTo(loc1, loc2);
        int deltaX = Math.abs(vector.x);
        int deltaY = Math.abs(vector.y);

        return Math.max(deltaX, deltaY);
    }

    public static XY nearest(XY start, List<XY> ends) {
        XY ret = null;
        int shortest = Integer.MAX_VALUE;
        for (XY end : ends) {
            int distance = XYSupport.getDistance(start, end);
            if (distance < shortest) {
                ret = end;
                shortest = distance;
            }
        }
        return ret;
    }


    public static XY randomDirection() {
        int random = new Random().nextInt(8);

        // 012
        // 3X4
        // 567
        // X is the starting position

        switch (random) {
            case 0:
                return XY.LEFT_UP;

            case 1:
                return XY.UP;

            case 2:
                return XY.RIGHT_UP;

            case 3:
                return XY.LEFT;

            case 4:
                return XY.RIGHT;

            case 5:
                return XY.LEFT_DOWN;

            case 6:
                return XY.DOWN;

            case 7:
                return XY.RIGHT_DOWN;

            default:
                throw new IllegalStateException("Unexpected value: " + random);
        }
    }
}
