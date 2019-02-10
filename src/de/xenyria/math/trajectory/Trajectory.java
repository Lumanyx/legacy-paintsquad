package de.xenyria.math.trajectory;

import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class Trajectory {

    private double speed;
    public double getSpeed() { return speed; }

    private double angle;
    public double getAngle() { return angle; }

    private ArrayList<Vector3f> vectors = new ArrayList<Vector3f>();
    public ArrayList<Vector3f> getVectors() { return vectors; }

    private double distancePerVector;
    public double getDistancePerVector() { return distancePerVector; }

    private Vector3f solutionNormalized;
    private Vector3f direction;
    public Vector3f getDirection() { return direction; }
    public double angleRad() {

        return Math.asin(solutionNormalized.y);

    }

    public void calculateFrom(double mDividier, double gravity, Vector3f solutionVector, Vector3f startPos, Vector3f goalPos) {

        startY = startPos.y;
        double requiredSpeed = solutionVector.length();
        solutionNormalized = solutionVector.clone().normalize();
        this.speed = requiredSpeed;
        double angleRad = Math.asin(solutionNormalized.y);
        this.angle = Math.toDegrees(angleRad);

        //Vector3f deltaVector = goalPos.clone().subtract(startPos);

        Vector3f startPosXZ = new Vector3f(startPos.x, 0f, startPos.z);
        Vector3f goalPosXZ = new Vector3f(goalPos.x, 0f, goalPos.z);
        direction = goalPosXZ.clone().subtract(startPosXZ).normalize();
        double distance = startPosXZ.distance(goalPosXZ);
        distancePerVector = mDividier;
        Vector3f cursorPosition = new Vector3f(startPos.x, startPos.y, startPos.z);

        for(double d = 0d; d < distance; d+=mDividier) {

            Vector3f currentPos = cursorPosition.clone();
            currentPos.add(direction.clone().multiply((float) d));
            double y = computeY(d, gravity, requiredSpeed);
            currentPos.y = (float) (cursorPosition.y + y);
            vectors.add(currentPos);

        }


    }

    public double computeY(double x, double g, double power)
    {
        // http://de.wikipedia.org/wiki/Wurfparabel
        //  #Mathematische_Beschreibung
        double b = angleRad();
        double v0 = power;
        if (b > Math.PI / 2)
        {
            b = Math.PI - b;
        }
        double cb = Math.cos(b);
        return x * Math.tan(b) - g / (2 * v0 * v0 * cb * cb) * x * x;
    }

    private float startY;
    public float startY() { return startY; }

    private double originSpeed;
    public double getOriginSpeed() { return originSpeed; }
    public void setOriginSpeed(double speed) {

        this.originSpeed = speed;

    }

}
