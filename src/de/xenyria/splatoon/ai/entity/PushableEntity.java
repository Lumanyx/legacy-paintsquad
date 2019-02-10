package de.xenyria.splatoon.ai.entity;

import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public interface PushableEntity {

    public void push(double x, double y, double z);
    public Location getLocation();
    public AxisAlignedBB getBoundingBox();

    public static PushableEntity wrapPlayer(Player player) {

        return new PushableEntity() {

            @Override
            public void push(double x, double y, double z) {

                player.setVelocity(player.getVelocity().add(new Vector(x,y,z)));

            }

            @Override
            public Location getLocation() {
                return player.getLocation();
            }

            @Override
            public AxisAlignedBB getBoundingBox() {
                return ((CraftPlayer)player).getHandle().getBoundingBox();
            }

        };

    }

}
