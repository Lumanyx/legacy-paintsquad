package de.xenyria.splatoon.game.equipment.weapon.special.inkstorm;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.inkstorm.InkStormProjectile;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class InkStorm extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public static final int ID = 34;

    public InkStorm() {
        super(ID, "Tintenschauer", "§7Erschafft eine Tintenwolke", 1);
    }


    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {

    }

    @Override
    public void asyncTick() {

        if(isSelected() && getPlayer().isShooting()) {

            if(getPlayer().isSpecialReady()) {

                getPlayer().resetSpecialGauge();
                getPlayer().addInk(100);

                Bukkit.getScheduler().runTask(XenyriaSplatoon.getPlugin(), () -> {

                    activateCall();

                });

            } else {

                getPlayer().specialNotReady();

            }

        }

    }

    public void activateCall() {

        InkStormProjectile projectile = new InkStormProjectile(getPlayer(), this, getPlayer().getMatch());
        projectile.spawn(getPlayer().getEyeLocation());

        getPlayer().getMatch().queueProjectile(projectile);
        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7setzt den §eTintenschauer §7ein!");

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.SNOWBALL;
    }

    @Override
    public void shoot() {

    }

    @Override
    public void activate() {

        activateCall();

    }
}
