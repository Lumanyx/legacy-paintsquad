package de.xenyria.splatoon.game.color;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Random;

public enum Color {

    ORANGE("Orange", '6', org.bukkit.Color.ORANGE, Material.ORANGE_STAINED_GLASS, Material.ORANGE_WOOL, Material.ORANGE_TERRACOTTA, Material.ORANGE_CARPET, Material.ORANGE_CONCRETE),
    BLUE("Blau", '1', org.bukkit.Color.BLUE, Material.BLUE_STAINED_GLASS, Material.BLUE_WOOL, Material.BLUE_TERRACOTTA, Material.BLUE_CARPET, Material.BLUE_CONCRETE),
    GREEN("Grün", 'a', org.bukkit.Color.LIME, Material.LIME_STAINED_GLASS, Material.LIME_WOOL, Material.LIME_TERRACOTTA, Material.LIME_CARPET, Material.LIME_CONCRETE),
    Pink("Pink", 'd', org.bukkit.Color.fromRGB(254, 68, 125), Material.PINK_STAINED_GLASS, Material.PINK_WOOL, Material.PINK_TERRACOTTA, Material.PINK_CARPET, Material.PINK_CONCRETE);

    public static ArrayList<Color> getRandomColors(int amount) {

        ArrayList<Color> colors = new ArrayList<>();
        while (colors.size() < amount) {

            Color clr = values()[new Random().nextInt(values().length - 1)];
            if(!colors.contains(clr)) { colors.add(clr); }

        }
        return colors;

    }

    private String name;
    public String getName() { return name; }

    private char color;
    public char getColor() { return color; }

    private org.bukkit.Color bukkitColor;
    public org.bukkit.Color getBukkitColor() { return bukkitColor; }

    Color(String name, char c, org.bukkit.Color color, Material glass, Material wool, Material clay, Material carpet, Material sponge) {

        this.name = name;
        this.color = c;
        this.bukkitColor = color;
        this.glass = glass;
        this.wool = wool;
        this.clay = clay;
        this.carpet = carpet;
        this.sponge = sponge;

    }

    public String prefix() { return "§" + color; }

    private Material glass, wool, clay, carpet, sponge;
    public Material getGlass() { return glass; }
    public Material getWool() { return wool; }
    public Material getClay() { return clay; }
    public Material getCarpet()  { return carpet; }
    public Material getSponge() { return sponge; }
}
