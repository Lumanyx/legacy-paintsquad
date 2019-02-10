package de.xenyria.splatoon.game.equipment.weapon.ai;

import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.player.SplatoonPlayer;

public interface AIWeapon {

    SplatoonPlayer getPlayer();

    default boolean assignedToNPC() {

        return getPlayer() instanceof EntityNPC;

    }

}
