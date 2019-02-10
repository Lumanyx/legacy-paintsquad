package de.xenyria.splatoon.game.equipment.weapon.secondary;

import org.bukkit.Material;

public enum SecondaryWeaponType {

    GRENADE("Bombe", "Explodiert nach Kontakt mit einer Fläche oder einem Feind.\nFärbt einen kleinen Teil der Fläche ein.", Material.GUNPOWDER),
    SPRINKLER("Sprinkler", "Kann an Böden, Decken und Wänden befestigt werden.\nFärbt selbstständig Fläche ein.\nEs kann maximal ein Sprinkler platziert werden.", Material.HOPPER),
    JUMPPOINT("Sprungboje", "Mobiler Sprungpunkt für deine Mitspieler.\nEs kann maximal eine Sprungboje platziert werden.", Material.BEACON);

    private String name;
    public String getName() { return name; }

    private String description;
    public String getDescription() { return description; }

    private Material representiveMaterial;
    public Material getRepresentiveMaterial() { return representiveMaterial; }

    SecondaryWeaponType(String name, String description, Material representiveMaterial) {

        this.name = name;
        this.description = description;
        this.representiveMaterial = representiveMaterial;

    }

}
