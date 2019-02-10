package de.xenyria.math.trajectory;

import de.xenyria.math.trajectory.csharp.MethodOutput;

public class TrajectoryFormula {

    // SolveQuadric, SolveCubic, and SolveQuartic were ported from C as written for Graphics Gems I
    // Original Author: Jochen Schwarze (schwarze@isa.de)
    // https://github.com/erich666/GraphicsGems/blob/240a34f2ad3fa577ef57be74920db6c4b00605e4/gems/Roots3And4.c

    // Utility function used by SolveQuadratic, SolveCubic, and SolveQuartic
    public static boolean IsZero(double d) {
        double eps = 1e-9;
        return d > -eps && d < eps;
    }

    // Solve quadratic equation: c0*x^2 + c1*x + c2.
    // Returns number of solutions.
    public static double[] SolveQuadric(double c0, double c1, double c2, double s0, double s1) {
        s0 = Double.NaN;
        s1 = Double.NaN;

        double p, q, D;

        /* normal form: x^2 + px + q = 0 */
        p = c1 / (2 * c0);
        q = c2 / c0;

        D = p * p - q;

        if (IsZero(D)) {
            s0 = -p;
            return new double[]{1, s0, s1};
        }
        else if (D < 0) {
            return new double[]{0, s0, s1};
        }
        else /* if (D > 0) */ {
            double sqrt_D = Math.sqrt(D);

            s0 =   sqrt_D - p;
            s1 = -sqrt_D - p;
            return new double[]{2, s0, s1};
        }
    }

    // Solve cubic equation: c0*x^3 + c1*x^2 + c2*x + c3.
    // Returns number of solutions.
    public static double[] SolveCubic(double c0, double c1, double c2, double c3, double s0, double s1, double s2)
    {
        s0 = Double.NaN;
        s1 = Double.NaN;
        s2 = Double.NaN;

        int     num;
        double  sub;
        double  A, B, C;
        double  sq_A, p, q;
        double  cb_p, D;

        /* normal form: x^3 + Ax^2 + Bx + C = 0 */
        A = c1 / c0;
        B = c2 / c0;
        C = c3 / c0;

        /*  substitute x = y - A/3 to eliminate quadric term:  x^3 +px + q = 0 */
        sq_A = A * A;
        p = 1.0/3 * (- 1.0/3 * sq_A + B);
        q = 1.0/2 * (2.0/27 * A * sq_A - 1.0/3 * A * B + C);

        /* use Cardano's formula */
        cb_p = p * p * p;
        D = q * q + cb_p;

        if (IsZero(D)) {
            if (IsZero(q)) /* one triple solution */ {
                s0 = 0;
                num = 1;
            }
            else /* one single and one double solution */ {
                double u = Math.pow(-q, 1.0/3.0);
                s0 = 2 * u;
                s1 = - u;
                num = 2;
            }
        }
        else if (D < 0) /* Casus irreducibilis: three real solutions */ {
            double phi = 1.0/3 * Math.acos(-q / Math.sqrt(-cb_p));
            double t = 2 * Math.sqrt(-p);

            s0 =   t * Math.cos(phi);
            s1 = - t * Math.cos(phi + Math.PI / 3);
            s2 = - t * Math.cos(phi - Math.PI / 3);
            num = 3;
        }
        else /* one real solution */ {
            double sqrt_D = Math.sqrt(D);
            double u = Math.pow(sqrt_D - q, 1.0/3.0);
            double v = - Math.pow(sqrt_D + q, 1.0/3.0);

            s0 = u + v;
            num = 1;
        }

        /* resubstitute */
        sub = 1.0/3 * A;

        if (num > 0)    s0 -= sub;
        if (num > 1)    s1 -= sub;
        if (num > 2)    s2 -= sub;

        return new double[]{num, s0, s1, s2};
    }

