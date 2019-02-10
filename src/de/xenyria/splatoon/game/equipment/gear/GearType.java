package de.xenyria.splatoon.game.equipment.gear;

import de.xenyria.api.spigot.ItemBuilder;
import net.minecraft.server.v1_13_R2.EnumItemSlot;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public enum GearType {

    HELMET("Helm", EnumItemSlot.HEAD, EquipmentSlot.HEAD, Material.LEATHER_HELMET),
    CHESTPLATE("Brustpanzer", EnumItemSlot.CHEST, EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE),
    BOOTS("Stiefel", EnumItemSlot.FEET, EquipmentSlot.FEET, Material.LEATHER_BOOTS);

    private String displayName;
    public String getDisplayName() { return displayName; }

    private EnumItemSlot slot;
    public EnumItemSlot getSlot() { return slot; }

    private EquipmentSlot slotID;
    public EquipmentSlot getSlotID() { return slotID; }

    private Material material;
    public Material getMaterial() { return material; }

    public ItemBuilder toBuilder() {

        return new ItemBuilder(material);

    }

    GearType(String displayName, EnumItemSlot slot, EquipmentSlot slotID, Material material) {

        this.displayName = displayName;
        this.slot = slot;
        this.slotID = slotID;
        this.material = material;

    }

}
