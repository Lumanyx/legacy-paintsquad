package de.xenyria.splatoon.game.projectile;

import de.xenyria.core.chat.Characters;

public enum DamageReason {

    WEAPON("Mit %WEAPON% erledigt.", Characters.SMALL_X.charAt(0)),
    HUMAN_ERROR("Du hast dich selbst erledigt.", '✖'),
    SPAWN_BARRIER("Von Spawnbarriere erledigt", '!');

    private String description;
    public String getDescription() { return description; }

    private char icon;
    public char getIcon() { return icon; }

    DamageReason(String description, char icon) {

        this.icon = icon;
        this.description = description;

    }

}
