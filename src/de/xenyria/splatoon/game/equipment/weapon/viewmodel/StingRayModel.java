package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.core.math.SlowRotation;
import de.xenyria.splatoon.game.equipment.weapon.special.stingray.StingRay;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.structure.editor.point.Point;
import de.xenyria.util.math.LocalCoordinateSystem;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class StingRayModel extends WeaponModel {

    private StingRay ray;

    public StingRayModel(SplatoonPlayer player, Location location, StingRay ray) {

        super(player, "stingray", location.getWorld(), location);
        this.ray = ray;

    }

    private SlowRotation rotation = new SlowRotation();
    private SlowRotation pitchRotation = new SlowRotation();

    public static final float turningSpeed = 1.2f;

    @Override
    public void onTick() {

        float yaw = getPlayer().yaw();
        while (yaw < 0) { yaw+=360; }

        rotation.target(yaw);
        pitchRotation.target(getPlayer().getLocation().getPitch() + 90f);

        if(!rotation.isReached()) {

            rotation.rotate(turningSpeed);

        }
        if(!pitchRotation.isReached()) {

            pitchRotation.rotate(turningSpeed);

        }

        rotateTowards(rotation.getAngle());
        moveToPosition(getPlayer().getLocation().getX(),
                getPlayer().getLocation().getY() + yOffset(),
                getPlayer().getLocation().getZ());

    }

    @Override
    public double yOffset() {
        return 0.30;
    }

    private Point noozlePoint;
    public Location noozleLocation() {

        Location location = new Location(getPlayer().getWorld(), 0,0,0);
        location = location.add(getPlayer().getLocation().clone());

        LocalCoordinateSystem lcs = new LocalCoordinateSystem();
        lcs.calculate(0, getRotation(), 0, .625);
        location = location.add(lcs.getForward().clone().multiply(noozlePoint.getX())).add(lcs.getSideways().clone().multiply(noozlePoint.getZ())).add(lcs.getUpwards().clone().multiply(noozlePoint.getY()));
        location.setYaw(rotation.getAngle());
        location.setPitch(pitchRotation.getAngle() - 90f);
        return location;

    }


    @Override
    public void handleSpecialPoint(Point point, ArmorStand stand) {

        this.noozlePoint = point;
        resetPosition();

    }

    public void resetPosition() {

        rotation.updateAngle(getPlayer().getLocation().getYaw());
        pitchRotation.updateAngle(getPlayer().getLocation().getPitch() + 90f);


    }

    public void spawn() {

        super.spawn();

    }

}
