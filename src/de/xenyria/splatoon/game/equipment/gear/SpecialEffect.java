package de.xenyria.splatoon.game.equipment.gear;

import de.xenyria.api.spigot.ItemBuilder;
import org.bukkit.Color;
import org.bukkit.Material;

public enum SpecialEffect {

    RUN_SPEED_UP("§dLaufgeschwindigkeit+", "§dRun+", "§7Deine Laufgeschwindigkeit erhöht sich.", true,
            new ItemBuilder(Material.LEATHER_BOOTS).addColor(Color.fromRGB(239, 14, 228)), Color.fromRGB(239, 14, 228)),

    INK_RECOVERY("§aTintenregeneration+", "§aInk+", "§7Der Tintentank füllt sich beim schwimmen\n§7in eigener Tinte schneller auf.", true,
            new ItemBuilder(Material.POTION).addColor(Color.LIME), Color.LIME),

    FAST_RESPAWN("§aSchneller Respawn", "§3Respawn+", "§7Nach einem Tod steigst du schneller\n§7wieder in's Spiel ein.",true,
            new ItemBuilder(Material.SPAWNER), Color.fromRGB(79, 246, 255)),

    SPECIAL_LOSE("§bSpezialverlust-", "§bS.Verlust-", "§7Die Spezialwaffe wird nach einem Tod\n§7weniger entladen und kann somit wieder\n§7schneller erreicht werden.", true,
            new ItemBuilder(Material.POTION).addColor(Color.fromRGB(79, 246, 255)), Color.fromRGB(79, 246, 255)),

    INK_SAVER_MAIN("§6Hauptverbrauch-", "§6M.Konsum-", "§7Primärwaffen verbrauchen weniger Tinte.", true,
            new ItemBuilder(Material.COAL), Color.fromRGB(255, 202, 79)),

    INK_SAVER_SUB("§1Sekundärverbrauch-", "§1S.Konsum-", "§7Sekundärwaffen verbrauchen weniger Tinte.", true,
            new ItemBuilder(Material.SUGAR), Color.fromRGB(41, 56, 229)),

    SWIM_SPEED_UP("§dSchwimmgeschwindigkeit+", "§dSwim+", "§7Das Schwimmen in eigener Tinte\n§7wird beschleunigt.", true,
            new ItemBuilder(Material.INK_SAC), Color.fromRGB(175, 7, 125)),

    TENACITY("§aZätigkeit", "§aZätigkeit", "§7Liegt dein Team stark zurück so\n§7füllt sich die Spezialwaffe langsam\n§7von selbst auf.", false,
            new ItemBuilder(Material.SQUID_SPAWN_EGG), Color.fromRGB(8, 1, 48)),

    SPECIAL_CHARGE_UP("§bSpezial+", "§bSpec+", "§7Deine Spezialwaffe wird schneller aufgeladen.", true,
            new ItemBuilder(Material.NETHER_STAR), Color.fromRGB(66, 172, 188)),

    INK_RESISTANCE_UP("§6Tintentoleranz+", "§6Resist+", "§7Du erleidest weniger Schaden\n§7in gegnerischer Tinte und kannst\n§7dich besser bewegen.", true,
            new ItemBuilder(Material.LEATHER_CHESTPLATE).addColor(Color.BLACK), Color.BLACK),

    BOMB_DEFENSE_UP("§eBombenschutz+", "§eBombDef+", "§7Du erleidest weniger Schaden durch Bomben.", true,
            new ItemBuilder(Material.BARRIER), Color.fromRGB(151, 66, 188)),

    STEALTH_JUMP("§fSprunginfiltration", "§fStealthJump", "§7Der Supersprung wird langsamer.\n§7Gegner sehen jedoch dein Sprungziel nicht.", false,
            new ItemBuilder(Material.POTION).addColor(Color.WHITE), Color.WHITE),

    SUPER_JUMP("§6Supersprung+", "§6Jump+", "§7Der Supersprung wird schneller.", true, new ItemBuilder(Material.BEACON), Color.ORANGE);

    private String name;
    public String getName() { return name; }

    private String shortName;
    public String getShortName() { return shortName; }

    private String description;
    public String getDescription() { return description; }

    private boolean sub;
    public boolean isSub() { return sub; }

    private Color color;
    public Color getColor() { return color; }

    private ItemBuilder builder;
    public ItemBuilder getBuilder() { return builder.clone(); }

    SpecialEffect(String name, String shortName, String description, boolean sub, ItemBuilder baseBuilderData, Color color) {

        this.name = name;
        this.shortName = shortName;
        this.sub = sub;
        this.description = description;
        this.builder = baseBuilderData.addAttributeHider();
        this.color = color;

    }

}
