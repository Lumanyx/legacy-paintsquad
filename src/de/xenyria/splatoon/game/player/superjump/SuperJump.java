package de.xenyria.splatoon.game.player.superjump;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.math.trajectory.TrajectoryCalculation;
import de.xenyria.math.trajectory.Vector3f;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.color.Color;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SuperJump {

    private Vector start, end;
    public Vector getStart() { return start; }
    public Vector getEnd() { return end; }

    public float yaw(World w) {

        Vector delta = end.clone().subtract(start.clone());
        Location location = new Location(w,0,0,0);
        location.setDirection(delta);
        return location.getYaw();

    }

    private Trajectory trajectory = null;
    public Trajectory getTrajectory() { return trajectory; }

    public SuperJump(Vector start, Vector end) {

        this.start = start;
        this.end = end;

    }
    public boolean calculate() {

        double speed = 3d;
        int iterCount = 0;

        while (true) {

            TrajectoryCalculation calculation = new TrajectoryCalculation(new Vector3f(
                    start.getX(), start.getY(), start.getZ()
            ), new Vector3f(end.getX(), end.getY(), end.getZ()), speed, 0.125);

            speed+=0.1d;
            iterCount++;

            double superJumpTime = 80;
            double distance = start.clone().setY(0).distance(end.clone().setY(0));
            double blocksPerSec = (distance / superJumpTime);

            calculation.calculate(blocksPerSec);
            if(calculation.found() && calculation.getWorstResult() != null) {

                trajectory = calculation.getWorstResult();
                return true;

            }

            if(iterCount > 1000) { return false; }

        }

    }

    public static void main(String[] args) {

        Vector start = new Vector(0,0,0);
        Vector end = new Vector(10,0,0);

        double speed = 4d;
        for(int x = 0; x < 10; x++) {


            System.out.println(x);
        }

        TrajectoryCalculation calculation = new TrajectoryCalculation(new Vector3f(
                start.getX(), start.getY(), start.getZ()
        ), new Vector3f(end.getX(), end.getY(), end.getZ()), speed, 0.125);

        speed+=0.1d;

        double superJumpTime = 80;
        double distance = start.clone().setY(0).distance(end.clone().setY(0));
        double blocksPerSec = (distance / superJumpTime);

        calculation.calculate(blocksPerSec);
        if(calculation.found() && calculation.getWorstResult() != null) {

            System.out.println(calculation.getWorstResult().getVectors().size());

        }

    }

}
