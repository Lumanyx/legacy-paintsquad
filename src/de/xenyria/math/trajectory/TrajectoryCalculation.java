package de.xenyria.math.trajectory;

import de.xenyria.math.trajectory.csharp.MethodOutput;
import net.minecraft.server.v1_13_R2.EntityItem;
import net.minecraft.server.v1_13_R2.EntitySkeleton;

import java.util.ArrayList;

public class TrajectoryCalculation {

    private Vector3f startingPosition, targetPosition;
    private double speed, gravity;

    public TrajectoryCalculation(Vector3f startingPosition, Vector3f targetPosition, double speed, double gravity) {

        this.startingPosition = startingPosition;
        this.targetPosition = targetPosition;
        this.gravity = gravity;
        this.speed = speed;

    }

    private int foundSolutions = 0;
    public boolean found() { return foundSolutions == 2 && !results.isEmpty(); }

    private ArrayList<Trajectory> results = new ArrayList<>();
    public Trajectory getBestResult() { return results.get(0); }
    public Trajectory getWorstResult() { return results.get(1); }

    public void calculate(double divider) {

        MethodOutput output = TrajectoryFormula.solve_ballistic_arc(startingPosition, speed, targetPosition, gravity, new Vector3f(0f,0f,0f), new Vector3f(0f,0f,0f));
        foundSolutions = (int)output.objects[0];
        if(foundSolutions > 0) {

            for (int i = 0; i < foundSolutions; i++) {

                // Flugbahnen vorberechnet in Vektoren
                Vector3f solutionVector = (Vector3f) output.objects[i + 1];
                Trajectory trajectory = new Trajectory();
                trajectory.calculateFrom(divider, gravity, solutionVector, startingPosition, targetPosition);
                trajectory.setOriginSpeed(speed);
                results.add(trajectory);

            }

        }

    }

}
