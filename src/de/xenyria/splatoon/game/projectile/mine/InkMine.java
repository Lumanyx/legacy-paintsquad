package de.xenyria.splatoon.game.projectile.mine;

import de.xenyria.math.trajectory.Trajectory;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.weapon.ai.AIThrowableBomb;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.objects.ObjectType;
import de.xenyria.splatoon.game.objects.RemovableGameObject;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class InkMine extends GameObject {

    private SplatoonPlayer owner;
    private Location location;
    public InkMine(Match match, SplatoonPlayer owner, Block mountedOn) {

        super(match);
        this.location = mountedOn.getLocation().clone().add(.5, .5, .5);
        this.owner = owner;
        Block below = location.getBlock().getRelative(BlockFace.DOWN);
        getMatch().paint(owner, below.getLocation().toVector(), owner.getTeam());

    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.HITBOX;
    }

    private int ticksToLive = (20*60);

    public boolean isDetonated() {

        return detonated;

    }
    @Override
    public void onTick() {

        if(!detonated) {

            ticksToLive--;
            if (ticksToLive < 1 || enemyNearby()) {

                detonate();

            }

        }

    }
    public boolean enemyNearby() {

        for(Team team : getMatch().getEnemyTeams(owner.getTeam())) {

            for(SplatoonPlayer player : getMatch().getPlayers(team)) {

                if(!player.isSplatted() && player.getLocation().distance(location) <= 1d) {

                    return true;

                }

            }

        }
        return false;

    }

    @Override
    public void reset() {

    }

    @Override
    public void onRemove() {

        if(!detonated) {

            detonated = true;

        }

    }

    private boolean detonated = false;

    public void detonate() {

        if(!detonated) {

            detonated = true;
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 0);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {
                getMatch().getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.7f);
            }, 5l);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {
                getMatch().getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }, 10l);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {
                getMatch().getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.4f);
            }, 15l);
            Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                if(!getMatch().inIntro() && !getMatch().inOutro()) {

                    getMatch().getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                    BombProjectile projectile = new BombProjectile(owner, owner.getEquipment().getSecondaryWeapon(), getMatch(), 4f, 0, 140f, false);
                    projectile.spawn(0, location);
                    getMatch().queueObjectRemoval(this);

                }

            }, 20l);

        }

    }

}