    // Solve quartic function: c0*x^4 + c1*x^3 + c2*x^2 + c3*x + c4.
    // Returns number of solutions.
    public static double[] SolveQuartic(double c0, double c1, double c2, double c3, double c4, double s0, double s1, double s2, double s3) {
        s0 = Double.NaN;
        s1 = Double.NaN;
        s2 = Double.NaN;
        s3 = Double.NaN;

        double[]  coeffs = new double[4];
        double  z, u, v, sub;
        double  A, B, C, D;
        double  sq_A, p, q, r;
        int     num;

        /* normal form: x^4 + Ax^3 + Bx^2 + Cx + D = 0 */
        A = c1 / c0;
        B = c2 / c0;
        C = c3 / c0;
        D = c4 / c0;

        /*  substitute x = y - A/4 to eliminate cubic term: x^4 + px^2 + qx + r = 0 */
        sq_A = A * A;
        p = - 3.0/8 * sq_A + B;
        q = 1.0/8 * sq_A * A - 1.0/2 * A * B + C;
        r = - 3.0/256*sq_A*sq_A + 1.0/16*sq_A*B - 1.0/4*A*C + D;

        if (IsZero(r)) {
            /* no absolute term: y(y^3 + py + q) = 0 */

            coeffs[ 3 ] = q;
            coeffs[ 2 ] = p;
            coeffs[ 1 ] = 0;
            coeffs[ 0 ] = 1;

            double[] solveCubicResult = TrajectoryFormula.SolveCubic(coeffs[0], coeffs[1], coeffs[2], coeffs[3], s0, s1, s2);

            num = (int) solveCubicResult[0];
            s0 = solveCubicResult[1];
            s1 = solveCubicResult[2];
            s2 = solveCubicResult[3];

        }
        else {
            /* solve the resolvent cubic ... */
            coeffs[ 3 ] = 1.0/2 * r * p - 1.0/8 * q * q;
            coeffs[ 2 ] = - r;
            coeffs[ 1 ] = - 1.0/2 * p;
            coeffs[ 0 ] = 1;

            double[] solveCubicResult = SolveCubic(coeffs[0], coeffs[1], coeffs[2], coeffs[3], s0, s1, s2);
            s0 = solveCubicResult[1];
            s1 = solveCubicResult[2];
            s2 = solveCubicResult[3];


            /* ... and take the one real solution ... */
            z = s0;

            /* ... to build two quadric equations */
            u = z * z - r;
            v = 2 * z - p;

            if (IsZero(u))
                u = 0;
            else if (u > 0)
                u = Math.sqrt(u);
            else
                return new double[]{0, s0, s1, s2, s3};

            if (IsZero(v))
                v = 0;
            else if (v > 0)
                v = Math.sqrt(v);
            else
                return new double[]{0, s0, s1, s2, s3};

            coeffs[ 2 ] = z - u;
            coeffs[ 1 ] = q < 0 ? -v : v;
            coeffs[ 0 ] = 1;

            double[] nSolveQuadric = TrajectoryFormula.SolveQuadric(coeffs[0], coeffs[1], coeffs[2], s0, s1);
            //num = de.xenyria.math.trajectory.TrajectoryFormula.SolveQuadric(coeffs[0], coeffs[1], coeffs[2], s0, s1);
            num = (int)nSolveQuadric[0];
            s0 = nSolveQuadric[1];
            s1 = nSolveQuadric[2];

            coeffs[ 2 ]= z + u;
            coeffs[ 1 ] = q < 0 ? v : -v;
            coeffs[ 0 ] = 1;

            if (num == 0) {

                double[] nnSolveQuadric = TrajectoryFormula.SolveQuadric(coeffs[0], coeffs[1], coeffs[2], s0, s1);
                num += (int)nnSolveQuadric[0];
                s0 = nnSolveQuadric[1];
                s1 = nnSolveQuadric[2];

            }
            if (num == 1) {

                double[] nnSolveQuadric = TrajectoryFormula.SolveQuadric(coeffs[0], coeffs[1], coeffs[2], s1, s2);
                num += (int)nnSolveQuadric[0];
                s1 = nnSolveQuadric[1];
                s2 = nnSolveQuadric[2];

            }
            if (num == 2) {

                double[] nnSolveQuadric = TrajectoryFormula.SolveQuadric(coeffs[0], coeffs[1], coeffs[2], s2, s3);
                num += (int)nnSolveQuadric[0];
                s2 = nnSolveQuadric[1];
                s3 = nnSolveQuadric[2];

            }

        }

        /* resubstitute */
        sub = 1.0/4 * A;

        if (num > 0)    s0 -= sub;
        if (num > 1)    s1 -= sub;
        if (num > 2)    s2 -= sub;
        if (num > 3)    s3 -= sub;

        return new double[]{num, s0, s1, s2, s3};
    }


