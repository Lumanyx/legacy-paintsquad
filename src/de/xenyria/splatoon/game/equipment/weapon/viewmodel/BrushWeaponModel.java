package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.Location;
import org.bukkit.World;

public class BrushWeaponModel extends RollerWeaponModel {

    public BrushWeaponModel(SplatoonPlayer player, World world, Location location) {
        super(player, "brush", world, location);
    }

}
