package de.xenyria.splatoon.game.equipment.weapon.ai;

import de.xenyria.math.trajectory.Trajectory;

public interface AIWeaponShooter extends AIWeapon {

    public void onValidFireTick();
    public float nextSprayYaw();
    public float nextSprayPitch();
    public float getImpulse();

}
