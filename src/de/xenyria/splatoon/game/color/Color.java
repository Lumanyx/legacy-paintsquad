package de.xenyria.splatoon.game.color;

import de.xenyria.splatoon.XenyriaSplatoon;
import net.minecraft.server.v1_13_R2.IBlockData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

import java.util.ArrayList;
import java.util.Random;

public enum Color {

    ORANGE("Orange", '6', org.bukkit.Color.ORANGE, Material.ORANGE_STAINED_GLASS, Material.ORANGE_WOOL, Material.ORANGE_TERRACOTTA, Material.ORANGE_CARPET, Material.ORANGE_CONCRETE),
    BLUE("Blau", '1', org.bukkit.Color.BLUE, Material.BLUE_STAINED_GLASS, Material.BLUE_WOOL, Material.BLUE_TERRACOTTA, Material.BLUE_CARPET, Material.BLUE_CONCRETE),
    GREEN("Grün", '2', org.bukkit.Color.LIME, Material.LIME_STAINED_GLASS, Material.LIME_WOOL, Material.LIME_TERRACOTTA, Material.LIME_CARPET, Material.LIME_CONCRETE),
    PINK("Pink", 'd', org.bukkit.Color.fromRGB(254, 68, 125), Material.PINK_STAINED_GLASS, Material.PINK_WOOL, Material.PINK_TERRACOTTA, Material.PINK_CARPET, Material.PINK_CONCRETE),
    YELLOW("Gelb", 'e', org.bukkit.Color.fromRGB(254, 220, 12), Material.YELLOW_STAINED_GLASS, Material.YELLOW_WOOL, Material.YELLOW_TERRACOTTA, Material.YELLOW_CARPET, Material.YELLOW_CONCRETE),
    RED("Rot", 'c', org.bukkit.Color.fromRGB(248, 76, 0), Material.RED_STAINED_GLASS, Material.RED_WOOL, Material.RED_TERRACOTTA, Material.RED_CARPET, Material.RED_CONCRETE),
    PURPLE("Lila", '5', org.bukkit.Color.fromRGB(146, 8, 231), Material.PURPLE_STAINED_GLASS, Material.PURPLE_WOOL, Material.PURPLE_TERRACOTTA, Material.PURPLE_CARPET, Material.PURPLE_CONCRETE),
    MINT("Minzgrün", 'a', org.bukkit.Color.fromRGB(137, 207, 158), Material.GREEN_STAINED_GLASS, Material.GREEN_WOOL, Material.GREEN_TERRACOTTA, Material.GREEN_CARPET, Material.GREEN_CONCRETE),
    LIGHT_BLUE("Hellblau", 'b', org.bukkit.Color.fromRGB(10, 205, 254), Material.LIGHT_BLUE_STAINED_GLASS, Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_TERRACOTTA, Material.LIGHT_BLUE_CARPET, Material.LIGHT_BLUE_CONCRETE),
    CYAN("Türkis", '3', org.bukkit.Color.fromRGB(3, 193, 205), Material.CYAN_STAINED_GLASS, Material.CYAN_WOOL, Material.CYAN_TERRACOTTA, Material.CYAN_CONCRETE, Material.CYAN_CARPET),
    MAGENTA("Magenta", '5', org.bukkit.Color.fromRGB(178, 28, 161), Material.MAGENTA_STAINED_GLASS, Material.MAGENTA_WOOL, Material.MAGENTA_TERRACOTTA, Material.MAGENTA_CONCRETE, Material.MAGENTA_CARPET);

    public short tankDurabilityValue() {

        int val = 0;
        for(Color color : values()) {

            if(color != this) {

                val++;

            } else {

                break;

            }

        }

        return (short)(val+1);

    }

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

    private static String hexify(int i) {

        String hexStr = Integer.toHexString(i);
        if(hexStr.length()<2) { hexStr="0"+hexStr; }
        return hexStr;

    }

    Color(String name, char c, org.bukkit.Color color, Material glass, Material wool, Material clay, Material carpet, Material sponge) {

        this.name = name;
        this.color = c;
        this.bukkitColor = color;
        this.glass = glass;
        this.wool = wool;
        this.carpet = carpet;
        this.clay = clay;
        this.sponge = sponge;

        String hexStr = "#";
        hexStr+=hexify(color.getRed());
        hexStr+=hexify(color.getGreen());
        hexStr+=hexify(color.getBlue());
        chatColor = ChatColor.getByChar(c);

        XenyriaSplatoon.getXenyriaLogger().log("Farbe §" + c + name + " §7registriert. RGB: " + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + " | " + hexStr);
        woolData = CraftBlockData.newData(wool, "").getState();

    }

    private IBlockData woolData;

    public String prefix() { return "§" + color; }

    private Material glass, wool, clay, carpet, sponge;
    public Material getGlass() { return glass; }
    public Material getWool() { return wool; }
    public Material getClay() { return clay; }
    public Material getCarpet()  { return carpet; }
    public Material getSponge() { return sponge; }

    private ChatColor chatColor;
    public ChatColor getChatColor() { return chatColor; }

    public IBlockData getWoolData() { return woolData; }

}