    // Calculate the maximum range that a ballistic projectile can be fired on given speed and gravity.
    //
    // speed (float): projectile velocity
    // gravity (float): force of gravity, positive is down
    // initial_height (float): distance above flat terrain
    //
    // return (float): maximum range
    public static final float Deg2Rad = (float) ((Math.PI * 2) / 360f);
    public static float ballistic_range(float speed, float gravity, float initial_height) {

        // Handling these cases is up to your project's coding standards
        //Debug.Assert(speed > 0 && gravity > 0 && initial_height >= 0, "de.xenyria.math.trajectory.TrajectoryFormula.ballistic_range called with invalid data");

        // Derivation
        //   (1) x = speed * time * cos O
        //   (2) y = initial_height + (speed * time * sin O) - (.5 * gravity*time*time)
        //   (3) via quadratic: t = (speed*sin O)/gravity + sqrt(speed*speed*sin O + 2*gravity*initial_height)/gravity    [ignore smaller root]
        //   (4) solution: range = x = (speed*cos O)/gravity * sqrt(speed*speed*sin O + 2*gravity*initial_height)    [plug t back into x=speed*time*cos O]
        float angle = 45 * Deg2Rad; // no air resistence, so 45 degrees provides maximum range
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float range = (float) ((speed*cos/gravity) * (speed*sin + Math.sqrt(speed*speed*sin*sin + 2*gravity*initial_height)));
        return range;
    }


    // Solve firing angles for a ballistic projectile with speed and gravity to hit a fixed position.
    //
    // proj_pos (de.xenyria.math.trajectory.Vector3f): point projectile will fire from
    // proj_speed (float): scalar speed of projectile
    // target (de.xenyria.math.trajectory.Vector3f): point projectile is trying to hit
    // gravity (float): force of gravity, positive down
    //
    // s0 (out de.xenyria.math.trajectory.Vector3f): firing solution (low angle)
    // s1 (out de.xenyria.math.trajectory.Vector3f): firing solution (high angle)
    //
    // return (int): number of unique solutions found: 0, 1, or 2.
    public static MethodOutput solve_ballistic_arc(Vector3f proj_pos, double proj_speed, Vector3f target, double gravity, Vector3f s0, Vector3f s1) {

        // Handling these cases is up to your project's coding standards
        //Debug.Assert(proj_pos != target && proj_speed > 0 && gravity > 0, "de.xenyria.math.trajectory.TrajectoryFormula.solve_ballistic_arc called with invalid data");

        // C# requires out variables be weapons
        s0 = Vector3f.zero();
        s1 = Vector3f.zero();

        // Derivation
        //   (1) x = v*t*cos O
        //   (2) y = v*t*sin O - .5*g*t^2
        //
        //   (3) t = x/(cos O*v)                                        [solve t from (1)]
        //   (4) y = v*x*sin O/(cos O * v) - .5*g*x^2/(cos^2 O*v^2)     [plug t into y=...]
        //   (5) y = x*tan O - g*x^2/(2*v^2*cos^2 O)                    [reduce; cos/sin = tan]
        //   (6) y = x*tan O - (g*x^2/(2*v^2))*(1+tan^2 O)              [reduce; 1+tan O = 1/cos^2 O]
        //   (7) 0 = ((-g*x^2)/(2*v^2))*tan^2 O + x*tan O - (g*x^2)/(2*v^2) - y    [re-arrange]
        //   Quadratic! a*p^2 + b*p + c where p = tan O
        //
        //   (8) let gxv = -g*x*x/(2*v*v)
        //   (9) p = (-x +- sqrt(x*x - 4gxv*(gxv - y)))/2*gxv           [quadratic formula]
        //   (10) p = (v^2 +- sqrt(v^4 - g(g*x^2 + 2*y*v^2)))/gx        [multiply top/bottom by -2*v*v/x; move 4*v^4/x^2 into root]
        //   (11) O = atan(p)

        // out de.xenyria.math.trajectory.Vector3f s0, out de.xenyria.math.trajectory.Vector3f s1
        Vector3f diff = target.clone().subtract(proj_pos);
        Vector3f diffXZ = new Vector3f(diff.x, 0f, diff.z);
        double groundDist = (float) diffXZ.magnitude();

        double speed2 = proj_speed * proj_speed;
        double speed4 = proj_speed * proj_speed * proj_speed * proj_speed;
        double y = diff.y;
        double x = groundDist;
        double gx = gravity * x;

        double root = speed4 - gravity * (gravity * x * x + 2 * y * speed2);

        // No solution
        if (root < 0)
            return new MethodOutput(new Object[]{0, s0, s1});

        root = (float) Math.sqrt(root);

        float lowAng = (float) Math.atan2(speed2 - root, gx);
        float highAng = (float) Math.atan2(speed2 + root, gx);
        int numSolutions = lowAng != highAng ? 2 : 1;

        Vector3f groundDir = diffXZ.clone().normalize();

        // Math.cos(lowAng)*proj_speed

        // s0 = groundDir*Mathf.Cos(lowAng)*proj_speed + Vector3.up*Mathf.Sin(lowAng)*proj_speed;
        Vector3f solutionOne = groundDir.clone();
        solutionOne = solutionOne.multiply((float) (Math.cos(lowAng) * proj_speed));
        solutionOne = solutionOne.add(Vector3f.up().multiply((float) (Math.sin(lowAng) * proj_speed)));

        /*de.xenyria.math.trajectory.Vector3f grdDir = groundDir.clone();
        grdDir = grdDir.multiply((float) Math.cos(lowAng)).multiply(proj_speed);
        grdDir.add(de.xenyria.math.trajectory.Vector3f.up().multiply((float) Math.sin(lowAng)).multiply(proj_speed));
        s0 = grdDir;*/
        s0 = solutionOne;

        if (numSolutions > 1) {

            s1 = groundDir.clone().multiply((float) Math.cos(highAng) * proj_speed);
            s1 = s1.add(Vector3f.up().multiply((float) (Math.sin(highAng) * proj_speed)));

        }

        return new MethodOutput(new Object[]{numSolutions, s0, s1});

    }

