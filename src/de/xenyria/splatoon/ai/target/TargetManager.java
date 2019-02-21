package de.xenyria.splatoon.ai.target;

import com.mysql.fabric.xmlrpc.base.Array;
import de.xenyria.core.math.AngleUtil;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.ai.pathfinding.PathfindingTarget;
import de.xenyria.splatoon.ai.pathfinding.SquidAStar;
import de.xenyria.splatoon.ai.pathfinding.grid.Node;
import de.xenyria.splatoon.ai.pathfinding.path.NodePath;
import de.xenyria.splatoon.ai.pathfinding.worker.PathfindingManager;
import de.xenyria.splatoon.ai.weapon.AIWeaponManager;
import de.xenyria.splatoon.game.equipment.weapon.special.stingray.StingRay;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Squid;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class TargetManager {

    public void removePotentialTarget(SplatoonPlayer player) {

        Iterator<PotentialTarget> iterator = possibleTargets.iterator();
        while (iterator.hasNext()) {

            PotentialTarget target1 = iterator.next();
            if(target1.target.equals(player)) { iterator.remove(); }

        }

    }

    public void resetTarget() {

        if(target != null) { removePotentialTarget(target.enemy); }

        target = null;

    }

    public ArrayList<SplatoonPlayer> nearbyThreats(Vector vector, double radius) {

        ArrayList<SplatoonPlayer> players = new ArrayList<>();
        for(SplatoonPlayer player : npc.getMatch().getAllPlayers()) {

            if(!player.isSpectator() && player.getTeam() != npc.getTeam()) {

                if(!player.isSplatted() && player.getLocation().toVector().distance(vector) <= radius) {

                    players.add(player);

                }

            }

        }
        return players;

    }

    public void reset() {

        target = null;
        possibleTargets.clear();

    }

    public PotentialTarget getMostImportantThreat() {

        double lowest = 0d;
        PotentialTarget lowestPlayer = null;
        for(PotentialTarget player : possibleTargets) {

            double newDist = player.target.getLocation().distance(npc.getLocation());
            if(lowestPlayer == null || newDist < lowest) {

                lowest = newDist;
                lowestPlayer = player;

            }

        }
        return lowestPlayer;

    }

    public boolean hasPotentialTarget() {

        return !possibleTargets.isEmpty();

    }

    public boolean hasTarget() { return target != null; }

    public void punishTarget(SplatoonPlayer enemy) {

        pathNotFoundPunishTicks.put(enemy, 20);

    }


    public static class Target {

        // Konstruktor für Spots
        public Target(SplatoonPlayer player, Location position, int ticks) {

            this.enemy = player;
            this.lastKnownLocation = position;
            this.ticksToDismiss = ticks;
            visible = true;

        }

        private SplatoonPlayer enemy;
        public SplatoonPlayer getEnemy() { return enemy; }

        private Location lastKnownLocation;
        public Location getLastKnownLocation() { return lastKnownLocation; }

        private int ticksToDismiss;
        public int getTicksToDismiss() { return ticksToDismiss; }

        private boolean visible;
        public boolean isVisible() { return visible; }

        public boolean isDead() { return enemy.isSplatted() || !enemy.isValid(); }

    }

    // Primärziel
    private Target target;
    public Target getTarget() { return target; }

    public static double MAX_ENEMY_DETECTION_DISTANCE = 21d;
    public static double MAX_YAW_DIFFERENCE = 180d;
    public static double MAX_PITCH_DIFFERENCE = 70d;

    public void target(SplatoonPlayer player) {

        this.target = new Target(player, player.centeredHeightVector().toLocation(npc.getWorld()), 30);

    }

    private EntityNPC npc;
    public TargetManager(EntityNPC npc) {

        this.npc = npc;

    }

    private int targetFindTicker = 0;

    public class ReachEntityTarget implements PathfindingTarget {

        private EntityNPC npc;
        private SplatoonPlayer target;
        private AIWeaponManager.AIPrimaryWeaponType weaponType;

        public ReachEntityTarget(EntityNPC npc, SplatoonPlayer player) {

            this.npc = npc;
            target = player;
            weaponType = npc.getWeaponManager().getAIPrimaryWeaponType();

        }

        public boolean needsUpdate(Vector vector) { return false; }
        public boolean isReached(SquidAStar pathfinder, Node node, Vector vector) {

            boolean usingStingray = npc.getEquipment().getSpecialWeapon() != null && npc.getEquipment().getSpecialWeapon() instanceof StingRay && npc.getEquipment().getSpecialWeapon().isActive();
            if(usingStingray) { return npc.hasLineOfSight(target); }

            if(weaponType == AIWeaponManager.AIPrimaryWeaponType.SHOOTER) {

                return node.toVector().distance(target.getLocation().toVector()) <= (npc.getWeaponManager().maxWeaponDistance() * 1.5) &&
                        npc.getWeaponManager().canHitEntity(npc.getShootingLocation(vector, npc.getWeaponManager().getCurrentHandBoolean()), target.centeredHeightVector().toLocation(npc.getWorld()), target) &&
                        lineOfSight(vector, target);

            } else if (weaponType == AIWeaponManager.AIPrimaryWeaponType.ROLLER) {

                return node.toVector().distance(target.getLocation().toVector()) <= 0.5d && lineOfSight(vector, target);

            } else if (weaponType == AIWeaponManager.AIPrimaryWeaponType.CHARGER) {

                return npc.hasLineOfSight(target) && node.toVector().distance(target.getLocation().toVector()) < (npc.getWeaponManager().maxWeaponDistance()-1.8);

            }
            return false;

        }

        @Override
        public boolean useGoalNode() {
            return false;
        }

        @Override
        public SquidAStar.MovementCapabilities getMovementCapabilities() {
            return new SquidAStar.MovementCapabilities();
        }

        @Override
        public void beginPathfinding() {

        }

        @Override
        public void endPathfinding() {

        }

        @Override
        public int maxNodeVisits() {
            return 140;
        }

        @Override
        public NodeListener getNodeListener() {
            return null;
        }

        @Override
        public Vector getEstimatedPosition() {
            return target.getLocation().toVector();
        }
    }

    private HashMap<SplatoonPlayer, Integer> pathNotFoundPunishTicks = new HashMap<>();
    private HashMap<SplatoonPlayer, SquidAStar> enemyQuery = new HashMap<>();

    public class PotentialTarget {

        public SplatoonPlayer target;
        public SquidAStar path;

        public PotentialTarget(SplatoonPlayer target, SquidAStar path) {

            this.target = target;
            this.path = path;

        }

    }

    private ArrayList<PotentialTarget> possibleTargets = new ArrayList<>();
    public ArrayList<PotentialTarget> getPossibleTargets() { return possibleTargets; }

    public boolean isProcessingFinished() {

        for(SquidAStar aStar : enemyQuery.values()) {

            if(aStar.getRequestResult() != SquidAStar.RequestResult.FOUND && aStar.getRequestResult() != SquidAStar.RequestResult.NOT_FOUND) {

                return false;

            }

        }
        return true;

    }

    public boolean isProcessingEnemyQuery() {

        for(SquidAStar aStar : enemyQuery.values()) {

            if(aStar.getRequestResult() == SquidAStar.RequestResult.PROCESSING) {

                return true;

            }

        }
        return false;

    }

    public void tick() {

        boolean processingEnemyQueries = isProcessingEnemyQuery();

        Iterator<Map.Entry<SplatoonPlayer, Integer>> iterator = pathNotFoundPunishTicks.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<SplatoonPlayer, Integer> entry = iterator.next();
            int newVal = entry.getValue() - 1;
            if(newVal < 1) {

                iterator.remove();

            } else {

                entry.setValue(newVal);

            }

        }


        boolean processingFinished = isProcessingFinished();
        if(target == null || processingFinished) {

            if(!processingEnemyQueries && enemyQuery.isEmpty()) {

                targetFindTicker++;
                if (targetFindTicker > 7) {

                    ArrayList<SplatoonPlayer> nearbyPlayers = new ArrayList<>();
                    targetFindTicker = 0;
                    for (SplatoonPlayer player : npc.getMatch().getAllPlayers()) {

                        if (!player.isSpectator() && !player.isSplatted() && player != npc && player.getTeam() != npc.getTeam()) {

                            if(pathNotFoundPunishTicks.containsKey(player)) {

                                if(target != null && target.enemy.equals(player)) {

                                    removePotentialTarget(target.enemy);
                                    target = null;
                                    continue;

                                }

                            }

                            double distance = npc.getLocation().distance(player.getLocation());
                            boolean highlighted = npc.isHighlighted(player);
                            if (distance < MAX_ENEMY_DETECTION_DISTANCE || highlighted) {

                                // "Frustum" Viewcheck
                                float npcYaw = npc.yaw();
                                Vector directional = player.centeredHeightVector().subtract(npc.getEyeLocation().toVector()).normalize();
                                if (highlighted || VectorUtil.isValid(directional)) {

                                    Location location = new Location(npc.getWorld(), 0, 0, 0);
                                    location.setDirection(directional);

                                    float differenceYaw = Math.abs(AngleUtil.distance(npcYaw, location.getYaw()));
                                    float differencePitch = Math.abs(90f + npc.pitch()) - (location.getPitch() + 90f);

                                    if (highlighted || (differenceYaw < MAX_YAW_DIFFERENCE && differencePitch < MAX_PITCH_DIFFERENCE)) {

                                        // Der Spieler liegt im Sichtfeld - vor einem Raytrace prüfen ob der Spieler sich versteckt
                                        boolean visible = true;
                                        if (player.isSquid()) {

                                            boolean submergedInInk = player.isSubmergedInInk();
                                            if (submergedInInk) {

                                                boolean trailVisible = player.isVisibleByTrail();
                                                if (!trailVisible) {

                                                    visible = false;

                                                }

                                            }

                                        }

                                        if(highlighted) { visible = true; }

                                        if (visible) {

                                            // Raytrace
                                            World world = npc.getWorld();
                                            double dist = player.getLocation().distance(npc.getLocation());

                                            RayTraceResult result = world.rayTraceBlocks(npc.getEyeLocation(), directional, dist);
                                            if (highlighted || (result == null || (result.getHitPosition() != null && result.getHitPosition().distance(npc.getEyeLocation().toVector()) <= dist))) {

                                                nearbyPlayers.add(player);

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                    if(!nearbyPlayers.isEmpty()) {

                        for(SplatoonPlayer player : nearbyPlayers) {

                            ReachEntityTarget target = new ReachEntityTarget(npc, player);
                            SquidAStar aStar = new SquidAStar(npc.getWorld(), npc.getLocation().toVector(), target, npc.getMatch(), npc.getTeam(), 120);
                            aStar.updateCapabilities(target.getMovementCapabilities());
                            enemyQuery.put(player, aStar);
                            PathfindingManager.queueRequest(aStar);

                        }

                    }

                }

            } else if(processingFinished) {

                processingFinished = false;
                ArrayList<SplatoonPlayer> players = new ArrayList<>();
                for(Map.Entry<SplatoonPlayer, SquidAStar> entry : enemyQuery.entrySet()) {

                    if(entry.getValue().getRequestResult() == SquidAStar.RequestResult.FOUND) {

                        players.add(entry.getKey());

                    } else if(entry.getValue().getRequestResult() == SquidAStar.RequestResult.NOT_FOUND) {

                        // TODO Punish ticks
                        pathNotFoundPunishTicks.put(entry.getKey(), 10);

                    }

                }

                possibleTargets.clear();
                if(!players.isEmpty()) {

                    Collections.sort(players, new Comparator<SplatoonPlayer>() {
                        @Override
                        public int compare(SplatoonPlayer o1, SplatoonPlayer o2) {
                            return Double.compare(
                                    o1.getLocation().distance(npc.getLocation()),
                                    o2.getLocation().distance(npc.getLocation()));
                        }
                    });

                    for(SplatoonPlayer player : players) {

                        possibleTargets.add(new PotentialTarget(player, enemyQuery.get(player)));

                    }


                }

                enemyQuery.clear();

            }

        } else {

            // Target Tracking
            if(target.lastKnownLocation.distance(npc.getLocation()) > MAX_ENEMY_DETECTION_DISTANCE) {

                removePotentialTarget(target.enemy);
                target = null;

            } else {

                if (!target.visible) {

                    invisibleTicks++;

                    // Echte Distanz
                    double distance = target.lastKnownLocation.distance(target.enemy.centeredHeightVector().toLocation(npc.getWorld()));
                    if(invisibleTicks > 30) {

                        if(distance > 0.5) {

                            removePotentialTarget(target.enemy);
                            target = null;
                            return;

                        }

                    }

                    if (distance > 1.5) {

                        removePotentialTarget(target.enemy);
                        target = null;

                    } else {

                        if(target.enemy.isSquid()) {

                            if(!target.enemy.isVisibleByTrail() || !target.enemy.isSubmergedInInk()) {

                                if(lineOfSight(target.enemy)) {

                                    target.lastKnownLocation = target.enemy.centeredHeightVector().toLocation(npc.getWorld());
                                    if(npc.estimateEnemyPosition()) {

                                        target.lastKnownLocation = target.lastKnownLocation.add(target.enemy.getLastDelta());

                                    }

                                    target.visible = true;


                                }

                            }

                        } else {

                            if(lineOfSight(target.enemy)) {

                                target.lastKnownLocation = target.enemy.centeredHeightVector().toLocation(npc.getWorld());

                                if(npc.estimateEnemyPosition()) {

                                    target.lastKnownLocation = target.lastKnownLocation.add(target.enemy.getLastDelta());

                                }

                                target.visible = true;

                            }

                        }

                    }

                } else {

                    if(!target.enemy.isSquid()) {

                        if (lineOfSight(target.enemy)) {

                            target.lastKnownLocation = target.enemy.centeredHeightVector().toLocation(npc.getWorld());

                            if(npc.estimateEnemyPosition()) {

                                target.lastKnownLocation = target.lastKnownLocation.add(target.enemy.getLastDelta());

                            }

                        } else {

                            target.visible = false;

                        }

                    } else {

                        if(!target.enemy.isSubmergedInInk() || target.enemy.isVisibleByTrail()) {

                            target.lastKnownLocation = target.enemy.centeredHeightVector().toLocation(npc.getWorld());
                            if(npc.estimateEnemyPosition()) {

                                target.lastKnownLocation = target.lastKnownLocation.add(target.enemy.getLastDelta());

                            }

                        } else {

                            target.visible = false;

                        }

                    }

                }

            }

        }

    }
    private int invisibleTicks = 0;

    public boolean lineOfSight(Vector vector, SplatoonPlayer player) {

        Location targetLoc = player.centeredHeightVector().toLocation(npc.getWorld());
        Location current = vector.toLocation(player.getWorld());
        Vector direction = targetLoc.toVector().clone().subtract(current.toVector()).normalize();

        World world = npc.getWorld();
        RayTraceResult result = world.rayTraceBlocks(current, direction, current.distance(targetLoc));
        if(result == null || (result.getHitPosition() == null)) {

            return true;

        } else {

            return false;

        }

    }

    public boolean lineOfSight(SplatoonPlayer player) {

        return lineOfSight(npc.getEyeLocation().toVector(), player);

    }

}
