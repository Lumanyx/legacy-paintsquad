package de.xenyria.splatoon.lobby.npc;

import de.xenyria.splatoon.game.color.Color;

import java.util.UUID;

public class RecentPlayer {

    private String username;
    private UUID uuid;
    private String textureValue,signature;
    private Color color;
    public Color getColor() { return color; }

    public RecentPlayer(Color color, String username, UUID uuid, String textureValue, String signature, int weaponSetID, int helmet, int chestplate, int boots) {

        this.color = color;
        this.username = username;
        this.uuid = uuid;
        this.textureValue = textureValue;
        this.signature = signature;
        this.weaponSetID = weaponSetID;
        this.helmetID = helmet;
        this.chestplateID = chestplate;
        this.bootsID = boots;

    }

    private int helmetID,chestplateID,bootsID;

    private int weaponSetID = 1;
    public int getWeaponSetID() { return weaponSetID; }

    public String getName() { return username; }

    public String getTexture() { return textureValue; }
    public String getSignature() { return signature; }

    public int getHelmetID() { return helmetID; }
    public int getChestplateID() { return chestplateID; }
    public int getBootsID() { return bootsID; }

}
