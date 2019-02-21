package de.xenyria.splatoon.game.equipment.weapon.secondary.debug;

import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;

public class Beacon extends SplatoonSecondaryWeapon {

    public Beacon() {
        super(14, "Sprungboje");
    }

    public void cleanUp() {

        delayPlace = false;

    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.JUMPPOINT;
    }

    @Override
    public Brand getBrand() {

        return Brand.NOT_BRANDED;

    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    @Override
    public void syncTick() {

        if(placeDelay > 0) { placeDelay--; }

        if(delayPlace) {

            BeaconObject object = new BeaconObject(getPlayer().getMatch(), getPlayer(), delayedPosition.getBlock());
            getPlayer().getMatch().addGameObject(object);
            delayPlace = false;
            placeDelay = 300;
            getPlayer().getMatch().colorSquare(delayedPosition.getBlock(), getPlayer().getTeam(), getPlayer(), 2);

        }

    }

    private boolean delayPlace = false;
    private Location delayedPosition;
    private int placeDelay;

    @Override
    public void asyncTick() {

        if(getPlayer().hasEnoughInk((float) getNextInkUsage()) && isSelected() && getPlayer().isShooting() && getPlayer().hasControl() && getPlayer().isOnGround()) {

            RayTraceResult result = getPlayer().getWorld().rayTraceBlocks(getPlayer().getEyeLocation(),
                    getPlayer().getLocation().getDirection(), 4.5);

            if(placeDelay > 0 || delayPlace) { return; }

            if(result != null) {

                Block block = result.getHitBlock();
                Block target = block.getRelative(result.getHitBlockFace());
                if(target.getType() == Material.AIR && target.getRelative(BlockFace.DOWN).getType().isSolid()) {

                    Block below = target.getRelative(BlockFace.DOWN);
                    if(below.getType() != Material.BEACON) {

                        getPlayer().removeInk(getNextInkUsage());
                        delayPlace = true;
                        delayedPosition = target.getLocation();

                    }

                } else {

                    getPlayer().sendActionBar("§cSprungboje kann nicht platziert werden!");

                }

            } else {

                getPlayer().sendActionBar("§cSprungboje kann nicht platziert werden!");

            }

        }

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
        return Material.BEACON;
    }

    @Override
    public void shoot() {

    }
}
