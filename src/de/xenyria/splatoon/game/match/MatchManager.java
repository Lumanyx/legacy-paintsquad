package de.xenyria.splatoon.game.match;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.core.chat.Characters;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class MatchManager {

    private static ArrayList<Match> customBattles = new ArrayList<>();
    public static ArrayList<Match> getCustomBattles() { return customBattles; }

    private static String CUSTOM_BATTLES_TITLE = "§8" + Characters.ARROW_RIGHT_FROM_TOP + " §7Eigene Kämpfe";

    private static Inventory inventory = Bukkit.createInventory(null, 54, CUSTOM_BATTLES_TITLE);

    public enum MatchStatus {

        OK(Material.GREEN_WOOL, "§aBetretbar!"),
        IN_PROGRESS(Material.ENDER_PEARL, "§eKampf aktiv - Zuschauerplatz frei"),
        SPECTATEABLE(Material.ENDER_EYE, "§eKampf inaktiv - Zuschauerplatz frei"),
        FULL(Material.RED_WOOL, "§cDieser Raum ist voll.");

        private Material material;
        private String description;

        MatchStatus(Material material, String description) {

            this.material = material;
            this.description = description;

        }

    }

    public void updateInventory() {

        /*inventory.clear();
        for(Match match : customBattles) {

            Material material = null;
            if(match.inProgress()) {

                material = Material.IRON_SWORD;

            } else {

                if(match.remainingSpace() >= 1) {

                    material = Material.GREEN_WOOL;

                } else if(match.remainingSpectatorSpace() >= 1) {

                    material = Material.ENDER_EYE;

                } else {

                    material = Material.BARRIER;

                }

            }

            //I/temBuilder builder = new ItemBuilder();

        }*/

    }

    public MatchManager() {



    }

}
