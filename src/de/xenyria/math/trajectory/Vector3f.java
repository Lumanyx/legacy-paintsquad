package de.xenyria.math.trajectory;

public class Vector3f implements Cloneable {

    public float x,y,z;
    public Vector3f(float x, float y, float z) {

        this.x = x; this.y = y; this.z = z;

    }

    public Vector3f(double x, double y, double z) {

        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;

    }

    public static Vector3f zero() { return new Vector3f(0,0,0); }

    public static Vector3f up() {
        return new Vector3f(0, 1, 0);
    }

    public Vector3f normalize() {
        Vector3f v2 = new Vector3f(0,0,0);

        double length = Math.sqrt( this.x*this.x + this.y*this.y + this.z*this.z );
        if (length != 0) {
            v2.x = (float) (this.x/length);
            v2.y = (float) (this.y/length);
            v2.z = (float) (this.z/length);
        }

        return v2;
    }

    public double dot(Vector3f that) {

        return (x * that.x) + (y * that.y) + (z * that.z);

    }
    public static double dot(Vector3f a, Vector3f b) {

        return a.dot(b);

    }

    public Vector3f subtract(Vector3f vec) {

        x-=vec.x;
        y-=vec.y;
        z-=vec.z;
        return this;

    }
    public Vector3f add(Vector3f vec) {

        x+=vec.x;
        y+=vec.y;
        z+=vec.z;
        return this;

    }
    public Vector3f multiply(Vector3f vec) {

        x*=vec.x;
        y*=vec.y;
        z*=vec.z;
        return this;

    }
    public double length() {

        return Math.sqrt((x*x) + (y*y) + (z*z));

    }
    public double magnitude() {
        return Math.sqrt(this.dot(this));
    }
    public Vector3f clone() {

        try {

            return ((Vector3f)super.clone());

        } catch (Exception e) {

            return null;

        }

    }

    public Vector3f multiply(float lateral_speed) {

        this.x*=lateral_speed;
        this.y*=lateral_speed;
        this.z*=lateral_speed;
        return this;

    }
    public Vector3f multiply(double lateral_speed) {

        this.x*=lateral_speed;
        this.y*=lateral_speed;
        this.z*=lateral_speed;
        return this;

    }
    public double distance(Vector3f end) {

        double dx = x-end.x;
        double dy = y-end.y;
        double dz = z-end.z;

        return Math.sqrt(dx*dx+dy*dy+dz*dz);

    }

}
