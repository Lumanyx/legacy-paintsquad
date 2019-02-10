package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.structure.editor.point.Point;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BallerModel extends WeaponModel {

    public BallerModel(SplatoonPlayer player, Location location, Baller baller) {

        super(player, "baller", location.getWorld(), location);
        this.baller = baller;

    }
    private Baller baller;

    @Override
    public void onTick() {

        float timeMod = baller.usedTimePercentage();
        double delta = (maxY - minY);
        double y = minY + (delta * timeMod);
        for(Map.Entry<Double, ArrayList<Point>> entry : pointYMap.entrySet()) {

            if(entry.getKey() <= y) {

                for (Point point : entry.getValue()) {

                    ArmorStand stand = getStandPointMap().get(point);
                    if (stand.getHelmet().getType() == Material.GLASS) {

                        stand.setHelmet(new ItemStack(baller.getPlayer().getTeam().getColor().getGlass()));

                    }

                }

            }

        }

    }

    private HashMap<Double, ArrayList<Point>> pointYMap = new HashMap<>();

    @Override
    public void remove() {

        super.remove();
        pointYMap.clear();

    }

    private double minY, maxY;
    private boolean ySet = false;

    @Override
    public void spawn() {

        super.spawn();

        for(Point point : getStandPointMap().keySet()) {

            if(!pointYMap.containsKey(point.getY())) {

                double y = point.getY();
                if(!ySet) {

                    ySet = true;
                    minY = y;
                    maxY = y;

                } else {

                    if(y < minY) { minY = y; }
                    if(y > maxY) { maxY = y; }

                }
                ArrayList<Point> points = new ArrayList<>();
                points.add(point);
                pointYMap.put(y, points);

            } else {

                pointYMap.get(point.getY()).add(point);

            }

        }

    }

    @Override
    public double yOffset() {
        return 0;
    }

    @Override
    public void handleSpecialPoint(Point point, ArmorStand stand) {

    }
}
