package de.xenyria.splatoon.game.equipment.weapon.ai;

public interface AIWeaponBlaster {

    public float nextSprayYaw();
    public float nextSprayPitch();
    public float getImpulse();
    public double distanceToDetonation();

}
