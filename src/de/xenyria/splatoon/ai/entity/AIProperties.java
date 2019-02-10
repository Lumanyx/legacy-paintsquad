package de.xenyria.splatoon.ai.entity;

public class AIProperties {

    // Aggressivität (0-100), Genauigkeit, Waffenumgangsfähigkeit
    private double aggressiveness, accuracy, weaponHandling;
    public double getAggressiveness() { return aggressiveness; }
    public double getAccuracy() { return accuracy; }
    public double getWeaponHandling() { return weaponHandling; }

    public AIProperties(double aggressiveness, double accuracy, double weaponHandling) {

        this.aggressiveness = aggressiveness;
        this.accuracy = accuracy;
        this.weaponHandling = weaponHandling;

    }

}
