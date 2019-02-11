package de.xenyria.splatoon.game.equipment.weapon.set;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.DebugBrush;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.Splattershot;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.BurstBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.SplatBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.SprinklerSecondary;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WeaponSetRegistry {

    private static HashMap<Integer, WeaponSet> sets = new HashMap<>();
    public static WeaponSet getSet(int id) {

        return sets.getOrDefault(id, sets.get(1));

    }

    public WeaponSetRegistry() {

        sets.put(1, new WeaponSet(1, "Junior-Kleckser", 0, 21, 2, 22));
        sets.put(2, new WeaponSet(2, "Kleckser", 1200, Splattershot.ID, BurstBomb.ID, Splashdown.ID));
        sets.put(3, new WeaponSet(3, "Tentatek Kleckser", 2100, Splattershot.ID, SplatBomb.ID, Jetpack.ID));
        sets.put(4, new WeaponSet(4, "Quasto", 2600, DebugBrush.ID, SprinklerSecondary.ID, Splashdown.ID));

    }

    public static ArrayList<WeaponSet> getSets(PrimaryWeaponType category) {

        ArrayList<WeaponSet> setList = new ArrayList<>();
        for(Map.Entry<Integer, WeaponSet> setEntry : sets.entrySet()) {

            WeaponSet set = setEntry.getValue();

            int primary = set.getPrimaryWeapon();
            SplatoonWeapon weapon = SplatoonWeaponRegistry.getDummy(primary);
            if(weapon instanceof SplatoonPrimaryWeapon) {

                SplatoonPrimaryWeapon weapon1 = (SplatoonPrimaryWeapon) weapon;
                if(weapon1.getPrimaryWeaponType() == category) {

                    setList.add(set);

                }

            }

        }
        return setList;

    }

    public static int size() { return sets.size(); }
}
