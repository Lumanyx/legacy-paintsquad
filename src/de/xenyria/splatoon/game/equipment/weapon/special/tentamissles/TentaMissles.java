package de.xenyria.splatoon.game.equipment.weapon.special.tentamissles;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.xenyria.core.math.AngleUtil;
import de.xenyria.core.math.BitUtil;
import de.xenyria.splatoon.game.color.Color;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.objects.GameObject;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.player.scoreboard.EntityHighlightController;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.projectile.TentaMissleRocket;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.CraftEffect;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;

public class TentaMissles extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public static final int ID = 3;

    public TentaMissles() {

        super(ID, "Schwarmraketen", "§7Markiere bis zu fünf Gegner und zwinge\n§7sie durch Tintenraketen zur Flucht.", 180);

    }


    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    private ArrayList<TentaMissleTarget> confirmedTargets = new ArrayList<>();
    private TentaMissleTarget currentTarget = null;
    private int firedRocketsForTarget = 0;
    private int remainingRockets;
    private int ticksSinceRocketShoot;

    public void cleanUp() {

        remainingRockets = 0;
        confirmedTargets.clear();
        currentTarget = null;

    }

    @Override
    public void syncTick() {

        if(isActive()) {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                int slot = player.getPlayer().getInventory().getHeldItemSlot();
                if (slot < 2) {

                    player.getPlayer().getInventory().setHeldItemSlot(2);

                } else if (slot > 3) {

                    player.getPlayer().getInventory().setHeldItemSlot(3);

                }

            }

        }

        if(aimPhase) {

            if(remainingAimTicks > 0) {

                remainingAimTicks--;
                if(remainingAimTicks < 1) {

                    end();
                    return;

                }

                ArrayList<TentaMissleTarget> targets = getTargetsInSight();
                if(!targets.isEmpty()) {

                    for(TentaMissleTarget target : targets) {

                        if(getPlayer() instanceof SplatoonHumanPlayer) {

                            SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();

                            EntityHighlightController.TeamEntry entry = player.getHighlightController().getEntry(target.getEntityID());
                            if (entry != null) {
                                entry.ticks = 2;
                            } else {

                                if (target.getNMSEntity() instanceof EntityPlayer) {

                                    player.getHighlightController().addEntry(new EntityHighlightController.TeamEntry(
                                            target.getName(), ChatColor.getByChar(target.getTeam().getColor().getColor()), target.getNMSEntity(), 2
                                    ));

                                } else {

                                    player.getHighlightController().addEntry(new EntityHighlightController.TeamEntry(target.getUUID().toString(),
                                            ChatColor.DARK_GRAY, target.getNMSEntity(), 20));

                                }

                            }

                        }

                    }

                    if(isSelected()) {

                        String st = "§6" + targets.size() + " Ziel(e) §7erfasst. Rechtsklick zum §e§labfeuern";
                        getPlayer().sendActionBar(st);

                    } else {

                        getPlayer().sendActionBar("§7Zum anvisieren und abfeuern die Spezialwaffe auswählen.");

                    }

                    if(getPlayer().isShooting() && isSelected()) {

                        aimPhase = false;
                        remainingRockets = targets.size() * 4;
                        confirmedTargets.addAll(targets);
                        shootPhase = true;
                        currentTarget = confirmedTargets.get(0);
                        confirmedTargets.remove(0);
                        ticksSinceRocketShoot = 0;
                        firedRocketsForTarget = 0;
                        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7feuert §e" + (confirmedTargets.size()*4) + " Schwarmraketen §7ab!");

                    }

                } else {

                    getPlayer().sendActionBar("§7Keine Ziele erfasst.");

                }

            }

        } else if(shootPhase) {

            getPlayer().setOverrideWalkSpeed(0.02f);
            if(remainingRockets > 0) {

                ticksSinceRocketShoot++;
                if(firedRocketsForTarget >= 4 && !confirmedTargets.isEmpty()) {

                    firedRocketsForTarget = 0;
                    currentTarget = confirmedTargets.get(0);
                    confirmedTargets.remove(0);

                }
                if (ticksSinceRocketShoot > 2) {

                    getPlayer().sendActionBar("§6" + remainingRockets + " Rakete(n) §7verbleiben...");
                    firedRocketsForTarget++;
                    ticksSinceRocketShoot = 0;
                    remainingRockets--;
                    TentaMissleRocket rocket = new TentaMissleRocket(getPlayer(), getPlayer().getEquipment().getSpecialWeapon(), getPlayer().getMatch());
                    getPlayer().getMatch().queueProjectile(rocket);
                    rocket.launch(getPlayer().getLocation(), currentTarget.getTargetLocationProvider());

                }
                if(remainingRockets < 1) {

                    aimPhase = false;
                    shootPhase = false;
                    confirmedTargets.clear();
                    end();

                }

            }

        }

    }

    public ArrayList<TentaMissleTarget> getTargetsInSight() {

        ArrayList<TentaMissleTarget> targets = new ArrayList<>();

        for(SplatoonPlayer player : getPlayer().getMatch().getAllPlayers()) {

            if(player.getTeam() != getPlayer().getTeam() && !player.isSpectator()) {

                TentaMissleTarget target = (TentaMissleTarget)player;
                if(target.isTargetable() && targets.size() < 5) {

                    Location location = target.getTargetLocationProvider().getLocation();
                    if(location.distance(getPlayer().getLocation()) < 128) {

                        Vector targetVector = location.toVector();
                        Vector currentPosition = getPlayer().getLocation().toVector();
                        Vector direction = targetVector.clone().subtract(currentPosition);

                        Location loc = new Location(getPlayer().getWorld(), 0,0,0);
                        loc.setDirection(direction);

                        float yaw = loc.getYaw();
                        float pitch = loc.getPitch();
                        /*while (yaw > 360) { yaw-=360; }
                        while (yaw < 0) { yaw+=360; }
                        while (pitch < -90) { pitch+=90; }
                        while (pitch > 90) { pitch-=90; }*/

                        // Werte dürfen nicht negativ sein
                        float relYaw = yaw;
                        float relPitch = pitch;

                        // Spielerwerte
                        float pYaw = getPlayer().getLocation().getYaw();
                        float pPitch = getPlayer().getLocation().getPitch();

                        /*
                        while (pYaw > 360) { pYaw-=360f; }
                        while (pYaw < 0) { pYaw+=360f; }
                        while (pPitch > 90) { pPitch-=90f; }
                        while (pPitch < -90) { pPitch+=90f; }

                        pYaw+=360f;
                        pPitch+=90f;*/

                        float differenceYaw = AngleUtil.distance(pYaw, yaw);
                        float differencePitch = Math.abs(pPitch - relPitch);
                        if(differenceYaw < 30 && differencePitch < 30) {

                            targets.add(target);

                        }

                    }

                }

            }

        }

        for(GameObject object : getPlayer().getMatch().getGameObjects()) {

            if(object instanceof TentaMissleTarget) {

                TentaMissleTarget target = (TentaMissleTarget)object;
                if(target.isTargetable() && targets.size() < 5) {

                    Location location = target.getTargetLocationProvider().getLocation();
                    if(location.distance(getPlayer().getLocation()) < 128) {

                        Vector targetVector = location.toVector();
                        Vector currentPosition = getPlayer().getLocation().toVector();
                        Vector direction = targetVector.clone().subtract(currentPosition);

                        Location loc = new Location(getPlayer().getWorld(), 0,0,0);
                        loc.setDirection(direction);

                        float yaw = loc.getYaw();
                        float pitch = loc.getPitch();
                        /*while (yaw > 360) { yaw-=360; }
                        while (yaw < 0) { yaw+=360; }
                        while (pitch < -90) { pitch+=90; }
                        while (pitch > 90) { pitch-=90; }*/

                        // Werte dürfen nicht negativ sein
                        float relYaw = yaw;
                        float relPitch = pitch;

                        // Spielerwerte
                        float pYaw = getPlayer().getLocation().getYaw();
                        float pPitch = getPlayer().getLocation().getPitch();

                        /*
                        while (pYaw > 360) { pYaw-=360f; }
                        while (pYaw < 0) { pYaw+=360f; }
                        while (pPitch > 90) { pPitch-=90f; }
                        while (pPitch < -90) { pPitch+=90f; }

                        pYaw+=360f;
                        pPitch+=90f;*/

                        float differenceYaw = AngleUtil.distance(pYaw, yaw);
                        float differencePitch = Math.abs(pPitch - relPitch);
                        if(differenceYaw < 30 && differencePitch < 30) {

                            targets.add(target);

                        }

                    }

                }

            }

        }
        return targets;

    }

    public void end() {

        getPlayer().sendActionBar(" ");
        if(getPlayer() instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer) getPlayer()).updateInventory();

        }

        getPlayer().disableWalkSpeedOverride();

        aimPhase = false;
        shootPhase = false;

        if(getPlayer() instanceof SplatoonHumanPlayer) {

            Scoreboard scoreboard = ((SplatoonHumanPlayer) getPlayer()).getPlayer().getScoreboard();
            fallbackTeamColor.unregister();
            for(Team team : registeredTeams.values()) { team.unregister(); }

        }

    }

    private Team fallbackTeamColor;
    private HashMap<Color, Team> registeredTeams = new HashMap<>();

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting() && !aimPhase && !shootPhase && isSelected() && getPlayer().hasControl()) {

            if(getPlayer().getSpecialPoints() >= getRequiredPoints()) {

                activateCall();

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                    Scoreboard scoreboard = player.getPlayer().getScoreboard();
                    fallbackTeamColor = scoreboard.registerNewTeam("hE_default");
                    fallbackTeamColor.setColor(ChatColor.DARK_GRAY);
                    for(HitableEntity object : getPlayer().getMatch().getHitableEntities()) {

                        if(object instanceof TentaMissleTarget) {

                            fallbackTeamColor.addEntry(((TentaMissleTarget) object).getUUID().toString());

                        }

                    }

                    for(de.xenyria.splatoon.game.team.Team team1 : getPlayer().getMatch().getRegisteredTeams()) {

                        if(team1 != getPlayer().getTeam()) {

                            Team nTeam = scoreboard.registerNewTeam("hE-" + team1.getColor().name());
                            nTeam.setColor(ChatColor.getByChar(team1.getColor().getColor()));
                            registeredTeams.put(team1.getColor(), nTeam);
                            for(SplatoonPlayer player1 : getPlayer().getMatch().getPlayers(team1)) {

                                // TODO Nickname
                                nTeam.addEntry(player1.getName());

                            }

                        }

                    }

                }

            } else {

                getPlayer().specialNotReady();

            }

        }

    }

    public void activateCall() {

        getPlayer().enableWalkSpeedOverride();
        getPlayer().setOverrideWalkSpeed(0.1f);

        getPlayer().resetSpecialGauge();
        getPlayer().resetLastInteraction();
        aimPhase = true;
        remainingAimTicks = 300;

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
        return Material.FIREWORK_ROCKET;
    }

    private boolean aimPhase, shootPhase;
    private int remainingAimTicks = 0;


    @Override
    public void shoot() {

    }

    @Override
    public boolean isActive() {
        return aimPhase || shootPhase;
    }

    @Override
    public void activate() {

        activateCall();
        ArrayList<TentaMissleTarget> targets = getTargetsInSight();
        aimPhase = false;
        remainingRockets = targets.size() * 4;
        confirmedTargets.addAll(targets);
        shootPhase = true;
        currentTarget = confirmedTargets.get(0);
        confirmedTargets.remove(0);
        ticksSinceRocketShoot = 0;
        firedRocketsForTarget = 0;
        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7feuert §e" + (confirmedTargets.size()*4) + " Schwarmraketen §7ab!");

    }
}
