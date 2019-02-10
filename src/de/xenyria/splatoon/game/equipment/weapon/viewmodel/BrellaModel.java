package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrella;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.structure.editor.point.Point;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

public class BrellaModel extends WeaponModel {

    private AbstractBrella brella;

    public BrellaModel(SplatoonPlayer player, Location location, AbstractBrella brella) {

        super(player, "brella", location.getWorld(), location);
        this.brella = brella;
        useInvisibilityInsteadOfRemoval();

    }

    @Override
    public void onTick() {

    }

    @Override
    public double yOffset() {
        return 0;
    }

    @Override
    public void handleSpecialPoint(Point point, ArmorStand stand) {

        stand.setHelmet(new ItemStack(brella.getPlayer().getTeam().getColor().getCarpet()));

    }

}
