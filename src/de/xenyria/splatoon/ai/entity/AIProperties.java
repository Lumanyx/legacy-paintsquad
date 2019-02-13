package de.xenyria.splatoon.ai.entity;

import org.bukkit.Material;

public class AIProperties {

    public static enum Difficulty {

        EASY("§aLeicht", Material.LIME_CONCRETE_POWDER, new AIProperties(30, 40, 70)),
        MEDIUM("§eMittel", Material.YELLOW_CONCRETE_POWDER, new AIProperties(60, 80, 85)),
        HARD("§cSchwer", Material.RED_CONCRETE_POWDER, new AIProperties(100, 100, 100));

        private String name; private Material material;
        private AIProperties properties;
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public AIProperties getProperties() { return properties; }

        Difficulty(String name, Material material, AIProperties properties) {

            this.name = name;
            this.material = material;
            this.properties = properties;

        }

    }

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
