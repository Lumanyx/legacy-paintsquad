package de.xenyria.splatoon.game.equipment.weapon.special.baller;

import de.xenyria.splatoon.SplatoonServer;
import de.xenyria.splatoon.ai.entity.EntityNPC;
import de.xenyria.splatoon.game.equipment.weapon.ai.AISpecialWeapon;
import de.xenyria.splatoon.game.equipment.weapon.viewmodel.BallerModel;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.objects.BallerHitbox;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import de.xenyria.splatoon.game.projectile.BombProjectile;
import de.xenyria.splatoon.game.projectile.SplatoonProjectile;
import de.xenyria.splatoon.game.util.AABBUtil;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;

public class Baller extends SplatoonSpecialWeapon implements AISpecialWeapon {

    public static final int ID = 13;

    public Baller() {
        super(ID, "Sepisphäre", "", 190);
    }

    private int ticksToExplosion;
    private boolean hasExploded;

    @Override
    public boolean isActive() {
        return !hasExploded && ticksToExplosion > 0;
    }

    @Override
    public void onProjectileSpawn(SplatoonProjectile projectile, SplatoonPlayer player) {

    }

    public void assign(SplatoonPlayer player) {

        super.assign(player);
        model = new BallerModel(player, player.getLocation(), this);

    }

    private EntityArmorStand cameraStand;
    public EntityPlayer mimic;
    private BallerModel model;
    private int fallingTicks = 0;
    private boolean movedInTick = false;
    private int lastUseTicker = 0;
    private int lastMoveTicker = 0;

    public void handleInput(double x, double z, boolean space) {

        lastMoveTicker++;
        if(lastMoveTicker > 4 && (x!=0||z!=0)) {

            lastMoveTicker = 0;
            mimic.getBukkitEntity().getLocation().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 1f, 2f);

        }

        velocity.multiply(0.7);
        if(velocity.length() < 0.0125) { velocity = new Vector(); }

        movedInTick = true;
        Vector movementVector = new Vector(x, 0, z);
        movementVector = movementVector.multiply(0.235);

        float downwardMovement = -0.01f;
        if(movementVector.length() > 0.01) {

            Vector vector = new Vector(mimic.locX, mimic.locY, mimic.locZ);
            vector = vector.add(movementVector);
            SplatoonServer.broadcastColorizedBreakParticle(getPlayer().getWorld(), mimic.locX, mimic.locY, mimic.locZ, getPlayer().getTeam().getColor());

            AxisAlignedBB aabb = new AxisAlignedBB(vector.getX() - .3, vector.getY(), vector.getZ() - .3, vector.getX() + .3, vector.getY() + 1.8, vector.getZ() + .3);
            if(!AABBUtil.hasSpace(getPlayer().getWorld(), aabb)) {

                Vector vec = AABBUtil.resolveWrap(getPlayer().getWorld(), vector, aabb);
                if(vec != null) {

                    movementVector = vec.clone().subtract(new Vector(mimic.locX, mimic.locY, mimic.locZ));

                } else {

                    if(velocity.getY() < .12) { velocity.add(new Vector(0, 0.02, 0)); }
                    movementVector = new Vector(velocity.getX(), velocity.getY(), velocity.getZ());
                    //mimic.motY = movementVector.getY();
                    mimic.onGround = true;
                    fallingTicks = 0;

                }

            }

            mimic.move(EnumMoveType.SELF, movementVector.getX() + velocity.getX(),
                        movementVector.getY() + downwardMovement + velocity.getY(),
                        movementVector.getZ() + velocity.getZ());
            mimic.yaw = getPlayer().getLocation().getYaw();
            mimic.setHeadRotation(mimic.yaw);

        } else {

            mimic.move(EnumMoveType.SELF, movementVector.getX() + velocity.getX(), velocity.getY() + downwardMovement,  movementVector.getZ() + velocity.getZ());
            mimic.yaw = getPlayer().getLocation().getYaw();
            mimic.setHeadRotation(mimic.yaw);

        }