    // Solve firing angles for a ballistic projectile with speed and gravity to hit a target moving with constant, linear velocity.
    //
    // proj_pos (de.xenyria.math.trajectory.Vector3f): point projectile will fire from
    // proj_speed (float): scalar speed of projectile
    // target (de.xenyria.math.trajectory.Vector3f): point projectile is trying to hit
    // target_velocity (de.xenyria.math.trajectory.Vector3f): velocity of target
    // gravity (float): force of gravity, positive down
    //
    // s0 (out de.xenyria.math.trajectory.Vector3f): firing solution (fastest time impact)
    // s1 (out de.xenyria.math.trajectory.Vector3f): firing solution (next impact)
    // s2 (out de.xenyria.math.trajectory.Vector3f): firing solution (next impact)
    // s3 (out de.xenyria.math.trajectory.Vector3f): firing solution (next impact)
    //
    // return (int): number of unique solutions found: 0, 1, 2, 3, or 4.
    /*public static de.xenyria.math.trajectory.csharp.MethodOutput solve_ballistic_arc(de.xenyria.math.trajectory.Vector3f proj_pos, float proj_speed, de.xenyria.math.trajectory.Vector3f target_pos, de.xenyria.math.trajectory.Vector3f target_velocity, float gravity, de.xenyria.math.trajectory.Vector3f s0, de.xenyria.math.trajectory.Vector3f s1) {

        // out de.xenyria.math.trajectory.Vector3f s0, out de.xenyria.math.trajectory.Vector3f s1
        // Initialize output parameters
        s0 = de.xenyria.math.trajectory.Vector3f.zero();
        s1 = de.xenyria.math.trajectory.Vector3f.zero();

        // Derivation
        //
        //  For full derivation see: blog.forrestthewoods.com
        //  Here is an abbreviated version.
        //
        //  Four equations, four unknowns (solution.x, solution.y, solution.z, time):
        //
        //  (1) proj_pos.x + solution.x*time = target_pos.x + target_vel.x*time
        //  (2) proj_pos.y + solution.y*time + .5*G*t = target_pos.y + target_vel.y*time
        //  (3) proj_pos.z + solution.z*time = target_pos.z + target_vel.z*time
        //  (4) proj_speed^2 = solution.x^2 + solution.y^2 + solution.z^2
        //
        //  (5) Solve for solution.x and solution.z in equations (1) and (3)
        //  (6) Square solution.x and solution.z from (5)
        //  (7) Solve solution.y^2 by plugging (6) into (4)
        //  (8) Solve solution.y by rearranging (2)
        //  (9) Square (8)
        //  (10) Set (8) = (7). All solution.xyz terms should be gone. Only time remains.
        //  (11) Rearrange 10. It will be of the form a*^4 + b*t^3 + c*t^2 + d*t * e. This is a quartic.
        //  (12) Solve the quartic using SolveQuartic.
        //  (13) If there are no positive, real roots there is no solution.
        //  (14) Each positive, real root is one valid solution
        //  (15) Plug each time value into (1) (2) and (3) to calculate solution.xyz
        //  (16) The end.

        double G = gravity;

        double A = proj_pos.x;
        double B = proj_pos.y;
        double C = proj_pos.z;
        double M = target_pos.x;
        double N = target_pos.y;
        double O = target_pos.z;
        double P = target_velocity.x;
        double Q = target_velocity.y;
        double R = target_velocity.z;
        double S = proj_speed;

        double H = M - A;
        double J = O - C;
        double K = N - B;
        double L = -.5f * G;

        // Quartic Coeffecients
        double c0 = L*L;
        double c1 = 2*Q*L;
        double c2 = Q*Q + 2*K*L - S*S + P*P + R*R;
        double c3 = 2*K*Q + 2*H*P + 2*J*R;
        double c4 = K*K + H*H + J*J;

        // Solve quartic
        double[] times = new double[4];
        int numTimes = SolveQuartic(c0, c1, c2, c3, c4, out times[0], out times[1], out times[2], out times[3]);

        // Sort so faster collision is found first
        System.Array.Sort(times);

        // Plug quartic solutions into base equations
        // There should never be more than 2 positive, real roots.
        de.xenyria.math.trajectory.Vector3f[] solutions = new de.xenyria.math.trajectory.Vector3f[2];
        int numSolutions = 0;

        for (int i = 0; i < numTimes && numSolutions < 2; ++i) {
            double t = times[i];
            if ( t <= 0)
                continue;

            solutions[numSolutions].x = (float)((H+P*t)/t);
            solutions[numSolutions].y = (float)((K+Q*t-L*t*t)/ t);
            solutions[numSolutions].z = (float)((J+R*t)/t);
            ++numSolutions;
        }

        // Write out solutions
        if (numSolutions > 0)   s0 = solutions[0];
        if (numSolutions > 1)   s1 = solutions[1];

        return numSolutions;
    }*/



