package de.xenyria.splatoon.game.equipment.weapon.special.armor;

import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import java.util.Random;

public class InkArmor extends SplatoonSpecialWeapon {

    public InkArmor() {
        super(22, "Tintenrüstung", "§7Du und Teammitglieder in deiner Nähe\n§7erhalten eine Rüstung zum Abwehren\n§7gegnerischer Tinte.", 180);
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

        if(ticksToActivation > 0) {

            ticksToActivation--;
            if(ticksToActivation < 1) {

                for(SplatoonPlayer player : getPlayer().getMatch().getPlayers(getPlayer().getTeam())) {

                    Vector vector = getPlayer().getLocation().toVector();
                    if(vector.distance(player.getLocation().toVector()) <= 24D) {

                        if(!player.isSplatted()) {

                            if(player != getPlayer()) {

                                player.sendMessage(Chat.SYSTEM_PREFIX + "Du erhältst eine Tintenrüstung von " + getPlayer().getColor().prefix() + getPlayer().getName() + "§7!");

                            }

                            for(int i = 0; i < 16; i++) {

                                double offsetX = new Random().nextDouble() * .1;
                                double offsetY = new Random().nextDouble() * player.getHeight();
                                double offsetZ = new Random().nextDouble() * .1;

                                if(new Random().nextBoolean()) { offsetX*=-1; offsetX-=0.3; } else {

                                    offsetX+=.3;

                                }
                                if(new Random().nextBoolean()) { offsetZ*=-1; offsetZ-=0.3; } else {

                                    offsetZ+=.3;

                                }
                                SplatoonServer.broadcastColorizedBreakParticle(getPlayer().getWorld(), player.getLocation().getX() + offsetX, player.getLocation().getY() + offsetY, player.getLocation().getZ() + offsetZ, player.getColor());

                            }
                            player.setArmorHealth(100d, true);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);

                        }

                    }

                }

                ticksToActivation = 0;

            }

        }

    }

    private int ticksToActivation;

    @Override
    public void asyncTick() {

        if(isSelected() && getPlayer().isShooting()) {

            if(getPlayer().isSpecialReady()) {

                ticksToActivation = 20;
                getPlayer().resetSpecialGauge();

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                    player.getPlayer().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 2f);

                }

            } else {

                getPlayer().specialNotReady();

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
        return Material.BEDROCK;
    }

    @Override
    public void shoot() {

    }
}
