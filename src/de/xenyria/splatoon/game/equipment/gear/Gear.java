package de.xenyria.splatoon.game.equipment.gear;

import com.mysql.fabric.xmlrpc.base.Data;
import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.level.GearLevelManager;
import de.xenyria.splatoon.game.equipment.gear.registry.SplatoonGearRegistry;
import de.xenyria.splatoon.game.equipment.gear.registry.SplatoonGenericGearRegistry;
import de.xenyria.splatoon.game.equipment.weapon.BrandedEquipment;
import de.xenyria.splatoon.game.equipment.weapon.util.ProgressBarUtil;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.gear.GearItem;
import org.bukkit.Material;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.annotation.Nullable;

public abstract class Gear implements BrandedEquipment {

    public Gear(int originID, GearType type, Brand brand, String itemName, SpecialEffect baseEffect, Material material, org.bukkit.Color color, int maxSubAbilities, int price) {

        this.originID = originID;
        this.type = type;
        this.brand = brand;
        this.name = itemName;
        this.defaultAbility = baseEffect;
        this.material = material;
        this.color = color;
        this.maxSubAbilities = maxSubAbilities;
        this.price = price;
        gearData = new GearData(this);

    }

    private int price;

    public static Gear newInstance(int gearID) {

        if(SplatoonGearRegistry.isRegistered(gearID)) {

            return XenyriaSplatoon.getGearRegistry().newInstance(gearID);

        } else if(SplatoonGenericGearRegistry.isRegistered(gearID)) {

            return XenyriaSplatoon.getGenericGearRegistry().getNewGearInstance(gearID);

        }
        return null;

    }

    public static Gear dummyInstance(int i) {

        if(SplatoonGearRegistry.isRegistered(i)) {

            return XenyriaSplatoon.getGearRegistry().dummyInstance(i);

        } else if(SplatoonGenericGearRegistry.isRegistered(i)) {

            return XenyriaSplatoon.getGenericGearRegistry().dummyInstance(i);

        }
        return null;

    }

    public int getPrice() { return price; }

    private org.bukkit.Color color;
    public org.bukkit.Color getColor() { return color; }

    private int originID;
    public int getOriginID() { return originID; }

    private String name;
    public String getName() { return name; }

    private Brand brand;
    public Brand getBrand() { return brand; }

    private GearType type;
    public GearType getType() { return type; }

    private GearData gearData;
    public GearData getGearData() { return gearData; }

    public abstract boolean useCustomModel();
    public abstract short getCustomModelDamageValue();
    public abstract Material getCustomModelMaterial();

    public SpecialEffect getMainAbility() {

        if(getGearData().getPrimaryEffect() != null) {

            return getGearData().getPrimaryEffect();

        } else {

            return defaultAbility;

        }

    }

    private int maxSubAbilities = 1;
    public int getMaxSubAbilities() { return maxSubAbilities; }

    public ItemBuilder handleItemBuilder(ItemBuilder stack, @Nullable Color color) {

        stack.setDisplayName("§e§l" + name).addLore(
                "§8§l> Rüstungstyp", "§7" + type.getDisplayName(), ""
        );

        stack.addLore("");
        stack.addLore("§8§l> Fähigkeiten");
        stack.addLore(getGearData().getPrimaryEffect().getName());
        for(int i = 0; i < maxSubAbilities; i++) {

            if(i >= (getGearData().getSubAbilities().size() - 1) && !getGearData().getSubAbilities().isEmpty()) {

                stack.addLore("§8- " + getGearData().getSubAbilities().get(i).getName());

            } else {

                stack.addLore("§8- §f§l?");

            }

        }
        stack.addLore("");

        if(assignedPlayer != null) {

            String colorPrefix = "§b";
            if(this.color == null) {

                if (color != null) {

                    colorPrefix = color.prefix();
                    stack.addColor(color.getBukkitColor());

                }

            } else {

                colorPrefix = "§e";
                stack.addColor(this.color);

            }

            stack.addLore("§8§l> Erfahrung");
            stack.addLore(ProgressBarUtil.generateProgressBar(
                    getGearData().levelPercentage(),
                    10,
                    colorPrefix,
                    "§8"
            ));
            if(getGearData().isFullyLevelled()) {

                stack.addLore("§bMaximalstufe (" + GearLevelManager.getLevels(maxSubAbilities) + ") erreicht");

            } else {

                stack.addLore("§bNoch " + getGearData().experienceToNextRank() + " XP bis Stufe " + (gearData.currentLevel()));

            }

        }
        if(useCustomModel()) {

            stack.addToNBT("rwr_mat", getCustomModelMaterial().name());
            stack.addToNBT("rwr_dmg", (int)getCustomModelDamageValue());

        }

        return stack;

    }

    public ItemStack asItemStack(@Nullable Color color) {

        ItemBuilder stack = new ItemBuilder(material);
        stack.setUnbreakable(true);
        handleItemBuilder(stack, color);
        ItemStack stack1 = stack.create();
        if(this.color == null && color != null) {

            if(stack1.getItemMeta() instanceof LeatherArmorMeta) {

                LeatherArmorMeta meta = (LeatherArmorMeta) stack1.getItemMeta();
                meta.setColor(color.getBukkitColor());
                stack1.setItemMeta(meta);

            }

        } else {

            if(stack1.getItemMeta() instanceof LeatherArmorMeta) {

                LeatherArmorMeta meta = (LeatherArmorMeta) stack1.getItemMeta();
                meta.setColor(this.color);
                stack1.setItemMeta(meta);

            }

        }
        return stack1;

    }

    public static ItemStack rewriteItemStack(ItemStack input) {

        Material material = Material.valueOf(ItemBuilder.getStringValue(input, "rwr_mat"));
        short damage = (short)ItemBuilder.getIntValue(input, "rwr_dmg");

        ItemStack stack = input.clone();
        stack.setType(material);
        stack.setDurability(damage);

        return stack;

    }

    private Material material;
    public Material getMaterial() { return material; }

    private SplatoonPlayer assignedPlayer;
    public void assignToPlayer(SplatoonPlayer player) {

        assignedPlayer = player;

    }

    private SpecialEffect defaultAbility;
    public SpecialEffect getDefaultEffect() { return defaultAbility; }

    public void addExistingData(GearItem.StoredGearData data) {

        this.gearData = new GearData(this);
        gearData.setExperience(data.getExperience());
        gearData.setPrimaryEffect(gearData.getPrimaryEffect());
        if(data.getSubEffects() != null) {

            for(SpecialEffect effect : data.getSubEffects()) {

                gearData.getSubAbilities().add(effect);

            }

        }

    }

}
