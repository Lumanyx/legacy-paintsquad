package de.xenyria.splatoon.game.equipment.weapon;

import de.xenyria.core.math.AngleUtil;
import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.game.util.VectorUtil;
import de.xenyria.util.math.LocalCoordinateSystem;
import org.bukkit.util.Vector;

public class WeaponPositionOffset {

    private boolean leftHand;
    private double depth,offsetX,offsetY,offsetZ;

    public WeaponPositionOffset(boolean leftHand, double depth, double offsetX, double offsetY, double offsetZ) {

        this.leftHand = leftHand;
        this.depth = depth;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;

    }

    public Vector getOffset(float yaw, float pitch) {

        Vector direction = DirectionUtil.yawAndPitchToDirection(yaw, pitch);
        Vector straight = direction.clone();
        Vector upwards = DirectionUtil.yawAndPitchToDirection(yaw, pitch + 90f);
        Vector sideways = DirectionUtil.yawAndPitchToDirection(yaw + 90f, pitch);

        Vector depthOffset = direction.clone().multiply(depth);

        LocalCoordinateSystem lcs = new LocalCoordinateSystem();
        lcs.calculate(-pitch, yaw, 0, 1);

        return depthOffset.add(straight.multiply(offsetX).add(upwards.multiply(offsetY).add(sideways.multiply(offsetZ))));

    }

}
