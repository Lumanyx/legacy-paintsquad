package de.xenyria.splatoon.game.equipment.gear.registry;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearData;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import de.xenyria.splatoon.game.equipment.gear.head.HeadGear;
import de.xenyria.splatoon.game.equipment.gear.head.LegacyHeadGear;
import org.bukkit.Art;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SplatoonGenericGearRegistry {

    private static final int DYNAMIC_GEAR_ID_START = 1000;
    private static final int GEAR_AMOUNT_PER_LEVEL = 10;

    private static HashMap<Integer, DynamicGearData> gearData = new HashMap<>();
    private static HashMap<Integer, Gear> dummyInstances = new HashMap<>();

    public static boolean isRegistered(int gearID) { return gearData.containsKey(gearID); }

    public Gear dummyInstance(int i) {

        return dummyInstances.get(i);

    }

    public class DynamicGearData {

        private Gear dummyInstance;
        private int id;
        private GearType type;
        private int price;
        private int abilities;
        private Color color;
        private Brand brand;
        private String name;
        private SpecialEffect ability;

        public DynamicGearData(int id, GearType type, int price, int abilityCount, Color color, Brand brand, String name, SpecialEffect effect) {

            this.id = id;
            this.type = type;
            this.price = price;
            this.abilities = abilityCount;
            this.color = color;
            this.brand = brand;
            this.name = name;
            this.ability = effect;
            dummyInstance = newInstance();

        }

        public Gear newInstance() {

            if(type == GearType.HELMET) {

                return new LegacyHeadGear(id, brand, name, ability, Material.LEATHER_HELMET, color, abilities, price);

            } else if(type == GearType.CHESTPLATE) {

                return new LegacyHeadGear(id, brand, name, ability, Material.LEATHER_CHESTPLATE, color, abilities, price);

            } else {

                return new LegacyHeadGear(id, brand, name, ability, Material.LEATHER_BOOTS, color, abilities, price);

            }

        }

    }

    private int nextGearItemID() { return DYNAMIC_GEAR_ID_START + gearData.size(); }
    public void addHeadGear(String name, Color color) {

        int starCount = 1 + (new Random().nextInt(2));
        int price = 0;
        switch (starCount) {

            case 1: price = 300 + (new Random().nextInt(7) * 100); break;
            case 2: price = 2000 + (new Random().nextInt(20) * 100); break;
            case 3: price = 9000 + (new Random().nextInt(30) * 100); break;

        }

        int gearID = nextGearItemID();
        Brand brand = Brand.values()[new Random().nextInt(Brand.values().length - 1)];
        SpecialEffect specialEffect = SpecialEffect.values()[new Random().nextInt(SpecialEffect.values().length - 1)];
        DynamicGearData data = new DynamicGearData(gearID, GearType.HELMET, price, starCount, color, brand, name, specialEffect);
        gearData.put(gearID, data);

    }
    public void addBodyGear(String name, Color color) {

        int starCount = 1 + (new Random().nextInt(2));
        int price = 0;
        switch (starCount) {

            case 1: price = 300 + (new Random().nextInt(7) * 100); break;
            case 2: price = 2000 + (new Random().nextInt(20) * 100); break;
            case 3: price = 9000 + (new Random().nextInt(30) * 100); break;

        }

        int gearID = nextGearItemID();
        Brand brand = Brand.values()[new Random().nextInt(Brand.values().length - 1)];
        SpecialEffect specialEffect = SpecialEffect.values()[new Random().nextInt(SpecialEffect.values().length - 1)];
        DynamicGearData data = new DynamicGearData(gearID, GearType.CHESTPLATE, price, starCount, color, brand, name, specialEffect);
        gearData.put(gearID, data);

    }
    public void addFootGear(String name, Color color) {

        int starCount = 1 + (new Random().nextInt(2));
        int price = 0;
        switch (starCount) {

            case 1: price = 300 + (new Random().nextInt(7) * 100); break;
            case 2: price = 2000 + (new Random().nextInt(20) * 100); break;
            case 3: price = 9000 + (new Random().nextInt(30) * 100); break;

        }

        int gearID = nextGearItemID();
        Brand brand = Brand.randomBrand();
        SpecialEffect specialEffect = SpecialEffect.values()[new Random().nextInt(SpecialEffect.values().length - 1)];
        DynamicGearData data = new DynamicGearData(gearID, GearType.BOOTS, price, starCount, color, brand, name, specialEffect);
        gearData.put(gearID, data);

    }

    public SplatoonGenericGearRegistry() {

        addHeadGear("Grüner Helm", Color.GREEN);
        addHeadGear("Blauer Helm", Color.BLUE);
        addHeadGear("Lila Helm", Color.PURPLE);
        addHeadGear("Gelber Helm", Color.YELLOW);
        addHeadGear("Roter Helm", Color.RED);
        addHeadGear("Schwarzer Helm", Color.BLACK);
        addHeadGear("Weißer Helm", Color.WHITE);
        addHeadGear("Oranger Helm", Color.ORANGE);
        addHeadGear("Hellgrüner Helm", Color.LIME);
        addHeadGear("Farbneutraler Helm", null);

        addHeadGear("Grüner Hut", Color.GREEN);
        addHeadGear("Blauer Hut", Color.BLUE);
        addHeadGear("Lila Hut", Color.PURPLE);
        addHeadGear("Gelber Hut", Color.YELLOW);
        addHeadGear("Roter Hut", Color.RED);
        addHeadGear("Schwarzer Hut", Color.BLACK);
        addHeadGear("Weißer Hut", Color.WHITE);
        addHeadGear("Oranger Hut", Color.ORANGE);
        addHeadGear("Hellgrüner Hut", Color.LIME);
        addHeadGear("Farbneutraler Hut", null);

        addHeadGear("Grüne Kappe", Color.GREEN);
        addHeadGear("Blaue Kappe", Color.BLUE);
        addHeadGear("Lila Kappe", Color.PURPLE);
        addHeadGear("Gelbe Kappe", Color.YELLOW);
        addHeadGear("Rote Kappe", Color.RED);
        addHeadGear("Schwarze Kappe", Color.BLACK);
        addHeadGear("Weiße Kappe", Color.WHITE);
        addHeadGear("Orange Kappe", Color.ORANGE);
        addHeadGear("Hellgrüne Kappe", Color.LIME);
        addHeadGear("Farbneutrale Kappe", null);

        addHeadGear("Grüne Mütze", Color.GREEN);
        addHeadGear("Blaue Mütze", Color.BLUE);
        addHeadGear("Lila Mütze", Color.PURPLE);
        addHeadGear("Gelbe Mütze", Color.YELLOW);
        addHeadGear("Rote Mütze", Color.RED);
        addHeadGear("Schwarze Mütze", Color.BLACK);
        addHeadGear("Weiße Mütze", Color.WHITE);
        addHeadGear("Orange Mütze", Color.ORANGE);
        addHeadGear("Hellgrüne Mütze", Color.LIME);
        addHeadGear("Farbneutrale Mütze", null);

        addHeadGear("Grüne Pudelmütze", Color.GREEN);
        addHeadGear("Blaue Pudelmütze", Color.BLUE);
        addHeadGear("Lila Pudelmütze", Color.PURPLE);
        addHeadGear("Gelbe Pudelmütze", Color.YELLOW);
        addHeadGear("Rote Pudelmütze", Color.RED);
        addHeadGear("Schwarze Pudelmütze", Color.BLACK);
        addHeadGear("Weiße Pudelmütze", Color.WHITE);
        addHeadGear("Orange Pudelmütze", Color.ORANGE);
        addHeadGear("Hellgrüne Pudelmütze", Color.LIME);
        addHeadGear("Farbneutrale Pudelmütze", null);

        addBodyGear("Grüne Jacke", Color.GREEN);
        addBodyGear("Blaue Jacke", Color.BLUE);
        addBodyGear("Lila Jacke", Color.PURPLE);
        addBodyGear("Gelbe Jacke", Color.YELLOW);
        addBodyGear("Rote Jacke", Color.RED);
        addBodyGear("Schwarze Jacke", Color.BLACK);
        addBodyGear("Weiße Jacke", Color.WHITE);
        addBodyGear("Orange Jacke", Color.ORANGE);
        addBodyGear("Hellgrüne Jacke", Color.LIME);
        addBodyGear("Farbneutrale Jacke", null);

        addBodyGear("Grüne Weste", Color.GREEN);
        addBodyGear("Blaue Weste", Color.BLUE);
        addBodyGear("Lila Weste", Color.PURPLE);
        addBodyGear("Gelbe Weste", Color.YELLOW);
        addBodyGear("Rote Weste", Color.RED);
        addBodyGear("Schwarze Weste", Color.BLACK);
        addBodyGear("Weiße Weste", Color.WHITE);
        addBodyGear("Orange Weste", Color.ORANGE);
        addBodyGear("Hellgrüne Weste", Color.LIME);
        addBodyGear("Farbneutrale Weste", null);

        addBodyGear("Grünes Shirt", Color.GREEN);
        addBodyGear("Blaues Shirt", Color.BLUE);
        addBodyGear("Lila Shirt", Color.PURPLE);
        addBodyGear("Gelbes Shirt", Color.YELLOW);
        addBodyGear("Rotes Shirt", Color.RED);
        addBodyGear("Schwarzes Shirt", Color.BLACK);
        addBodyGear("Weißes Shirt", Color.WHITE);
        addBodyGear("Oranges Shirt", Color.ORANGE);
        addBodyGear("Hellgrünes Shirt", Color.LIME);
        addBodyGear("Farbneutrales Shirt", null);

        addBodyGear("Grünes Lagenshirt", Color.GREEN);
        addBodyGear("Blaues Lagenshirt", Color.BLUE);
        addBodyGear("Lila Lagenshirt", Color.PURPLE);
        addBodyGear("Gelbes Lagenshirt", Color.YELLOW);
        addBodyGear("Rotes Lagenshirt", Color.RED);
        addBodyGear("Schwarzes Lagenshirt", Color.BLACK);
        addBodyGear("Weißes Lagenshirt", Color.WHITE);
        addBodyGear("Oranges Lagenshirt", Color.ORANGE);
        addBodyGear("Hellgrünes Lagenshirt", Color.LIME);
        addBodyGear("Farbneutrales Lagenshirt", null);

        addBodyGear("Grüner Hoodie", Color.GREEN);
        addBodyGear("Blauer Hoodie", Color.BLUE);
        addBodyGear("Lila Hoodie", Color.PURPLE);
        addBodyGear("Gelber Hoodie", Color.YELLOW);
        addBodyGear("Roter Hoodie", Color.RED);
        addBodyGear("Schwarzer Hoodie", Color.BLACK);
        addBodyGear("Weißer Hoodie", Color.WHITE);
        addBodyGear("Oranger Hoodie", Color.ORANGE);
        addBodyGear("Hellgrüner Hoodie", Color.LIME);
        addBodyGear("Farbneutraler Hoodie", null);

        addBodyGear("Grüner Cardigan", Color.GREEN);
        addBodyGear("Blauer Cardigan", Color.BLUE);
        addBodyGear("Lila Cardigan", Color.PURPLE);
        addBodyGear("Gelber Cardigan", Color.YELLOW);
        addBodyGear("Roter Cardigan", Color.RED);
        addBodyGear("Schwarzer Cardigan", Color.BLACK);
        addBodyGear("Weißer Cardigan", Color.WHITE);
        addBodyGear("Oranger Cardigan", Color.ORANGE);
        addBodyGear("Hellgrüner Cardigan", Color.LIME);
        addBodyGear("Farbneutraler Cardigan", null);

        addFootGear("Grüne Stiefel", Color.GREEN);
        addFootGear("Blaue Stiefel", Color.BLUE);
        addFootGear("Lila Stiefel", Color.PURPLE);
        addFootGear("Gelbe Stiefel", Color.YELLOW);
        addFootGear("Rote Stiefel", Color.RED);
        addFootGear("Schwarze Stiefel", Color.BLACK);
        addFootGear("Weiße Stiefel", Color.WHITE);
        addFootGear("Orange Stiefel", Color.ORANGE);
        addFootGear("Hellgrüne Stiefel", Color.LIME);
        addFootGear("Farbneutrale Stiefel", null);

        addFootGear("Grüne Kampfstiefel", Color.GREEN);
        addFootGear("Blaue Kampfstiefel", Color.BLUE);
        addFootGear("Lila Kampfstiefel", Color.PURPLE);
        addFootGear("Gelbe Kampfstiefel", Color.YELLOW);
        addFootGear("Rote Kampfstiefel", Color.RED);
        addFootGear("Schwarze Kampfstiefel", Color.BLACK);
        addFootGear("Weiße Kampfstiefel", Color.WHITE);
        addFootGear("Orange Kampfstiefel", Color.ORANGE);
        addFootGear("Hellgrüne Kampfstiefel", Color.LIME);
        addFootGear("Farbneutrale Kampfstiefel", null);

        addFootGear("Grüne Pfeilschuhe", Color.GREEN);
        addFootGear("Blaue Pfeilschuhe", Color.BLUE);
        addFootGear("Lila Pfeilschuhe", Color.PURPLE);
        addFootGear("Gelbe Pfeilschuhe", Color.YELLOW);
        addFootGear("Rote Pfeilschuhe", Color.RED);
        addFootGear("Schwarze Pfeilschuhe", Color.BLACK);
        addFootGear("Weiße Pfeilschuhe", Color.WHITE);
        addFootGear("Orange Pfeilschuhe", Color.ORANGE);
        addFootGear("Hellgrüne Pfeilschuhe", Color.LIME);
        addFootGear("Farbneutrale Pfeilschuhe", null);

        addFootGear("Grüne Slip-Ons", Color.GREEN);
        addFootGear("Blaue Slip-Ons", Color.BLUE);
        addFootGear("Lila Slip-Ons", Color.PURPLE);
        addFootGear("Gelbe Slip-Ons", Color.YELLOW);
        addFootGear("Rote Slip-Ons", Color.RED);
        addFootGear("Schwarze Slip-Ons", Color.BLACK);
        addFootGear("Weiße Slip-Ons", Color.WHITE);
        addFootGear("Orange Slip-Ons", Color.ORANGE);
        addFootGear("Hellgrüne Slip-Ons", Color.LIME);
        addFootGear("Farbneutrale Slip-Ons", null);

        addFootGear("Grüne Leinenschuhe", Color.GREEN);
        addFootGear("Blaue Leinenschuhe", Color.BLUE);
        addFootGear("Lila Leinenschuhe", Color.PURPLE);
        addFootGear("Gelbe Leinenschuhe", Color.YELLOW);
        addFootGear("Rote Leinenschuhe", Color.RED);
        addFootGear("Schwarze Leinenschuhe", Color.BLACK);
        addFootGear("Weiße Leinenschuhe", Color.WHITE);
        addFootGear("Orange Leinenschuhe", Color.ORANGE);
        addFootGear("Hellgrüne Leinenschuhe", Color.LIME);
        addFootGear("Farbneutrale Leinenschuhe", null);

        addFootGear("Grüne Sneaker", Color.GREEN);
        addFootGear("Blaue Sneaker", Color.BLUE);
        addFootGear("Lila Sneaker", Color.PURPLE);
        addFootGear("Gelbe Sneaker", Color.YELLOW);
        addFootGear("Rote Sneaker", Color.RED);
        addFootGear("Schwarze Sneaker", Color.BLACK);
        addFootGear("Weiße Sneaker", Color.WHITE);
        addFootGear("Orange Sneaker", Color.ORANGE);
        addFootGear("Hellgrüne Sneaker", Color.LIME);
        addFootGear("Farbneutrale Sneaker", null);
        for(Map.Entry<Integer, DynamicGearData> entry : gearData.entrySet()) {

            dummyInstances.put(entry.getKey(), entry.getValue().dummyInstance);

        }
        XenyriaSplatoon.getXenyriaLogger().log("Es wurden §e" + gearData.size() + " generische Ausrüstungsteile §7registriert.");

    }

    public ArrayList<Gear> getGear(GearType type) {

        ArrayList<Gear> gears = new ArrayList<>();
        for(DynamicGearData data : gearData.values()) {

            if(data.type == type) {

                gears.add(data.dummyInstance);

            }

        }
        return gears;

    }

    public Gear getNewGearInstance(int id) {

        for(DynamicGearData data : gearData.values()) {

            if(data.id == id) {

                return data.newInstance();

            }

        }
        return null;

    }

}
