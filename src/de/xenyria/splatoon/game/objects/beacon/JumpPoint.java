package de.xenyria.splatoon.game.objects.beacon;

import de.xenyria.api.spigot.ItemBuilder;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.team.Team;
import de.xenyria.splatoon.game.util.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public abstract class JumpPoint {

    public abstract Material getMaterial();
    public abstract String getItemTitle();
    public abstract Location getLocation();
    public abstract void onJumpBegin();
    public abstract void onJumpEnd();
    public abstract boolean isAvailable(Team team);

    public ItemStack getItem() {

        return new ItemBuilder(
                getMaterial()
        ).setDisplayName(getItemTitle()).addLore("§7Klicke für einen Supersprung.").create();

    }

    public static class Player extends JumpPoint {

        private SplatoonPlayer player;
        public Player(SplatoonPlayer player) {

            this.player = player;

        }

        @Override
        public Material getMaterial() {
            return Material.PLAYER_HEAD;
        }

        @Override
        public String getItemTitle() {
            return player.getName();
        }

        public SplatoonPlayer getPlayer() { return player; }

        @Override
        public Location getLocation() {

            return player.getLastSafePosition();

        }

        @Override
        public void onJumpBegin() {

        }

        @Override
        public void onJumpEnd() {

        }

        @Override
        public boolean isAvailable(Team team) {

            return !this.player.isSplatted();

        }

    }

    public static class Beacon extends JumpPoint {

        private String title;
        private BeaconObject beacon;

        public Beacon(SplatoonPlayer owner, BeaconObject beacon) {

            this.beacon = beacon;
            title = "Sprungboje von " + owner.getName();

        }

        @Override
        public Material getMaterial() {
            return Material.BEACON;
        }

        @Override
        public String getItemTitle() {
            return title;
        }

        @Override
        public Location getLocation() {
            return beacon.getLocation();
        }

        @Override
        public void onJumpBegin() {

            beacon.remove();
        }

        @Override
        public void onJumpEnd() {


        }

        @Override
        public boolean isAvailable(Team team) {
            return team.equals(beacon.getOwner().getTeam());
        }


    }

}
