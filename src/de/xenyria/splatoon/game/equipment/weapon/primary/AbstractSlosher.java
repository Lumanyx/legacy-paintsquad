package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.equipment.weapon.util.SprayUtil;
import de.xenyria.splatoon.game.projectile.ink.InkProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Random;

public abstract class AbstractSlosher extends SplatoonPrimaryWeapon {

    public AbstractSlosher(int id, String name) {

        super(id, name);

    }

    private int queuedSloshes;
    private long sloshDelay = 900;
    private long lastSlosh;
    private double usage = 21d;
    private int projectilesPerSlosh = 6;
    private float maxYawOffset = 21f;
    private float impulse = 0.53f;
    private double damage = 70d;

    public Material getRepresentiveMaterial() {

        return Material.BUCKET;

    }

    public void syncTick() {

        int toSpawn = queuedSloshes;

        for(int i = 0; i < toSpawn; i++) {

            getPlayer().getWorld().playSound(getPlayer().getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, .4f, 0.8f);
            for(int p = 0; p < projectilesPerSlosh; p++) {

                float yaw = SprayUtil.addSpray(getPlayer().getLocation().getYaw(), maxYawOffset);
                float pitch = getPlayer().getLocation().getPitch() - 20f - (10f * new Random().nextFloat());
                if(pitch > 90f) { pitch = 90f; } else if(pitch < -90f) { pitch = -90f; }
                Location location = new Location(getPlayer().getWorld(), 0,0,0);
                location.setYaw(yaw);
                location.setPitch(pitch);

                InkProjectile projectile = new InkProjectile(getPlayer(), this, getPlayer().getMatch());
                projectile.spawn(getPlayer().getShootingLocation(true).clone().add(0, -0.6, 0), yaw, pitch, impulse);
                projectile.setDrippingRatio(20d);
                projectile.withDamage(damage);
                projectile.setPaintBelowRatio(15, 10, 1);
                getPlayer().getMatch().queueProjectile(projectile);

            }

        }

        queuedSloshes-=toSpawn;

    }

    public void asyncTick() {

        if(isSelected() && getPlayer().hasEnoughInk((float) usage) && getPlayer().isShooting()) {

            if(System.currentTimeMillis() > (lastSlosh + sloshDelay)) {

                getPlayer().removeInk(usage);
                queuedSloshes++;
                lastSlosh = System.currentTimeMillis();

            }

        }

    }

}
