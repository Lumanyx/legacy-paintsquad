package de.xenyria.splatoon.game.equipment.weapon.registry;


import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.primary.ai.tutorial.TutorialOctoshot;
import de.xenyria.splatoon.game.equipment.weapon.primary.debug.*;
import de.xenyria.splatoon.game.equipment.weapon.primary.unbranded.*;
import de.xenyria.splatoon.game.equipment.weapon.secondary.debug.*;
import de.xenyria.splatoon.game.equipment.weapon.special.armor.InkArmor;
import de.xenyria.splatoon.game.equipment.weapon.special.baller.Baller;
import de.xenyria.splatoon.game.equipment.weapon.special.jetpack.Jetpack;
import de.xenyria.splatoon.game.equipment.weapon.special.splashdown.Splashdown;
import de.xenyria.splatoon.game.equipment.weapon.special.stingray.StingRay;
import de.xenyria.splatoon.game.equipment.weapon.special.tentamissles.TentaMissles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class SplatoonWeaponRegistry {

    private static HashMap<Integer, Class> weaponClassMap = new HashMap<>();
    private static HashMap<Integer, SplatoonWeapon> dummyWeapons = new HashMap<>();
    public static Collection<SplatoonWeapon> getWeapons() { return dummyWeapons.values(); }
    public static int getRegisterCount() { return weaponClassMap.size(); }

    public static Class<SplatoonWeapon> getWeaponClass(int id) { return weaponClassMap.getOrDefault(id, null); }

    public static SplatoonWeapon getDummy(int id) { return dummyWeapons.get(id); }


    public void registerWeapon(int id, Class weaponClass) { weaponClassMap.put(id, weaponClass); }
    public SplatoonWeapon newInstance(int id) {

        try {

            Class<SplatoonWeapon> splatoonWeaponClass = weaponClassMap.get(id);
            if(splatoonWeaponClass != null) {

                SplatoonWeapon weapon = splatoonWeaponClass.newInstance();
                return weapon;

            } else {

                XenyriaSplatoon.getXenyriaLogger().warn("Die gewählte Waffe §e(#" + id + ") §7ist nicht registriert.");

            }

        } catch (Exception e) {

            e.printStackTrace();

        }
        return null;

    }

    public SplatoonWeaponRegistry() {

        registerWeapon(1, Splattershot.class);
        registerWeapon(2, SplatBomb.class);
        registerWeapon(3, TentaMissles.class);
        registerWeapon(4, DebugRoller.class);
        registerWeapon(5, DebugCurlingBomb.class);
        registerWeapon(6, Jetpack.class);
        registerWeapon(7, Charger.class);
        registerWeapon(8, SprinklerSecondary.class);
        registerWeapon(9, StingRay.class);
        registerWeapon(10, Splashdown.class);
        registerWeapon(11, DebugDualies.class);
        registerWeapon(12, DebugSuctionBomb.class);
        registerWeapon(13, Baller.class);
        registerWeapon(14, Beacon.class);
        registerWeapon(15, DebugSlosher.class);
        registerWeapon(16, Splatling.class);
        registerWeapon(17, DebugBrella.class);
        registerWeapon(18, DebugBlaster.class);
        registerWeapon(19, RainMaker.class);
        registerWeapon(20, BurstBomb.class);
        registerWeapon(21, SplattershotJr.class);
        registerWeapon(22, InkArmor.class);
        registerWeapon(23, TutorialOctoshot.class);
        registerWeapon(24, DebugBrush.class);
        registerWeapon(25, ScopedCharger.class);
        registerWeapon(26, Squiffer.class);
        registerWeapon(27, HeroCharger.class);
        registerWeapon(28, Eliter4k.class);
        registerWeapon(29, JetSquelcher.class);
        registerWeapon(30, AerosprayMG.class);
        registerWeapon(31, AerosprayRG.class);
        registerWeapon(32, CarbonRoller.class);
        registerWeapon(33, MiniSplatling.class);

        for(int id : weaponClassMap.keySet()) {

            SplatoonWeapon weapon = newInstance(id);
            dummyWeapons.put(id, weapon);

        }

        XenyriaSplatoon.getXenyriaLogger().log("§b" + weaponClassMap.size() + " Waffe(n) §7wurden registriert.");

    }

    public ArrayList<SplatoonWeapon> getAllWeapons() { return new ArrayList<>(dummyWeapons.values()); }
}
