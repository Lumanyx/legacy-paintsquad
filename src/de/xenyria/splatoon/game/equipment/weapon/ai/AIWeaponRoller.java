package de.xenyria.splatoon.game.equipment.weapon.ai;

public interface AIWeaponRoller extends AIWeapon {

    public float[] nextSprayYaw();
    public float[] nextSprayPitch();
    public float getImpulse();
    public boolean isRolling();
    public float getPitchOffset();
    default void onValidFireTick() {}
    int getTicksToRoll();

}
