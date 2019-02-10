package de.xenyria.splatoon.game.equipment.weapon.viewmodel;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.xenyria.splatoon.game.equipment.weapon.util.ResourcePackUtil;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.structure.Structure;
import de.xenyria.structure.cache.StructureCache;
import de.xenyria.structure.editor.point.Point;
import de.xenyria.util.math.LocalCoordinateSystem;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class WeaponModel {

    private Structure structure;
    private HashMap<Point, ArmorStand> specialPoints = new HashMap<>();
    private HashMap<Point, ArmorStand> standPointMap = new HashMap<>();
    public Map<Point, ArmorStand> getSpecialPoints() { return specialPoints; }

    private boolean nonResourcepackExclusive = false;
    public boolean isNonResourcepackExclusive() { return nonResourcepackExclusive; }
    public void setNonResourcepackExclusive(boolean val) { this.nonResourcepackExclusive = val; }

    public HashMap<Point, ArmorStand> getStandPointMap() { return standPointMap; }

    private SplatoonPlayer player;
    public SplatoonPlayer getPlayer() { return player; }

    private Location currentLocation;

    public void moveByDelta(double x, double y, double z) {

        currentLocation.add(x,y,z);
        rotate(0f);

    }

    private long lastMove;
    public void moveToPosition(double tX, double tY, double tZ) {

        double[] prevX = new double[standPointMap.size()];
        double[] prevY = new double[standPointMap.size()];
        double[] prevZ = new double[standPointMap.size()];

        if(lagCompensation) {

            int i = 0;
            for (ArmorStand stand : standPointMap.values()) {

                prevX[i] = stand.getLocation().getX();
                prevY[i] = stand.getLocation().getY();
                prevZ[i] = stand.getLocation().getZ();
                i++;

            }

        }

        Location old = currentLocation.clone();
        double deltaX = tX - currentLocation.getX();
        double deltaY = tY - currentLocation.getY();
        double deltaZ = tZ - currentLocation.getZ();
        if(deltaX == 0 && deltaY == 0 && deltaZ == 0) { return; }
        currentLocation.add(deltaX, deltaY, deltaZ);

        lastMove = System.currentTimeMillis();
        teleportFlag = false;
        rotate(0f);

        if(lagCompensation && getPlayer() instanceof SplatoonHumanPlayer) {


            SplatoonHumanPlayer player = ((SplatoonHumanPlayer)getPlayer());
            if(nonResourcepackExclusive && ResourcePackUtil.hasCustomResourcePack(player.getPlayer())) {

                return;

            }

            int i = 0;
            for (ArmorStand stand : standPointMap.values()) {

                double interpolMod = 1.8d;

                PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(((CraftArmorStand) stand).getHandle());
                PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT, teleport);
                container.getDoubles().write(0, stand.getLocation().getX() + (deltaX * interpolMod));
                container.getDoubles().write(1, stand.getLocation().getY());
                container.getDoubles().write(2, stand.getLocation().getZ() + (deltaZ * interpolMod));
                try {

                    ProtocolLibrary.getProtocolManager().sendServerPacket(player.getPlayer(), container);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                i++;

            }

        }

    }
    public double getRotation() { return yRotation; }

    private boolean teleportFlag = false;
    public void tick() {

        if(lagCompensation && getPlayer() instanceof SplatoonHumanPlayer) {

            SplatoonHumanPlayer player = ((SplatoonHumanPlayer)getPlayer());
            if(nonResourcepackExclusive && ResourcePackUtil.hasCustomResourcePack(player.getPlayer())) {

                return;

            }

            long time = System.currentTimeMillis() - lastMove;
            if (time > 80 && !teleportFlag) {

                teleportFlag = true;

                for (ArmorStand stand : standPointMap.values()) {

                    ((CraftPlayer) player.getPlayer()).getHandle().playerConnection.sendPacket(
                            new PacketPlayOutEntityTeleport(((CraftArmorStand) stand).getHandle())
                    );

                }

            }

        }
        onTick();

    }

    public abstract void onTick();

    public void removeForcefully() {

        active = false;
        for(ArmorStand stand : standPointMap.values()) {

            stand.remove();
            NMSUtil.broadcastEntityRemovalToSquids(stand);

        }

    }

    public void remove() {

        active = false;
        if(invisibilityInsteadOfRemoval) {

            for(ArmorStand stand : standPointMap.values()) {

                stand.setHelmet(null);

            }

        } else {

            for(ArmorStand stand : standPointMap.values()) {

                stand.remove();
                NMSUtil.broadcastEntityRemovalToSquids(stand);

            }

        }


    }

    private boolean invisibilityInsteadOfRemoval = false;
    public void useInvisibilityInsteadOfRemoval() { invisibilityInsteadOfRemoval = true; }

    private float yRotation;
    public void rotateTowards(float yaw) {

        this.yRotation = yaw;
        rotate(0f);

    }
    public abstract double yOffset();

    public void rotate(float yaw) {

        yRotation+=yaw;
        for(Map.Entry<Point, ArmorStand> entry : standPointMap.entrySet()) {

            int rotY = (int) (yRotation + entry.getKey().y_rotation);
            Location location = currentLocation.clone();
            location.setYaw(rotY);

            LocalCoordinateSystem lcs = new LocalCoordinateSystem();
            lcs.calculate(0, yRotation, 0, .625);
            Location startLocation = currentLocation.clone().set(0,0,0);
            startLocation = startLocation.add(currentLocation.toVector());

            startLocation.add(lcs.getForward().clone().multiply(entry.getKey().getX()));
            startLocation.add(lcs.getUpwards().clone().multiply(entry.getKey().getY()));
            startLocation.add(lcs.getSideways().clone().multiply(entry.getKey().getZ()));
            startLocation.setYaw((float) ((double) ((float) this.yRotation) + entry.getKey().y_rotation));
            startLocation = startLocation.add(0, -1.6 + yOffset(), 0);

            EntityArmorStand stand = ((CraftArmorStand)entry.getValue()).getHandle();
            if(lagCompensation) {

                stand.locX = startLocation.getX();
                stand.locY = startLocation.getY();
                stand.locZ = startLocation.getZ();
                stand.lastX = startLocation.getX();
                stand.lastY = startLocation.getY();
                stand.lastZ = startLocation.getZ();
                stand.yaw = startLocation.getYaw();

            } else {

                entry.getValue().teleport(startLocation);

            }

        }

    }

    private boolean lagCompensation;
    public void useLagCompensation(boolean lagCompensation) { this.lagCompensation = lagCompensation; }

    private boolean active;
    public boolean isActive() { return active; }

    public abstract void handleSpecialPoint(Point point, ArmorStand stand);

    public static CopyOnWriteArrayList<Integer> resourcePackIgnoreIDs = new CopyOnWriteArrayList<>();

    public void spawn() {

        active = true;
        for(Point point : structure.getPoints()) {

            net.minecraft.server.v1_13_R2.World world = ((CraftWorld)currentLocation.getWorld()).getHandle();
            ArmorStand stand = null;
            if(invisibilityInsteadOfRemoval) {

                stand = standPointMap.getOrDefault(point, null);
                if(stand == null) {

                    //stand = (ArmorStand) currentLocation.getWorld().spawnEntity(currentLocation, EntityType.ARMOR_STAND);
                    EntityArmorStand stand1 = new EntityArmorStand(world,
                            currentLocation.getX(), currentLocation.getY(), currentLocation.getZ());
                    stand1.setPositionRotation(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ(), currentLocation.getYaw(), currentLocation.getPitch());

                    if(nonResourcepackExclusive) {

                        resourcePackIgnoreIDs.add(stand1.getId());

                    }

                    stand1.getWorld().addEntity(stand1);stand.setVisible(false);
                    stand = (ArmorStand) stand1.getBukkitEntity();

                    stand.setVisible(false);
                    stand.setCanMove(false);
                    stand.setCanTick(false);
                    stand.setHelmet(new ItemStack(point.getMaterial()));
                    stand.setSmall(point.isSmall());
                    stand.setHeadPose(stand.getHeadPose().setX(-Math.toRadians(point.z_rotation)).setZ(Math.toRadians(point.x_rotation)));

                    if(point.isSpecial()) { specialPoints.put(point, stand); stand.setHelmet(null); handleSpecialPoint(point, stand); }
                    standPointMap.put(point, stand);

                } else {

                    if(point.isSpecial()) { stand.setHelmet(null); handleSpecialPoint(point, stand); } else {

                        stand.setHelmet(new ItemStack(point.getMaterial()));

                    }

                }

            } else {

                EntityArmorStand stand1 = new EntityArmorStand(world,
                        currentLocation.getX(), currentLocation.getY(), currentLocation.getZ());
                stand1.setPositionRotation(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ(), currentLocation.getYaw(), currentLocation.getPitch());

                if(nonResourcepackExclusive) {

                    resourcePackIgnoreIDs.add(stand1.getId());

                }

                stand1.getWorld().addEntity(stand1);
                stand = (ArmorStand) stand1.getBukkitEntity();
                stand.setVisible(false);

                stand = (ArmorStand) currentLocation.getWorld().spawnEntity(currentLocation, EntityType.ARMOR_STAND);
                stand.setVisible(false);
                stand.setCanMove(false);
                stand.setCanTick(false);
                stand.setHelmet(new ItemStack(point.getMaterial()));
                stand.setSmall(point.isSmall());
                stand.setHeadPose(stand.getHeadPose().setX(-Math.toRadians(point.z_rotation)).setZ(Math.toRadians(point.x_rotation)));

                if (point.isSpecial()) {
                    specialPoints.put(point, stand);
                    stand.setHelmet(null);
                    handleSpecialPoint(point, stand);
                }
                standPointMap.put(point, stand);

            }

        }
        rotate(0f);

    }

    public WeaponModel(SplatoonPlayer player, String modelName, World world, Location location) {

        this.player = player;
        location.setYaw(yRotation);
        currentLocation = location.clone();
        structure = (Structure) StructureCache.getCachedProjectData().get(modelName);
        structure.generateCache();

        for(Point point : structure.getPoints()) {

            if(point.isSpecial()) {

                specialPoints.put(point, null);

            }

        }

    }

}
