package de.xenyria.splatoon.game.equipment.gear;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.gear.level.GearLevelManager;
import de.xenyria.splatoon.game.equipment.weapon.BrandedEquipment;
import de.xenyria.splatoon.game.equipment.weapon.util.ProgressBarUtil;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
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
    public int getPrice() { return price; }

    private org.bukkit.Color color;
    public org.bukkit.Color getColor() { return color; }

    private int originID;
    public int getOriginID() { return originID; }

    private int inventoryID;
    public int getInventoryID() { return inventoryID; }

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

    public ItemStack asItemStack(@Nullable Color color) {

        ItemBuilder stack = new ItemBuilder(material).setDisplayName(brand.getDisplayName() + name).addLore(
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

                colorPrefix = assignedPlayer.getColor().prefix();
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

                stack.addLore("§bNoch " + getGearData().experienceToNextRank() + " XP bis Stufe " + gearData.currentLevel() + 1);

            }

        }
        if(useCustomModel()) {

            stack.addToNBT("rwr_mat", getCustomModelMaterial().name());
            stack.addToNBT("rwr_dmg", (int)getCustomModelDamageValue());

        }

        ItemStack stack1 = stack.create();
        if(this.color == null && color != null) {

            if(stack1.getItemMeta() instanceof LeatherArmorMeta) {

                LeatherArmorMeta meta = (LeatherArmorMeta) stack1.getItemMeta();
                meta.setColor(color.getBukkitColor());
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
    public void assignToPlayer(SplatoonPlayer player, int equipmentID) {

        assignedPlayer = player;
        this.inventoryID = equipmentID;

    }

    private SpecialEffect defaultAbility;
    public SpecialEffect getDefaultEffect() { return defaultAbility; }

}
