package de.xenyria.splatoon.game.projectile;

import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.equipment.weapon.SplatoonWeapon;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import org.bukkit.Location;

public abstract class SplatoonProjectile {

    private Match match;
    public Match getMatch() { return match; }

    private SplatoonWeapon weapon;
    public SplatoonWeapon getWeapon() { return weapon; }

    private SplatoonPlayer shooter;
    public SplatoonPlayer getShooter() { return shooter; }

    public Color getColor() { return getTeam().getColor(); }

    public void setTeam(Team team) { this.team = team; }
    private Team team;

    public Team getTeam() {

        if(shooter != null) {

            return shooter.getTeam();

        } else {

            return team;

        }

    }

    public abstract Location getLocation();

    private DamageReason reason = DamageReason.WEAPON;
    public DamageReason getReason() { return reason; }
    public void setReason(DamageReason reason) { this.reason = reason; }

    public SplatoonProjectile(SplatoonPlayer shooter, SplatoonWeapon weapon, Match match) {

        this.match = match;
        this.shooter = shooter;
        this.weapon = weapon;

    }

    private boolean toRemove = false;
    public void remove() { toRemove = true; onRemove(); }
    public abstract void onRemove();
    public boolean isRemoved() { return toRemove; }
    public abstract void tick();
    public abstract AxisAlignedBB aabb();

}
