package de.xenyria.splatoon.game.equipment.weapon.set;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.Blaster;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.DebugBrush;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.Splattershot;
import de.xenyria.splatoon.game.equipment.weapon.primary.unbranded.*;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.*;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.bombrush.AutobombRush;
import de.xenyria.splatoon.game.equipment.weapon.special.bombrush.CurlingBombRush;
import de.xenyria.splatoon.game.equipment.weapon.special.inkstorm.InkStorm;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;
import de.xenyria.splatoon.game.equipment.weapon.special.stingray.StingRay;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissles;

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
        sets.put(3, new WeaponSet(3, "Tentatek Kleckser", 1800, Splattershot.ID, SplatBomb.ID, Jetpack.ID));
        sets.put(4, new WeaponSet(4, "Quasto", 2600, DebugBrush.ID, SplatBomb.ID, Splashdown.ID));
        sets.put(5, new WeaponSet(5, "Platscher", 10300, JetSquelcher.ID, ToxicMist.ID, TentaMissles.ID));
        sets.put(6, new WeaponSet(6, "Airbrush MG", 4700, AerosprayMG.ID, SuctionBomb.ID, CurlingBombRush.ID));
        sets.put(7, new WeaponSet(7, "Airbrush RG", 4700, AerosprayRG.ID, SprinklerSecondary.ID, Baller.ID));
        sets.put(8, new WeaponSet(8, "Kleckskonzentrator", 1200, Charger.ID, SplatBomb.ID, StingRay.ID));
        sets.put(9, new WeaponSet(9, "Sepiator α", 2100, Squiffer.ID, SplatBomb.ID, StingRay.ID));
        sets.put(10, new WeaponSet(10, "Zielkleckskonzentrator", 2100, ScopedCharger.ID, SplatBomb.ID, StingRay.ID));
        sets.put(11, new WeaponSet(11, "Heldenkonzentrator Replik", 8100, HeroCharger.ID, SplatBomb.ID, StingRay.ID));
        sets.put(12, new WeaponSet(12, "Eliter 4k", 5100, Eliter4k.ID, SplatBomb.ID, StingRay.ID));
        sets.put(13, new WeaponSet(13, "Klecksroller", 5100, SplatRoller.ID, CurlingBomb.ID, Splashdown.ID));
        sets.put(14, new WeaponSet(14, "Heldenroller", 5100, HeroRoller.ID, CurlingBomb.ID, Splashdown.ID));
        sets.put(15, new WeaponSet(15, "Karbonroller", 6100, CarbonRoller.ID, Autobomb.ID, InkStorm.ID));
        sets.put(16, new WeaponSet(16, "Platscher SE", 15900, JetSquelcher.ID, BurstBomb.ID, StingRay.ID));
        sets.put(17, new WeaponSet(17, "Blaster", 3000, Blaster.ID, BurstBomb.ID, Splashdown.ID));
        sets.put(18, new WeaponSet(18, "Blaster SE", 15000, Blaster.ID, Autobomb.ID, Jetpack.ID));
        sets.put(19, new WeaponSet(19, "Karbonroller Deko", 8500, CarbonRoller.ID, BurstBomb.ID, AutobombRush.ID));
        sets.put(20, new WeaponSet(20, "Krak-On Klecksroller", 5100, SplatRoller.ID, Beacon.ID, Baller.ID));
        sets.put(4, new WeaponSet(4, "Quasto Fresco", 8600, DebugBrush.ID, SplatBomb.ID, Splashdown.ID));

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