        if(mimic.onGround) {

            if(space) {

                velocity.add(new Vector(0, 0.82, 0));
                mimic.velocityChanged = true;

            }
            fallingTicks = 0;

        } else {

            fallingTicks++;
            if(velocity.getY() > -1.2) {

                velocity.add(new Vector(0, -0.09, 0));

            }

        }

        if(velocity.getY() < -1.2) {

            velocity.setY(-1.2);
            mimic.velocityChanged = true;

        }

        for(Player player : Bukkit.getOnlinePlayers()) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(mimic));
            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(mimic, (byte)(mimic.yaw * (0.70333333))));

        }

        World world = getPlayer().getWorld();
        getPlayer().getMatch().colorSquare(world.getBlockAt((int)mimic.locX, (int)mimic.locY, (int)mimic.locZ), getPlayer().getTeam(), getPlayer(), 2);

        model.moveToPosition(mimic.locX, mimic.locY, mimic.locZ);
        model.rotateTowards(mimic.yaw);

    }

    private int explodeHoldTicker = 0;
    private BallerHitbox hitbox;

    public void explode() {

        BombProjectile projectile = new BombProjectile(getPlayer(), this, getPlayer().getMatch(), 5f, 0, 150, false);
        projectile.spawn(0, mimic.getBukkitEntity().getLocation());
        getPlayer().getMatch().queueProjectile(projectile);
        getPlayer().getLocation().getWorld().playSound(getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
        hasExploded = true;
        explodeHoldTicker = 0;

    }

    public void cleanUp() {

        if(model != null) {

            model.removeForcefully();

        }

    }

    public void end() {

        lastUseTicker=30;
        ticksToExplosion = 0;
        hasExploded = true;
        explodeHoldTicker = 0;
        getPlayer().addInk(100d);
        if(hitbox != null) {

            getPlayer().getMatch().queueObjectRemoval(hitbox);

        }

        if(model.isActive()) {

            model.remove();

        }
        getPlayer().getMatch().queueObjectRemoval(hitbox);

        if((getPlayer() instanceof SplatoonHumanPlayer)) {

            ((SplatoonHumanPlayer) getPlayer()).getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);

        }

        for(Player player : getPlayer().getWorld().getPlayers()) {

            ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(mimic.getId()));
            if(getPlayer() instanceof SplatoonHumanPlayer && player != ((SplatoonHumanPlayer) getPlayer()).getPlayer()) {

                EntityPlayer player1 = ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle();
                EntityPlayer receiver = ((CraftPlayer)player).getHandle();
                receiver.playerConnection.sendPacket(new PacketPlayOutEntityDestroy(mimic.getId()));
                receiver.playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle()));
                receiver.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(player1.getId(), player1.getDataWatcher(), player1.onGround));
                receiver.playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(player1, (byte)(player1.yaw * 0.703)));
                for(EnumItemSlot slot : EnumItemSlot.values()) {

                    receiver.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(player1.getId(), slot, player1.getEquipment(slot)));

                }

            }

        }

        if((getPlayer() instanceof SplatoonHumanPlayer)) {

            EntityPlayer player1 = ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle();
            player1.playerConnection.sendPacket(new PacketPlayOutEntityDestroy(cameraStand.getId()));
            getPlayer().teleport(new Location(getPlayer().getWorld(),
                    mimic.locX, mimic.locY, mimic.locZ, mimic.yaw, getPlayer().getLocation().getPitch()));

        }
        mimic = null;
        cameraStand = null;

        getPlayer().unlockSquidForm();

        if(getPlayer() instanceof EntityNPC) {

            ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponEnd();

        }

    }

    private int maxBallerTicks = 230;
    private Vector velocity = new Vector(0,0,0);

    @Override
    public void syncTick() {

        if(lastUseTicker > 0) { lastUseTicker--; }

        if(isActive()) {

            if(model.isActive()) { model.tick(); }

            if(getPlayer() instanceof EntityNPC) {

                if(mimic != null) {

                    EntityNPC npc = (EntityNPC) getPlayer();
                    npc.getNMSEntity().onGround = true;
                    //handleInput(1, 0, false);
                    npc.getNMSEntity().setPositionRotation(mimic.locX, mimic.locY, mimic.locZ, mimic.yaw, 0);
                    npc.broadcastPositionUpdate();

                }

            }

            if(!getPlayer().isSplatted()) {

                if (!initialized) {

                    if((getPlayer() instanceof SplatoonHumanPlayer)) {

                        ((SplatoonHumanPlayer) getPlayer()).getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 2, false, false));

                        for (Player player : Bukkit.getOnlinePlayers()) {

                            boolean isSelf = false;
                            if(getPlayer() instanceof SplatoonHumanPlayer) {

                                isSelf = ((SplatoonHumanPlayer)getPlayer()).getPlayer().equals(player);

                            }

                            if (player.getLocation().distance(getPlayer().getLocation()) < 96 && !isSelf) {

                                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(
                                        getPlayer().getNMSPlayer().getId()
                                ));

                            }

                        }

                    }


                    EntityPlayer player = getPlayer().getNMSPlayer();
                    WorldServer server = (WorldServer) getPlayer().getNMSPlayer().world;
                    mimic = new EntityPlayer(server.getMinecraftServer(), server, player.getProfile(), new PlayerInteractManager(server));
                    //mimic.setSneaking(true);
                    mimic.locX = getPlayer().getLocation().getX();
                    mimic.locY = getPlayer().getLocation().getY();
                    mimic.locZ = getPlayer().getLocation().getZ();
                    mimic.setPosition(getPlayer().getLocation().getX(),
                            getPlayer().getLocation().getY(),
                            getPlayer().getLocation().getZ());
                    mimic.locX = getPlayer().getLocation().getX();
                    mimic.locY = getPlayer().getLocation().getY();
                    mimic.locZ = getPlayer().getLocation().getZ();

                    if(getPlayer() instanceof SplatoonHumanPlayer) {

                        cameraStand = new EntityArmorStand(server, player.locX, player.locY, player.locZ);
                        cameraStand.setPosition(player.locX, player.locY, player.locZ);
                        cameraStand.getBukkitEntity().addPassenger(getPlayer().getBukkitEntity());
                        cameraStand.setInvisible(true);
                        for (Player player1 : Bukkit.getOnlinePlayers()) {

                            if (player1.getLocation().distance(getPlayer().getLocation()) < 96) {

                                ((CraftPlayer) player1).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(mimic));
                                ((CraftPlayer) player1).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(mimic.getId(), mimic.getDataWatcher(), mimic.onGround));

                            }

                        }
                        player.playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(cameraStand));
                        player.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(cameraStand.getId(), cameraStand.getDataWatcher(), false));
                        player.playerConnection.sendPacket(new PacketPlayOutMount(cameraStand));

                    }

                    initialized = true;

                    hitbox = new BallerHitbox(getPlayer().getMatch(), getPlayer(), this);
                    getPlayer().getMatch().addGameObject(hitbox);

                }

                boolean end = false;

                if (!model.isActive()) {

                    model.spawn();

                }

                if (!getPlayer().isShooting()) {

                    explodeHoldTicker = 0;
                    float remSecs = ((float) ticksToExplosion / 20f);
                    DecimalFormat format = new DecimalFormat("#.#");
                    String seconds = format.format(remSecs);
                    getPlayer().sendActionBar("§e" + seconds + " Sekunde(n) §7bis zur automatischen Detonation.");

                } else {

                    ticksToExplosion-=7;
                    if(ticksToExplosion < 1) { ticksToExplosion = 0; end = true; }
                    getPlayer().sendActionBar("§eExplosion steht bevor!");

                }

                if (getPlayer().isSquid()) {

                    getPlayer().leaveSquidForm();

                }

                getPlayer().lockSquidForm();
                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    ((SplatoonHumanPlayer) getPlayer()).getPlayer().getInventory().setHeldItemSlot(3);

                }

                if (movedInTick) {

                    movedInTick = false;

                } else {

                    if (initialized) {

                        handleInput(0, 0, false);

                    }

                }

                if (ticksToExplosion > 0) {

                    ticksToExplosion--;

                    if (ticksToExplosion < 1) {
                        end = true;
                    }

                }

                if (end) {

                    explode();
                    end();
                    return;

                }

                mimic.yaw = getPlayer().getLocation().getYaw();
                mimic.pitch = 0f;
                //mimic.setSneaking(true);

                if(ticksToExplosion < 40) {

                    float pitch = 1f-((float)ticksToExplosion/40f);
                    warnSoundTicks++;
                    if(warnSoundTicks > 4) {

                        warnSoundTicks = 0;
                        location().getWorld().playSound(location(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);

                    }

                }

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    Vector cameraStartPos = mimic.getBukkitEntity().getLocation().toVector();
                    Vector cameraEndPos = cameraStartPos.clone();
                    Location location = getPlayer().getLocation().clone();
                    location.setYaw(getPlayer().getLocation().getYaw() - 180f);
                    location.setPitch(-25f);
                    for (double d = 0; d < 5; d += 0.5) {

                        cameraEndPos = cameraEndPos.add(location.getDirection().clone().multiply(0.5));
                        Block block = getPlayer().getWorld().getBlockAt((int) cameraEndPos.getX(), (int) cameraEndPos.getY(), (int) cameraEndPos.getZ());
                        if (!block.isEmpty() && !block.isPassable()) {

                            cameraEndPos = cameraEndPos.subtract(location.getDirection().clone().multiply(d));
                            break;

                        }

                    }

                    cameraStand.locX = cameraEndPos.getX();
                    cameraStand.locY = cameraEndPos.getY();
                    cameraStand.locZ = cameraEndPos.getZ();

                    ((CraftPlayer) ((SplatoonHumanPlayer) getPlayer()).getPlayer()).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(cameraStand));

                }

            } else {

                end();

            }

        } else {

            if(model.isActive()) { model.remove(); }

        }

    }

    private int warnSoundTicks = 0;
    private boolean initialized = false;

    public float usedTimePercentage() {

        float val = 1-((float)ticksToExplosion / (float)(maxBallerTicks / 10));
        if(val > 1) { return 1; } else { return val; }

    }

    @Override
    public void asyncTick() {

        if(!isActive() && getPlayer().hasControl() && lastUseTicker < 1 && isSelected() && getPlayer().isShooting()) {

            if(getPlayer().getSpecialPoints() >= getRequiredPoints()) {

                activateCall();

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

        return Material.SNOWBALL;

    }

    @Override
    public void shoot() {

    }

    private Location lastPos = null;
    public Location location() {

        if(lastPos == null) { lastPos = getPlayer().getLocation(); }

        if(mimic == null) {

            return lastPos;

        } else {

            lastPos = new Location(getPlayer().getLocation().getWorld(), mimic.locX, mimic.locY, mimic.locZ);
            return lastPos;

        }

    }

    public Vector getVelocity() { return velocity; }

    public BallerHitbox getHitbox() {
        return hitbox;
    }

    public void setHitbox(BallerHitbox hitbox) {
        this.hitbox = hitbox;
    }

    public void activateCall() {

        initialized = false;
        hasExploded = false;
        getPlayer().resetSpecialGauge();
        ticksToExplosion = maxBallerTicks;

    }

    @Override
    public void activate() {

        activateCall();
        ((EntityNPC)getPlayer()).getTaskController().getSpecialWeaponManager().onSpecialWeaponBegin();

    }
}
