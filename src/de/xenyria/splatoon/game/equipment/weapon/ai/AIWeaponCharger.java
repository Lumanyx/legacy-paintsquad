package de.xenyria.splatoon.game.equipment.weapon.ai;

public interface AIWeaponCharger extends AIWeapon {

    public float nextSprayYaw();
    public float nextSprayPitch();
    public double getRange();
    public long estimatedChargeTimeForTargetDistance(double dist);

}
