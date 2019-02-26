package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.objects.beacon.BeaconObject;
import de.xenyria.splatoon.game.objects.beacon.JumpPoint;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;

public class Beacon extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    public static final int ID = 14;

    public Beacon() {
        super(ID, "Sprungboje");
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

            place(delayedPosition.getBlockX(), delayedPosition.getBlockY(), delayedPosition.getBlockZ());

        }

    }

    private boolean delayPlace = false;
    private Location delayedPosition;
    private int placeDelay;

    private BeaconObject object;

    public void deploy() {

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

    @Override
    public void asyncTick() {

        if(getPlayer().hasEnoughInk((float) getNextInkUsage()) && isSelected() && getPlayer().isShooting() && getPlayer().hasControl() && getPlayer().isOnGround()) {

            deploy();

        }

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(75d);

    }

    public boolean beaconActive() {

        return object == null || object.isDead();

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.BEACON;
    }

    @Override
    public void shoot() {

    }

    @Override
    public void throwBomb(Location target, Trajectory trajectory) {

    }

    @Override
    public double getImpulse() {
        return 0;
    }

    public void place(int x, int y, int z) {

        if(object != null && !object.isDead()) {

            object.remove();

        }

        Block block = getPlayer().getWorld().getBlockAt(x,y,z);

        object = new BeaconObject(getPlayer().getMatch(), getPlayer(), block);
        getPlayer().getMatch().addGameObject(object);
        delayPlace = false;
        placeDelay = 20;
        getPlayer().getMatch().colorSquare(delayedPosition.getBlock(), getPlayer().getTeam(), getPlayer(), 2);

        for(SplatoonPlayer player : getPlayer().getMatch().getPlayers(getPlayer().getTeam())) {

            if(player != getPlayer()) {

                player.sendMessage(Chat.SYSTEM_PREFIX + getPlayer().coloredName() + " §7hat eine §eSprungboje §7aufgestellt.");
                player.sendMessage("§8§m--§r§8» §7Du kannst jederzeit einen Supersprung zur Sprungboje über das Sprungmenü durchführen.");

            }

        }

    }

}
