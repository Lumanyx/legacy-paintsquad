package de.xenyria.splatoon.game.equipment.weapon.primary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.primary.PrimaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.primary.AbstractBrella;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;

public class DebugBrella extends AbstractBrella {

    public DebugBrella() {
        super(17, "Protopluviator", 2400, 200, 1200, 7, 52, 0.65, 15);
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.BRELLA;
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

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.STICK;
    }

    @Override
    public void shoot() {

    }
}
