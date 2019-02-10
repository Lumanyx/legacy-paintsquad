package de.xenyria.splatoon.ai.entity;

import org.bukkit.util.Vector;

public class VelocityProcessor {

    private Vector velocity = new Vector(0,0,0);

    public static double MIN_MOVEMENT_VALUE = 0.05d;
    public static double VERT_VELOCITY_DECREMENT_FACTOR = 0.8515d;
    public static double HOR_VELOCITY_DECREMENT_FACTOR = 0.72525d;

    public boolean ground = false;
    private int airTicks = 0;
    public double airTicksToExpectedVelocity() {

        if(airTicks >= 1 && airTicks <= 16) {

            double ratio = (double)airTicks / 16d;
            return ratio;

        } else if(airTicks > 16 && airTicks <= 37) {

            double minVal = 17d;
            double delta = 37 - minVal;
            double ratio = (double)(airTicks - minVal) / delta;
            return 1d + ratio;

        } else if(airTicks > 37 && airTicks <= 73) {

            double minVal = 38;
            double delta = 73 - minVal;
            double ratio = (double)(airTicks - minVal) / delta;
            return 2d + ratio;

        } else {

            return 3d;

        }

    }

    public boolean hasVelocity() {

        return Math.abs(velocity.getX()) >= MIN_MOVEMENT_VALUE || Math.abs(velocity.getY()) >= MIN_MOVEMENT_VALUE || Math.abs(velocity.getZ()) >= MIN_MOVEMENT_VALUE;

    }

    public void process() {

        double y = velocity.getY();
        // Gravitation
        if(y > 0) {

            y*=VERT_VELOCITY_DECREMENT_FACTOR;
            if(y <= MIN_MOVEMENT_VALUE) { y = 0d; }
            velocity.setY(y);
            airTicks = 0;

        } else {

            if(!ground) {

                airTicks++;
                y = -airTicksToExpectedVelocity();
                velocity.setY(y);

            } else {

                y = 0d;
                airTicks = 0;
                velocity.setY(y);

            }

        }
        double x = velocity.getX() * HOR_VELOCITY_DECREMENT_FACTOR;
        double z = velocity.getZ() * HOR_VELOCITY_DECREMENT_FACTOR;
        if(x < MIN_MOVEMENT_VALUE) { x = 0; }
        if(z < MIN_MOVEMENT_VALUE) { z = 0; }
        velocity.setX(x); velocity.setZ(z);

    }

    public static void main(String[] args) {

        VelocityProcessor processor = new VelocityProcessor();
        double y = 0d;
        int i = 0;
        double travelledY = 0d;
        processor.getVelocity().add(new Vector(1, 1, 0));
        while (processor.velocity.getY() > 0) {

            travelledY+=processor.velocity.getY();
            processor.process();
            i++;

        }
        for(int xc = 10; xc > 0; xc--) {

            System.out.println(xc);

        }
        System.out.println(travelledY);

    }

    public Vector getVelocity() { return velocity; }
    public void setVelocity(Vector vector) { this.velocity = vector; airTicks = 0; }

    public void resetAirTicks() { airTicks = 0; }

    public void setGrounded(boolean onGround) { this.ground = onGround; if(onGround) { resetAirTicks(); }}

    public int getAirTicks() { return airTicks; }
}
