package de.xenyria.splatoon.game.equipment.gear.registry;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.boots.*;
import de.xenyria.splatoon.game.equipment.gear.chest.*;
import de.xenyria.splatoon.game.equipment.gear.head.*;
import de.xenyria.splatoon.game.player.SplatoonPlayer;

import java.util.ArrayList;
import java.util.HashMap;

public class SplatoonGearRegistry {

    private static HashMap<Integer, Class> gearClassMap = new HashMap<>();
    private static HashMap<Integer, Gear> dummyInstances = new HashMap<>();

    public SplatoonGearRegistry() {

        gearClassMap.put(1, WhiteHeadband.class);
        gearClassMap.put(2, BasicTee.class);
        gearClassMap.put(3, CreamBasics.class);
        gearClassMap.put(4, CircleShades.class);
        gearClassMap.put(5, StudioHeadphones.class);
        gearClassMap.put(6, OctoHeadphones.class);
        gearClassMap.put(7, JungleHat.class);
        gearClassMap.put(8, SportyBobbleHat.class);
        gearClassMap.put(9, ArmorHelmetReplica.class);
        gearClassMap.put(100, ArmorJacketReplica.class);
        gearClassMap.put(101, BlackHoodie.class);
        gearClassMap.put(102, BluePeaksTee.class);
        gearClassMap.put(103, CamoZipHoodie.class);
        gearClassMap.put(104, ChocoLayeredLS.class);
        gearClassMap.put(105, GrapeTee.class);
        gearClassMap.put(106, HeroJacketReplica.class);
        gearClassMap.put(107, MintTee.class);
        gearClassMap.put(108, OctoLayeredLS.class);
        gearClassMap.put(109, PinkHoodie.class);
        gearClassMap.put(200, ArmorBootsReplica.class);
        gearClassMap.put(201, BlackSeahorses.class);
        gearClassMap.put(202, CrazyArrows.class);
        gearClassMap.put(203, CustomTrailBoots.class);
        gearClassMap.put(204, GoldHiHorses.class);

        for(int i : gearClassMap.keySet()) {

            dummyInstances.put(i, newInstance(i));

        }
        XenyriaSplatoon.getXenyriaLogger().log("§b" + gearClassMap.size() + " Ausrüstungsteil(e) §7registriert.");

    }

    public ArrayList<Gear> getGear(GearType category) {

        ArrayList<Gear> gears = new ArrayList<>();
        for(Gear gear : dummyInstances.values()) {

            if(gear.getType() == category) {

                gears.add(gear);

            }

        }
        return gears;

    }

    public Gear newInstance(int id) {

        Class clazz = gearClassMap.get(id);
        try {

            return (Gear) clazz.newInstance();

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        }

    }

    public Gear dummyInstance(int helmetID) { return dummyInstances.get(helmetID); }
}
