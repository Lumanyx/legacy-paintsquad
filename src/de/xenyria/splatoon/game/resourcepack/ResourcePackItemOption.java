package de.xenyria.splatoon.game.resourcepack;

public enum ResourcePackItemOption {

    NONE(0),

    DEFAULT_CHARGER(1),
    SCOPED_CHARGER(2),
    ELITER(3),
    SQUIFFER(4),
    HEROCHARGER(5),

    SPLATTERSHOT_JR(2),
    SPLATTERSHOT(1),
    HEROSHOT(3),
    OCTOSHOT(4),
    JET_SQUELCHER(5),
    AEROSPRAY_MG(6),
    AEROSPRAY_RG(7),

    MINI_SPLATLING(1),

    BLASTER(1),
    DUALIES(1),
    HEROROLLER_IDLE(1),
    HEROROLLER_ROLLING(2),
    ROLLER_IDLE(3),
    ROLLER_ROLLING(4),
    CARBON_IDLE(5),
    CARBON_ROLL(6),
    INKBRUSH_IDLE(1),
    INKBRUSH_SURF(2);

    private short damageValue;
    public short getDamageValue() { return damageValue; }

    ResourcePackItemOption(int dmg) {

        this.damageValue = (short) dmg;

    }

}