    // Solve the firing arc with a fixed lateral speed. Vertical speed and gravity varies.
    // This enables a visually pleasing arc.
    //
    // proj_pos (de.xenyria.math.trajectory.Vector3f): point projectile will fire from
    // lateral_speed (float): scalar speed of projectile along XZ plane
    // target_pos (de.xenyria.math.trajectory.Vector3f): point projectile is trying to hit
    // max_height (float): height above Max(proj_pos, impact_pos) for projectile to peak at
    //
    // fire_velocity (out de.xenyria.math.trajectory.Vector3f): firing velocity
    // gravity (out float): gravity necessary to projectile to hit precisely max_height
    //
    // return (bool): true if a valid solution was found
    public static MethodOutput solve_ballistic_arc_lateral(Vector3f proj_pos, float lateral_speed, Vector3f target_pos, float max_height, Vector3f fire_velocity, float gravity) {

        // out de.xenyria.math.trajectory.Vector3f fire_velocity, out float gravity

        // Handling these cases is up to your project's coding standards
        //Debug.Assert(proj_pos != target_pos && lateral_speed > 0 && max_height > proj_pos.y, "de.xenyria.math.trajectory.TrajectoryFormula.solve_ballistic_arc called with invalid data");

        fire_velocity = Vector3f.zero();
        gravity = Float.NaN;

        Vector3f diff = target_pos.clone().subtract(proj_pos);
        Vector3f diffXZ = new Vector3f(diff.x, 0f, diff.z);
        float lateralDist = (float) diffXZ.magnitude();

        if (lateralDist == 0)
            return new MethodOutput(new Object[]{false, fire_velocity, gravity});

        float time = lateralDist / lateral_speed;

        fire_velocity = diffXZ.normalize().multiply(lateral_speed);

        // System of equations. Hit max_height at t=.5*time. Hit target at t=time.
        //
        // peak = y0 + vertical_speed*halfTime + .5*gravity*halfTime^2
        // end = y0 + vertical_speed*time + .5*gravity*time^s
        // Wolfram Alpha: solve b = a + .5*v*t + .5*g*(.5*t)^2, c = a + vt + .5*g*t^2 for g, v
        float a = proj_pos.y;       // initial
        float b = max_height;       // peak
        float c = target_pos.y;     // final

        gravity = -4*(a - 2*b + c) / (time* time);
        fire_velocity.y = -(3*a - 4*b + c) / time;

        return new MethodOutput(new Object[]{true, fire_velocity, gravity});
    }

