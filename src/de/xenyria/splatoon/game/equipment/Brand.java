package de.xenyria.splatoon.game.equipment;

import de.xenyria.core.chat.Characters;
import de.xenyria.core.chat.Chat;

public enum Brand {

    PROTO("Prototyp", "PR0T0", "§e§l[PR0T0] "),
    TAKOROKA("Kalamati", "§1ED>", "§8[§9Kalamati§8] §7"),
    NOT_BRANDED("Markenlos", "", ""),
    SQUID_FORCE("SquidForce", "§8" + Characters.BIG_X + " SqF.", "§7§l[SquidForce] "),
    CUTTLEGEAR("Cuttlegear", "§0<コ:彡 §fCuG.", "§0§l[§7Cuttlegear§0§l] §7"),
    FORGE("FORGE", "§e§o§lF§8§o§lorge§r", "§6[Forge] "),
    KRAKON("Krak-On", "§9♠ Krak-On", "§e[§1Krak-On§e] §e"),
    ROCKENBERG("Rockenberg", "§2|~","§8[§2Rockenberg§8] §2"),
    FIREFIN("Rilax", "§c♠", "§4[§cRilax§4] §c"),
    SKALOP("Jakomu", "§8(§f" + Characters.TRIANGLE_RIGHT + Characters.TRIANGLE_LEFT + "§8)", "§8[§fJakomu§8] §7"),
    XENYRIA("Xenyria", "§a" + Chat.SERVER_NAME.toCharArray()[0], "§8[" + "§a" + Chat.SERVER_NAME.toCharArray()[0] + "§8] §7"),
    INKLINE("Alpomar", "§3" + Characters.TRIANGLE_UP, "§8[§3Alpomar§8] §b"),
    SPLASH_MOB("Gian", "§7GIAN", "§8[§7GIAN§8] §f"),
    ZINK("Sagitron", "§1" + Characters.ARROW_RIGHT_FROM_TOP, "§8[§9Sagitron§8] §3");


    private String name;

    public String getName() {
        return name;
    }

    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    private String icon;

    public String getIcon() {
        return icon;
    }

    Brand(String name, String icon, String displayName) {

        this.name = name;
        this.icon = icon;
        this.displayName = displayName;

    }

    }
