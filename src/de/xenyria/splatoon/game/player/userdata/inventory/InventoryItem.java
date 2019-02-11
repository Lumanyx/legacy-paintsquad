package de.xenyria.splatoon.game.player.userdata.inventory;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public abstract class InventoryItem {

    private int localItemID;
    public InventoryItem(SplatoonHumanPlayer player, Material material, short durability, @Nullable ItemRewriteRule rewriteRule) {

        this.assignedPlayer = player;
        this.rewriteRule = rewriteRule;
        this.durability = durability;
        this.material = material;

    }

    public void addToPlayerInventory() {

        localItemID = getPlayer().getInventory().addItem(this);

    }

    private ItemRewriteRule rewriteRule;
    private short durability;

    private Material material;
    public Material getMaterial() { return material; }

    public abstract ItemCategory getCategory();
    public abstract void onClick();

    private SplatoonHumanPlayer assignedPlayer;
    public SplatoonHumanPlayer getPlayer() { return assignedPlayer; }


    private ItemStack stack;
    public void buildItem() {

        ItemBuilder builder = new ItemBuilder(material);
        builder.setDisplayName("NO_NAME_DEFINED");
        builder.setDurability(durability);
        handleItemBuild(builder);

        builder.addToNBT("ItemID", localItemID);
        stack = builder.create();

    }
    public abstract void handleItemBuild(ItemBuilder builder);
    public ItemStack asItemStack() {

        if(stack == null) { buildItem(); }

        return stack;

    }

    public int getLocalItemID() { return localItemID; }

    private boolean equipped;
    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean val) {

        this.equipped = val;

    }

}