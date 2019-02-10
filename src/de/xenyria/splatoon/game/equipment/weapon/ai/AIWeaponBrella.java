package de.xenyria.splatoon.game.equipment.weapon.ai;

public interface AIWeaponBrella extends AIWeapon {

    public float[] nextSprayYaw();
    public float[] nextSprayPitch();
    public float getImpulse();

}
