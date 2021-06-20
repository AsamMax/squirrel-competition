package botsimp.testbot24.astar.utility;

public class Vector {

    public static final Vector ONE = new Vector(1,1,1);
    public static final Vector ZERO = new Vector(0,0,0);
    public static final Vector RIGHT = new Vector(1,0,0);
    public static final Vector LEFT = new Vector(-1,0,0);
    public static final Vector UP = new Vector(0,1,0);
    public static final Vector DOWN = new Vector(0,-1,0);
    public static final Vector FORWARD = new Vector(0,0,1);
    public static final Vector BACK = new Vector(0,0,-1);

    public int x, y, z;

    public Vector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector add(Vector v) {
        return new Vector(x + v.x, y + v.y, z + v.z);
    }

    public Vector multiply(int v) {
        return new Vector(x * v, y * v, z * v);
    }
}
