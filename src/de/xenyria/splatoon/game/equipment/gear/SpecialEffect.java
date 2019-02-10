package de.xenyria.splatoon.game.equipment.gear;

public enum SpecialEffect {

    RUN_SPEED_UP("§dLaufgeschwindigkeit+", "§dRun+", "§7Deine Laufgeschwindigkeit erhöht sich.", true),
    INK_RECOVERY("§aTintenregeneration+", "§aInk+", "§7Der Tintentank füllt sich beim schwimmen\n§7in eigener Tinte schneller auf.", true),
    FAST_RESPAWN("§aSchneller Respawn", "§aRespawn+", "§7Nach einem Tod steigst du schneller\n§7wieder in's Spiel ein.",true),
    SPECIAL_LOSE("§bSpezialverlust-", "§bVerlust-", "§7Die Spezialwaffe wird nach einem Tod\n§7weniger entladen und kann somit wieder\n§7schneller erreicht werden.", true),
    INK_SAVER_MAIN("§6Hauptverbrauch-", "§6M.Konsum-", "§7Primärwaffen verbrauchen weniger Tinte.", true),
    INK_SAVER_SUB("§1Sekundärverbrauch-", "§1S.Konsum-", "§7Sekundärwaffen verbrauchen weniger Tinte.", true),
    SWIM_SPEED_UP("§dSchwimmgeschwindigkeit+", "§dSwim+", "§7Das Schwimmen in eigener Tinte\n§7wird beschleunigt.", true),
    TENACITY("§aZätigkeit", "§aZätigkeit", "§7Liegt dein Team stark zurück so\n§7füllt sich die Spezialwaffe langsam\n§7von selbst auf.", false),
    SPECIAL_CHARGE_UP("§bSpezial+", "§bSpec+", "§7Deine Spezialwaffe wird schneller aufgeladen.", true),
    INK_RESISTANCE_UP("§6Tintentoleranz+", "§6Resist+", "§7Du erleidest weniger Schaden\n§7in gegnerischer Tinte und kannst\n§7dich besser bewegen.", true),
    BOMB_DEFENSE_UP("§eBombenschutz+", "§eBombDef+", "§7Du erleidest weniger Schaden durch Bomben.", true),
    STEALTH_JUMP("§fSprunginfiltration", "§fStealthJump", "§7Der Supersprung wird langsamer.\n§7Gegner sehen jedoch dein Sprungziel nicht.", false),
    SUPER_JUMP("§6Supersprung+", "§6Jump+", "§7Der Supersprung wird schneller.", true);

    private String name;
    public String getName() { return name; }

    private String shortName;
    public String getShortName() { return shortName; }

    private String description;
    public String getDescription() { return description; }

    private boolean sub;
    public boolean isSub() { return sub; }

    SpecialEffect(String name, String shortName, String description, boolean sub) {

        this.name = name;
        this.shortName = shortName;
        this.sub = sub;
        this.description = description;

    }

}
