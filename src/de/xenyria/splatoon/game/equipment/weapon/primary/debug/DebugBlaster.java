package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBlaster;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public class DebugBlaster extends AbstractBlaster {

    public DebugBlaster() {

        super(18, "Protoblaster", 2.75f, 110, 1000, 13, 6);

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {
        return ResourcePackItemOption.BLASTER;
    }

    @Override
    public Brand getBrand() {
        return Brand.PROTO;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(12d);

    }

    @Override
    public void shoot() {

    }

}
