package de.xenyria.splatoon.game.equipment.weapon.special.tentamissles;

import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.DataWatcher;
import net.minecraft.server.v1_13_R2.Entity;

import java.util.UUID;

public interface TentaMissleTarget {

    boolean isTargetable();
    LocationProvider getTargetLocationProvider();
    UUID getUUID();
    int getEntityID();
    String getName();
    Team getTeam();
    DataWatcher getDataWatcher();
    Entity getNMSEntity();

}
