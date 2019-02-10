package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.equipment.weapon.registry.SplatoonWeaponRegistry;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

public class WeaponDrop extends GameObject {

    private int id;
    private SplatoonWeapon weapon;
    private Item item;
    private Location location;
    public WeaponDrop(Match match, int weaponID, Location location) {

        super(match);
        this.location = location;
        this.id = weaponID;
        weapon = SplatoonWeaponRegistry.getDummy(id);
        drop();

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private boolean used = false;
    public void restore() {

        if(used) {

            reset();
            used = false;

        }

    }

    @Override
    public void onTick() {

        if(!used) {

            for (SplatoonPlayer player : getMatch().getAllPlayers()) {

                if (player.getLocation().distance(item.getLocation()) < .75) {

                    item.remove();
                    player.sendMessage("");

                    String weaponName = "Primärwaffe";
                    if (weapon instanceof SplatoonSecondaryWeapon) {

                        weaponName = "Sekundärwaffe";
                        player.getEquipment().setSecondaryWeapon(weapon.getID());

                    } else if (weapon instanceof SplatoonSpecialWeapon) {

                        weaponName = "Spezialwaffe";
                        player.getEquipment().setSpecialWeapon(weapon.getID());
                        player.setSpecialPoints(9999);

                    } else {

                        player.getEquipment().setPrimaryWeapon(weapon.getID());

                    }

                    if(player instanceof SplatoonHumanPlayer) {

                        ((SplatoonHumanPlayer)player).getPlayer().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                    }
                    player.sendMessage(" §8§l> §e§l" + weapon.getName() + " §7als §e" + weaponName + " §7erhalten!");
                    player.sendMessage("");
                    used = true;

                }

            }

        }

    }

    public void drop() {

        item = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
        item.setItemStack(weapon.asItemStack());
        item.setGravity(false);
        item.setVelocity(new Vector(0,0,0));
        item.setPickupDelay(999999);
        item.setCustomNameVisible(true);
        item.setCustomName("§e§l" + weapon.getName());

    }

    @Override
    public void reset() {

        if(item == null || item.isDead()) {

            drop();

        }

    }

}
