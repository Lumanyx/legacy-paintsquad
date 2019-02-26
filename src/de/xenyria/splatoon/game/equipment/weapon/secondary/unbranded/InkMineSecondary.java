package de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded;

import de.xenyria.core.chat.Chat;
import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.game.equipment.Brand;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SecondaryWeaponType;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.mine.InkMine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class InkMineSecondary extends SplatoonSecondaryWeapon implements AIThrowableBomb {

    public static final int ID = 43;

    public InkMineSecondary() {
        super(ID, "Tintenmine");
    }

    @Override
    public SecondaryWeaponType getSecondaryWeaponType() {
        return SecondaryWeaponType.GRENADE;
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

    }

    private InkMine oldMine = null;

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting()) {

            layMine();

        }

    }
    public void layMine() {

        if(getPlayer().hasEnoughInk((float) getNextInkUsage())) {

            Block block = getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
            if(getPlayer().getMatch().isPaintable(getPlayer().getTeam(), block.getX(), block.getY(), block.getZ()) || getPlayer().getMatch().isOwnedByTeam(block, getPlayer().getTeam())) {

                if (oldMine != null) {

                    if (!oldMine.isDetonated()) {

                        oldMine.detonate();

                    }
                    oldMine = null;

                }
                getPlayer().removeInk(getNextInkUsage());
                InkMine mine = new InkMine(getPlayer().getMatch(), getPlayer(), block.getRelative(BlockFace.UP));
                getPlayer().getMatch().addGameObject(mine);
                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                    player.getPlayer().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                }
                oldMine = mine;
                getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Mine gelegt!");

            }

        }

    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public void calculateNextInkUsage() {

        setNextInkUsage(70d);

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.TNT;
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

    public boolean isMineSet() {

        return oldMine == null || oldMine.isDetonated();

    }

}
