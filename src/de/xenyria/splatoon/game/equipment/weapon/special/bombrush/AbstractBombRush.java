package de.xenyria.splatoon.game.equipment.weapon.special.bombrush;

import de.xenyria.core.chat.Chat;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.Autobomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.BurstBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.SplatBomb;
import de.xenyria.splatoon.game.equipment.weapon.secondary.unbranded.SuctionBomb;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.BurstBombProjectile;
import de.xenyria.splatoon.game.projectile.CurlingBombProjectile;
import de.xenyria.splatoon.game.projectile.SuctionBombProjectile;
import de.xenyria.splatoon.game.projectile.autobomb.AutobombProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import org.bukkit.Material;

public abstract class AbstractBombRush extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public void activate() {

        activateCall();

    }

    public static final int REQUIRED_POINTS = 210;

    public enum BombType {

        SPLATBOMB,
        BURSTBOMB,
        SUCTIONBOMB,
        CURLINGBOMB,
        AUTOBOMB;

    }

    private int remainingTicks = 0;
    private boolean throwBomb = false;
    private int ticksToNextBomb = 0;
    private BombType bombType;

    public Material getRepresentiveMaterial() {

        switch (bombType) {

            case CURLINGBOMB: return Material.FLINT;
            case BURSTBOMB: return Material.SUGAR;
            case SUCTIONBOMB: return Material.SLIME_SPAWN_EGG;
            case SPLATBOMB: return Material.GUNPOWDER;
            case AUTOBOMB: return Material.CHICKEN_SPAWN_EGG;

        }
        return Material.AIR;

    }

    public void cleanUp() {

        remainingTicks = 0;
        throwBomb = false;

    }

    public AbstractBombRush(int id, String name, String desc, int requiredPoints, BombType type) {

        super(id, name, desc, requiredPoints);
        bombType = type;

    }

    public void syncTick() {

        if(isActive()) {

            if (getPlayer().isSplatted()) {

                remainingTicks = 0;

            }
            remainingTicks--;
            if(remainingTicks < 1) {

                if(getPlayer() instanceof EntityNPC) { ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd(); }

            }

            if (ticksToNextBomb > 0) {

                ticksToNextBomb--;

            }

            if (throwBomb) {

                ticksToNextBomb = 12;
                throwBomb();
                throwBomb = false;

            }

        }

    }
    public void throwBomb() {

        switch (bombType) {

            case SPLATBOMB:
                BombProjectile projectile = new BombProjectile(getPlayer(), this, getPlayer().getMatch(), SplatBomb.RADIUS, SplatBomb.EXPLOSION_TICKS, SplatBomb.MAX_DAMAGE, true);
                projectile.spawn(SplatBomb.IMPULSE, getPlayer().getShootingLocation(true));
                getPlayer().getMatch().queueProjectile(projectile); break;
            case BURSTBOMB:
                BurstBombProjectile projectile1 = new BurstBombProjectile(getPlayer(), this, getPlayer().getMatch(), BurstBomb.RADIUS, BurstBomb.MAX_DAMAGE);
                projectile1.spawn(BurstBomb.IMPULSE, getPlayer().getShootingLocation(true));
                getPlayer().getMatch().queueProjectile(projectile1); break;
            case SUCTIONBOMB:
                SuctionBombProjectile projectile2 = new SuctionBombProjectile(getPlayer(), this, getPlayer().getMatch());
                projectile2.spawn(SuctionBomb.IMPULSE, getPlayer().getShootingLocation(false), getPlayer().getLocation().getDirection());
                getPlayer().getMatch().queueProjectile(projectile2); break;
            case CURLINGBOMB:
                CurlingBombProjectile projectile3 = new CurlingBombProjectile(getPlayer(), this, getPlayer().getMatch());
                projectile3.spawn(getPlayer().getLocation());
                getPlayer().getMatch().queueProjectile(projectile3); break;
            case AUTOBOMB:
                AutobombProjectile projectile4 = new AutobombProjectile(getPlayer(), this, getPlayer().getMatch());
                projectile4.spawn(Autobomb.IMPULSE, getPlayer().getLocation());
                getPlayer().getMatch().queueProjectile(projectile4); break;

        }

    }

    public void asyncTick() {

        if(getPlayer().isShooting() && isSelected()) {

            if(!isActive()) {

                if (getPlayer().isSpecialReady()) {

                    activateCall();

                }

            } else {

                if(ticksToNextBomb < 1) {

                    throwBomb = true;

                }

            }

        }

        if(isActive()) {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                if (getPlayer().heldItemSlot() != 3 && getPlayer().heldItemSlot() != 2) { player.getPlayer().getInventory().setHeldItemSlot(3); }

            }

        }

    }

    public void activateCall() {

        getPlayer().resetSpecialGauge();
        ticksToNextBomb = 0;
        remainingTicks = 20*7;
        getPlayer().sendMessage(Chat.SYSTEM_PREFIX + "Bombenhagel aktiv! Halte die §erechte Maustaste §7gedrückt um Bomben zu werfen.");

    }

    @Override
    public ResourcePackItemOption getResourcepackOption() {

        return null;

    }

    @Override
    public boolean isActive() {
        return remainingTicks>0;
    }
}
