package de.xenyria.splatoon.game.player.userdata.inventory.effect;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import de.xenyria.splatoon.game.equipment.gear.SpecialEffect;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.userdata.inventory.InventoryItem;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemCategory;
import de.xenyria.splatoon.game.player.userdata.inventory.ItemRewriteRule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.PotionMeta;

import javax.annotation.Nullable;

public class ConsumableItem extends InventoryItem {

    public static Material resolveMaterial(SplatoonHumanPlayer.ActiveEffect.Type type, SpecialEffect effect) {

        if(type == SplatoonHumanPlayer.ActiveEffect.Type.MONEY) {

            return Material.GOLD_NUGGET;

        } else if(type == SplatoonHumanPlayer.ActiveEffect.Type.EXPERIENCE) {

            return Material.EXPERIENCE_BOTTLE;

        } else if(type == SplatoonHumanPlayer.ActiveEffect.Type.GEAR_ABILITY) {

            return Material.POTION;

        }
        return Material.AIR;

    }

    public ConsumableItem(SplatoonHumanPlayer player, SplatoonHumanPlayer.ActiveEffect.Type type, int stackSize, int level, SpecialEffect effect) {

        super(player, resolveMaterial(type, effect), (short)0, null);
        this.effectType = type;

    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.CONSUMABLES;
    }

    public static final String CONSUME_ASK_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §cAnwenden?";

    private int level = 1;

    private SplatoonHumanPlayer.ActiveEffect.Type effectType;
    public SplatoonHumanPlayer.ActiveEffect.Type getEffectType() { return effectType; }

    private SpecialEffect specialEffect;
    public SpecialEffect getSpecialEffect() { return specialEffect; }

    private int stackSize = 1;

    @Override
    public void onClick() {

        Inventory inventory = Bukkit.createInventory(null, 36, CONSUME_ASK_TITLE);
        for(int i = 0; i < 36; i++) {

            inventory.setItem(i, ItemBuilder.getUnclickablePane());

        }
        inventory.setItem(13, asItemStack());
        inventory.setItem(29, new ItemBuilder(Material.HOPPER).setDisplayName("§4Wegwerfen").addLore("§7Entfernt den Gegenstand aus", "§7deinem Inventar.", "", "§4Kann nicht rückgängig gemacht werden!").addToNBT("Remove", getLocalItemID()).create());
        inventory.setItem(31, new ItemBuilder(Material.BARRIER).setDisplayName("§cNein").addLore("§7Zurück zum Inventar").addToNBT("Dismiss", true).create());

        ItemBuilder builder = null;
        if(getPlayer().getCurrentEffect() != null) {

            builder = new ItemBuilder(Material.YELLOW_TERRACOTTA).setDisplayName("§eAnwenden").addLore("§7Beachte, dass dein derzeitiger Effekt:", "§e" + getPlayer().getCurrentEffect().getName(), "§7entfernt wird.", "").addToNBT("UseEffect", getLocalItemID());

        } else {

            builder = new ItemBuilder(Material.GREEN_TERRACOTTA).setDisplayName("§aAnwenden").addLore("§7Beachte, dass dein derzeitiger Effekt:", "§e" + getPlayer().getCurrentEffect().getName(), "§7entfernt wird.", "").addToNBT("UseEffect", getLocalItemID());

        }
        inventory.setItem(33, builder.create());

    }

    private int matches = 20;

    @Override
    public void handleItemBuild(ItemBuilder builder) {

        builder.addLore("§8§l> §e§lInfo");
        switch (effectType) {

            case MONEY: builder.addLore("§8- §7Du erhälst §e" + SplatoonHumanPlayer.ActiveEffect.getModifier(level) + " mal §7soviele Taler nach Rundenende."); break;
            case EXPERIENCE: builder.addLore("§8- §7Du erhältst §e" + SplatoonHumanPlayer.ActiveEffect.getModifier(level) + " mal §7soviel Erfahrung nach Rundenende."); break;
            case GEAR_ABILITY:
                builder.addLore("§8- §7Du erhältst §e" + SplatoonHumanPlayer.ActiveEffect.getModifier(level) + " mal §7soviel Erfahrung für Ausrüstungsteile.");
                builder.addLore("§8- §7Bei einem Levelanstieg eines Ausrüstungsteil gibt es erhöhte Chancen für den folgenden Effekt:");
                builder.addLore("  " + specialEffect.getName()).create(); break;

        }
        builder.addLore("§7Dieser Effekt hält für §6" + matches + " Runden§7.");

    }
}
