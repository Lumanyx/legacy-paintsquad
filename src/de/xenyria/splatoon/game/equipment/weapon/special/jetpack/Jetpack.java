package de.xenyria.splatoon.game.equipment.weapon.special.jetpack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.JetpackModel;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.InkjetProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.NMSUtil;
import de.xenyria.structure.editor.point.Point;
import de.xenyria.util.math.LocalCoordinateSystem;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.EnumMoveType;
import net.minecraft.server.v1_13_R2.PacketPlayOutEntityTeleport;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Jetpack extends SplatoonSpecialWeapon implements AISpecialWeapon {

    private JetpackModel model;

    public static final int ID = 6;

    public Jetpack() {

        super(ID, "Tintendüser", "§7Du erhältst Kontrolle über ein tintenbetriebenes Jetpack.\n§eRechtsklick §7feuert ein tödliches Geschoss ab.\n§7Nachdem die Zeit abgelaufen ist springst du zum Ursprungspunkt zurück.", 190);

    }

    @Override
    public void assign(SplatoonPlayer player) {

        super.assign(player);
        model = new JetpackModel(getPlayer(), this);

    }

    private int remainingTicks = 0;

    @Override
    public boolean isActive() {
        return remainingTicks > 0;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    private Location startLocation;
    private Location currentLocation() { return new Location(startLocation.getWorld(), hitboxStand.locX, hitboxStand.locY, hitboxStand.locZ); }

    public double distanceToGround() {

        double y = currentLocation().getY();
        for(int x = 0; x < 30; x++) {

            double checkY = y-x;
            World world = getPlayer().getWorld();

            Block block = world.getBlockAt(
                    ((int)currentLocation().getX()),
                    ((int)checkY),
                    ((int)currentLocation().getZ())
            );
            if(block.getType().isSolid()) {

                return x;

            }

        }
        return 30d;

    }

    private double velocityY = 0d;
    private ArmorStand seat;

    private ArrayList<Point> particleSources = new ArrayList<>();
    public ArrayList<Point> getParticleSources() { return particleSources; }

    public AxisAlignedBB jetpackAABB() {

        if(isActive()) {

            Location location = hitboxStand.getBukkitEntity().getLocation();

            return new AxisAlignedBB(location.getX() - .4, location.getY(), location.getZ() - .4, location.getX() + .4, location.getY() + 2, location.getZ() + .4);

        } else {

            return new AxisAlignedBB(0,0,0,0,0,0);

        }

    }

    @Override
    public void syncTick() {

        if(isActive()) {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                SplatoonHumanPlayer player = (SplatoonHumanPlayer) getPlayer();
                int slot = player.getPlayer().getInventory().getHeldItemSlot();
                if(slot < 2) {

                    player.getPlayer().getInventory().setHeldItemSlot(2);

                } else if(slot > 3) {

                    player.getPlayer().getInventory().setHeldItemSlot(3);

                }

            } else {

                if(seat != null) {

                    EntityNPC npc = (EntityNPC) getPlayer();
                    npc.getNMSEntity().locX = seat.getLocation().getX();
                    npc.getNMSEntity().locY = seat.getLocation().getY();
                    npc.getNMSEntity().locZ = seat.getLocation().getZ();

                }

            }

        }

        if(getPlayer().isSplatted()) {

            remainingTicks = 0;
            if(model.isActive()) { model.remove(); }
            eject();
            return;

        }

        if(remainingTicks > 0) {

            float remSecs = ((float)remainingTicks / 20f);
            DecimalFormat format = new DecimalFormat("#.#");
            String seconds = format.format( remSecs);
            getPlayer().sendActionBar("§e" + seconds + " Sekunde(n) §7verbleiben.");

            lastShootTicks++;
            if(queuedShots > 0 && isSelected() && seat != null) {

                int copy = queuedShots;
                for(int i = 0; i < copy; i++) {

                    InkjetProjectile projectile = new InkjetProjectile(getPlayer(), getPlayer().getEquipment().getSpecialWeapon(), getPlayer().getMatch());
                    projectile.spawn(seat.getLocation().clone().add(0, 2, 0), getPlayer().getLocation().getDirection());
                    getPlayer().getMatch().queueProjectile(projectile);
                    currentLocation().getWorld().playSound(currentLocation(),
                            Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 1f);

                }
                queuedShots-=copy;

            }

            if(isSelected()) {

                currentLocation().getWorld().playSound(currentLocation(), Sound.ITEM_ELYTRA_FLYING, 0.1f, 1f);

            }

            hitboxStand.yaw = (getPlayer().getLocation().getYaw());
            lastBoostTicks++;
            remainingTicks--;
            boolean last = (remainingTicks < 1);
            if (model.isActive()) {

                if (!isSelected()) {

                    if(getPlayer() instanceof SplatoonHumanPlayer) {

                        seat.removePassenger(getPlayer().getBukkitEntity());

                    }

                    model.remove();
                    getParticleSources().clear();

                    seat.remove();
                    NMSUtil.broadcastEntityRemovalToSquids(seat);

                    seat = null;

                } else {

                    Location location = currentLocation().clone();
                    EntityArmorStand stand = ((CraftArmorStand)seat).getHandle();
                    stand.setPosition(location.getX(), location.getY() - 1.6, location.getZ());
                    stand.yaw = getPlayer().getLocation().getYaw();

                    for(Point point : getParticleSources()) {

                        LocalCoordinateSystem lcs = new LocalCoordinateSystem();
                        lcs.calculate(0, stand.yaw, 0);
                        Location startLocation = currentLocation().clone();

                        startLocation.add(lcs.getForward().clone().multiply(point.getX()));
                        startLocation.add(lcs.getUpwards().clone().multiply(point.getY()));
                        startLocation.add(lcs.getSideways().clone().multiply(point.getZ()));
                        startLocation.setYaw((float) ((double) ((float) currentLocation().getYaw()) + point.y_rotation));
                        startLocation = startLocation.add(0, 0, 0);

                        SplatoonServer.broadcastColorizedBreakParticle(getPlayer().getWorld(),
                                startLocation.getX(), startLocation.getY(), startLocation.getZ(), 0f, -3f, 0f, getPlayer().getTeam().getColor());

                        for(int y = 0; y < 8; y++) {

                            double newY = startLocation.getY() - y;
                            Location loc = startLocation.clone();
                            loc.setY(newY);

                            if(getPlayer().getMatch().isPaintable(getPlayer().getTeam(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ())) {

                                getPlayer().getMatch().paint(getPlayer(), new Vector(loc.getX(), loc.getY(), loc.getZ()), getPlayer().getTeam());
                                break;

                            }

                        }

                    }

                    for(Player player : Bukkit.getOnlinePlayers()) {

                        if(player.getWorld().equals(getPlayer().getWorld())) {

                            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(stand));

                        }

                    }

                }

            } else {

                if (isSelected()) {

                    model.spawn();
                    seat = (ArmorStand) currentLocation().getWorld().spawnEntity(currentLocation(), EntityType.ARMOR_STAND);
                    seat.setVisible(false);
                    seat.setCanTick(false);
                    seat.setCanMove(false);


                    if(getPlayer() instanceof SplatoonHumanPlayer) {

                        seat.addPassenger(getPlayer().getBukkitEntity());

                    } else {

                        for(Player player : Bukkit.getOnlinePlayers()) {

                            if(player.getWorld().equals(getPlayer().getLocation().getWorld())) {

                                if(player.getLocation().distance(getPlayer().getLocation()) <= 64) {

                                    PacketContainer container = new PacketContainer(PacketType.Play.Server.MOUNT);
                                    container.getIntegers().write(0, seat.getEntityId());
                                    container.getIntegerArrays().write(0, new int[]{getPlayer().getEntityID()});
                                    try {

                                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);

                                    } catch (Exception e) {

                                        e.printStackTrace();

                                    }

                                }

                            }

                        }

                    }

                } else {

                    /*hitboxStand.locX = getPlayer().getLocation().getX();
                    hitboxStand.locY = getPlayer().getLocation().getY();
                    hitboxStand.locZ = getPlayer().getLocation().getZ();
                    hitboxStand.setPosition(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ());*/

                }

            }

            double distanceToGround = distanceToGround();
            if (distanceToGround > 5) {

                if (velocityY > -0.2) {

                    velocityY -= 0.03;

                }

            } else {

                if (velocityY < 0.1) {

                    velocityY += 0.012;

                }

            }

            hitboxStand.move(EnumMoveType.SELF, 0, velocityY, 0);

            model.moveToPosition(currentLocation().getX(), currentLocation().getY(), currentLocation().getZ());
            model.rotateTowards(getPlayer().yaw());

            if(last) {

                eject();

            }

        }

    }

    private EntityArmorStand hitboxStand = null;

    public void handleInput(double x, double z) {

        hitboxStand.move(EnumMoveType.SELF, x*0.15, 0, z * 0.15);

    }

    public void eject() {

        remainingTicks = 0;
        if(model.isActive()) {

            seat.removePassenger(getPlayer().getBukkitEntity());
            seat.remove();
            NMSUtil.broadcastEntityRemovalToSquids(seat);

            model.remove();
            getParticleSources().clear();

        }
        if(!getPlayer().isSplatted()) {

            if (!getPlayer().inSuperJump()) {

                getPlayer().superJump(startLocation, 0);

            }

        }

        if(getPlayer() instanceof EntityNPC) {

            ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd();

        }

    }

    private int lastShootTicks = 0;
    private int queuedShots = 0;

    @Override
    public void asyncTick() {

        if(getPlayer().isShooting()) {

            if(isActive()) {

                if(isSelected() && lastShootTicks > 30) {

                    lastShootTicks = 0;
                    queuedShots++;

                }

            } else {

                if (isSelected() && getPlayer().hasControl()) {

                    if (getPlayer().isSpecialReady()) {

                        activateCall();

                    } else {

                        getPlayer().specialNotReady();

                    }

                }

            }

        }

    }

    public void activateCall() {

        lastBoostTicks = 0;
        lastShootTicks = 0;
        remainingTicks = 210;
        startLocation = getPlayer().getLocation();
        velocityY = 0.15;

        hitboxStand = new EntityArmorStand(getPlayer().getMatch().nmsWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ());
        hitboxStand.setNoGravity(true);
        hitboxStand.setPosition(startLocation.getX(), startLocation.getY(), startLocation.getZ());
        getPlayer().resetSpecialGauge();
        ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponBegin();
        getPlayer().addInk(100d);
        getPlayer().getMatch().broadcast(" " + getPlayer().coloredName() + " §7aktiviert den §eTintendüser§7!");

    }

    public void cleanUp() {

        if(!seat.isDead()) {

            seat.removePassenger(getPlayer().getBukkitEntity());
            seat.remove();
            NMSUtil.broadcastEntityRemovalToSquids(seat);

        }

        if(model != null) {

            model.removeForcefully();

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
        return Material.STONE_HOE;
    }

    @Override
    public void shoot() {

    }

    private int lastBoostTicks = 0;
    public void boost() {

        if(lastBoostTicks > 45) {

            lastBoostTicks = 0;
            velocityY = 0.2;

        }

    }

    @Override
    public void activate() {

        activateCall();

    }

    public Location getCurrentLocation() {

        return currentLocation();

    }

}
