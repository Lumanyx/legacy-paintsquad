package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.structure.editor.point.Point;
import org.bukkit.entity.ArmorStand;

public class JetpackModel extends WeaponModel {

    private Jetpack jetpack;
    public JetpackModel(SplatoonPlayer player, Jetpack jetpack) {

        super(player, "jetpack", player.getWorld(), player.getLocation());
        this.jetpack = jetpack;
        useLagCompensation(false);

    }

    @Override
    public void onTick() {

    }

    @Override
    public double yOffset() {
        return 0;
    }

    private int specialPointIDTicker;

    @Override
    public void handleSpecialPoint(Point point, ArmorStand stand) {

        if(specialPointIDTicker > 0) {

            jetpack.getParticleSources().add(point);

        }

        specialPointIDTicker++;
        if(specialPointIDTicker > 2) { specialPointIDTicker = 0; }

    }

}
