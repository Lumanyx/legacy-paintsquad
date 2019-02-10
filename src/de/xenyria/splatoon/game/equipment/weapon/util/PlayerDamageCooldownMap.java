package de.xenyria.splatoon.game.equipment.weapon.util;

import de.xenyria.splatoon.game.player.SplatoonPlayer;

import java.util.HashMap;

public class PlayerDamageCooldownMap {

    private HashMap<SplatoonPlayer, Long> lastDamage = new HashMap<>();
    public void registerDamage(SplatoonPlayer player) {

        lastDamage.put(player, System.currentTimeMillis());

    }
    public long lastDamage(SplatoonPlayer player) {

        return System.currentTimeMillis() - lastDamage.getOrDefault(player, 0l);

    }

}
