package de.xenyria.splatoon.game.equipment.weapon.ai;

public interface AIWeaponCharger extends AIWeapon {

    public float nextSprayYaw();
    public float nextSprayPitch();
    public double maxChargeDistance();
    public long estimatedChargeTimeForTargetDistance();

}
