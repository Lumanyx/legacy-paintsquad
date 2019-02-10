package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractRoller;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.structure.editor.point.Point;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

public class RollerWeaponModel extends WeaponModel {

    public RollerWeaponModel(SplatoonPlayer player, String rollerModel, World world, Location location) {
        super(player, rollerModel, world, location);
        useLagCompensation(true);
        setNonResourcepackExclusive(true);

    }

    boolean colorsSet = false;

    @Override
    public void onTick() {

        if(getPlayer().getEquipment().getPrimaryWeapon() != null) {

            SplatoonPrimaryWeapon weapon = getPlayer().getEquipment().getPrimaryWeapon();
            if(weapon instanceof AbstractRoller) {

                AbstractRoller roller = (AbstractRoller) weapon;
                if(!roller.isRolling()) {

                    if(isActive()) {

                        remove();

                    }

                } else {

                    if(!isActive() && roller.isSelected()) {

                        spawn();

                    }

                    moveToPosition(getPlayer().getLocation().getX(),
                            getPlayer().getLocation().getY(),
                            getPlayer().getLocation().getZ());
                    rotateTowards(getPlayer().getLocation().getYaw() - 180f);


                }

            }

        }

    }

    @Override
    public double yOffset() {
        return 0.7;
    }

    @Override
    public void handleSpecialPoint(Point point, ArmorStand stand) {

        stand.setHelmet(new ItemStack(getPlayer().getTeam().getColor().getWool()));

    }

}
