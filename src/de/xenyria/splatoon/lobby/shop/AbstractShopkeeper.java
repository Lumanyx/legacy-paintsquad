package de.xenyria.splatoon.lobby.shop;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.xenyria.splatoon.game.util.VectorUtil;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.PlayerInteractManager;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

public abstract class AbstractShopkeeper {


    private static ArrayList<AbstractShopkeeper> shopkeepers = new ArrayList<>();
    public static ArrayList<AbstractShopkeeper> getShopkeepers() { return shopkeepers; }

    private Villager villager;
    public Villager getVillager() { return villager; }

    private World world;
    private Location location;

    public AbstractShopkeeper(Location location, String name) {

        world = location.getWorld();
        this.location = location;

        villager = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        villager.setCustomNameVisible(true);
        villager.setCustomName(name);
        villager.setAI(false);
        villager.setCollidable(false);
        shopkeepers.add(this);

    }

    public static void tickShopKeepers() {

        for(AbstractShopkeeper shopkeeper : shopkeepers) {

            shopkeeper.tick();

        }

    }

    public void tick() {

        for(Player player : world.getPlayers()) {

            if(player.getLocation().distance(location) <= 6d) {

                Vector target = player.getEyeLocation().toVector().subtract(new Vector(0, 0.2, 0));

                Vector direction = player.getEyeLocation().toVector().subtract(villager.getEyeLocation().toVector()).normalize();
                if(VectorUtil.isValid(direction)) {

                    Location location = villager.getLocation();
                    location.setDirection(direction);
                    villager.teleport(location);

                }

                return;

            }

        }

    }

    public abstract void onInteraction(Player player);

    public int getEntityID() { return villager.getEntityId(); }

}
