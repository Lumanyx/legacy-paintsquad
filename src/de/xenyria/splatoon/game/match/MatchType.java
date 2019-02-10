package de.xenyria.splatoon.game.match;

public enum MatchType {

    TURF_WAR("Revierkampf", "Färbe möglichst viel ein!", "§7Färbe zusammen mit deinem Team die Arena ein.\n§7Nach §edrei Minuten §7siegt das Team,\n§7dass die meiste Fläche einfärbt.", "Färben, färben und noch mehr färben!"),
    TUTORIAL("Tutorial", "Tutorial", "Tutorial", "Tutorial"),
    SHOOTING_RANGE("Waffentest", "Waffentest", "Waffentest", "Waffentest");

    private String title;
    public String getTitle() { return title; }

    private String description;
    public String getDescription() { return description; }

    private String longDescription;
    public String getLongDescription() { return longDescription; }

    private String matchBeginText;
    public String getMatchBeginText() { return matchBeginText; }

    MatchType(String title, String description, String longDescription, String matchBeginText) {

        this.title = title;
        this.description = description;
        this.longDescription = longDescription;
        this.matchBeginText = matchBeginText;

    }

}
