package de.xenyria.splatoon.lobby.shop;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.GearType;
import de.xenyria.splatoon.lobby.shop.gear.GearShopItem;
import de.xenyria.splatoon.lobby.shop.item.ShopItem;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Random;

public abstract class AbstractShop {

    private AbstractShopkeeper shopkeeper;
    private Location[] locations;

    public AbstractShop(AbstractShopkeeper shopkeeper, ShopType type, Location... locations) {

        this.shopkeeper = shopkeeper;
        this.shopType = type;
        this.locations = locations;

    }

    private ShopType shopType;
    public ShopType getShopType() { return shopType; }

    private Sortiment sortiment;
    public Sortiment getSortiment() { return sortiment; }
    public void initializeSortiment() {

        this.sortiment = new Sortiment();

    }

    public enum ShopType {

        HEAD_GEAR(GearType.HELMET),
        BODY_GEAR(GearType.CHESTPLATE),
        FOOT_GEAR(GearType.BOOTS),
        WEAPONS(null);

        private GearType type;
        public boolean isGearShop() { return type != null; }

        ShopType(GearType type) {

            this.type = type;

        }

    }

    public class Sortiment {

        private ArrayList<ShopItem> items = new ArrayList<>();
        public ArrayList<ShopItem> getItems() { return items; }

        public void generateSortiment() {

            if(!items.isEmpty()) {

                for(ShopItem item : items) {

                    item.remove();
                    item.onRemove();

                }

            }

            ArrayList<Gear> gear = new ArrayList<>();
            ArrayList<Gear> availableGear = XenyriaSplatoon.getGearRegistry().getGear(shopType.type);
            availableGear.addAll(XenyriaSplatoon.getGenericGearRegistry().getGear(shopType.type));

            // Dummy Instanz aller Ausrüstungsteile
            int requiredItems = locations.length;
            for(int i = 0; i < requiredItems; i++) {

                gear.add(availableGear.get(new Random().nextInt(availableGear.size()-1)));

            }

            int x = 0;
            for(Gear gear1 : gear) {

                Location location = locations[x];
                GearShopItem item = GearShopItem.fromGear(gear1, location);
                items.add(item);
                x++;

            }

        }

    }

}