    // Solve the firing arc with a fixed lateral speed. Vertical speed and gravity varies.
    // This enables a visually pleasing arc.
    //
    // proj_pos (de.xenyria.math.trajectory.Vector3f): point projectile will fire from
    // lateral_speed (float): scalar speed of projectile along XZ plane
    // target_pos (de.xenyria.math.trajectory.Vector3f): point projectile is trying to hit
    // max_height (float): height above Max(proj_pos, impact_pos) for projectile to peak at
    //
    // fire_velocity (out de.xenyria.math.trajectory.Vector3f): firing velocity
    // gravity (out float): gravity necessary to projectile to hit precisely max_height
    // impact_point (out de.xenyria.math.trajectory.Vector3f): point where moving target will be hit
    //
    // return (bool): true if a valid solution was found
    public static MethodOutput solve_ballistic_arc_lateral(Vector3f proj_pos, float lateral_speed, Vector3f target, Vector3f target_velocity, float max_height_offset, Vector3f fire_velocity, float gravity, Vector3f impact_point) {

        // Handling these cases is up to your project's coding standards
        //Debug.Assert(proj_pos != target && lateral_speed > 0, "de.xenyria.math.trajectory.TrajectoryFormula.solve_ballistic_arc_lateral called with invalid data");

        // Initialize output variables
        fire_velocity = Vector3f.zero();
        gravity = 0f;
        impact_point = Vector3f.zero();

        // Ground plane terms
        Vector3f targetVelXZ = new Vector3f(target_velocity.x, 0f, target_velocity.z);
        Vector3f diffXZ = target.clone().subtract(proj_pos);
        diffXZ.y = 0;

        // Derivation
        //   (1) Base formula: |P + V*t| = S*t
        //   (2) Substitute variables: |diffXZ + targetVelXZ*t| = S*t
        //   (3) Square both sides: Dot(diffXZ,diffXZ) + 2*Dot(diffXZ, targetVelXZ)*t + Dot(targetVelXZ, targetVelXZ)*t^2 = S^2 * t^2
        //   (4) Quadratic: (Dot(targetVelXZ,targetVelXZ) - S^2)t^2 + (2*Dot(diffXZ, targetVelXZ))*t + Dot(diffXZ, diffXZ) = 0
        float c0 = (float) (Vector3f.dot(targetVelXZ, targetVelXZ) - lateral_speed*lateral_speed);
        float c1 = (float) (2f * Vector3f.dot(diffXZ, targetVelXZ));
        float c2 = (float) Vector3f.dot(diffXZ, diffXZ);
        double t0 = 0d, t1 = 0d;

        double[] solveQuadric = TrajectoryFormula.SolveQuadric(c0, c1, c2, t0, t1);
        int n = (int)solveQuadric[0];
        t0 = solveQuadric[1];
        t1 = solveQuadric[2];

        // pick smallest, positive time
        boolean valid0 = n > 0 && t0 > 0;
        boolean valid1 = n > 1 && t1 > 0;

        float t;
        if (!valid0 && !valid1)
            return new MethodOutput(new Object[]{false, fire_velocity, gravity, impact_point});
        else if (valid0 && valid1)
            t = Math.min((float)t0, (float)t1);
        else
            t = valid0 ? (float)t0 : (float)t1;

        // Calculate impact point
        impact_point = target.add(target_velocity.multiply(t));

        // Calculate fire velocity along XZ plane
        Vector3f dir = impact_point.clone().subtract(proj_pos);
        fire_velocity = new Vector3f(dir.x, 0f, dir.z).normalize().multiply(lateral_speed);

        // Solve system of equations. Hit max_height at t=.5*time. Hit target at t=time.
        //
        // peak = y0 + vertical_speed*halfTime + .5*gravity*halfTime^2
        // end = y0 + vertical_speed*time + .5*gravity*time^s
        // Wolfram Alpha: solve b = a + .5*v*t + .5*g*(.5*t)^2, c = a + vt + .5*g*t^2 for g, v
        float a = proj_pos.y;       // initial
        float b = Math.max(proj_pos.y, impact_point.y) + max_height_offset;  // peak
        float c = impact_point.y;   // final

        gravity = -4*(a - 2*b + c) / (t* t);
        fire_velocity.y = -(3*a - 4*b + c) / t;

        return new MethodOutput(new Object[]{true, fire_velocity, gravity, impact_point});
    }
}