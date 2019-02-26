package de.xenyria.splatoon.game.objects;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.combat.HitableEntity;
import de.xenyria.splatoon.game.equipment.weapon.util.PlayerDamageCooldownMap;
import de.xenyria.splatoon.game.match.Match;
import de.xenyria.splatoon.game.match.blocks.BlockFlagManager;
import de.xenyria.splatoon.game.projectile.*;
import de.xenyria.splatoon.game.team.Team;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class Sponge extends GameObject implements HitableEntity {

    private double size;
    private Block baseBlock;

    private Team owningTeam;
    private double health;

    private PlayerDamageCooldownMap map = new PlayerDamageCooldownMap();

    public Sponge(Match match, Block baseBlock) {

        super(match);
        this.baseBlock = baseBlock;
        baseBlock.setType(Material.SPONGE);

    }

    private final double MAX_HP = 200d;
    private final int MAX_SIZE = 2;

    private int size() {

        double hpFactor = (health / (float)MAX_HP);
        return (int) Math.floor((float)MAX_SIZE * hpFactor);

    }

    private HashMap<BlockPosition, IBlockData> backup = new HashMap<>();
    private int lastDamageTicker = 0;

    public void pushNearbyPlayersOut() {

        AxisAlignedBB bb = aabb();
        for(GameObject entity : getMatch().getGameObjects()) {

            if(entity instanceof HitableEntity && entity != this && !(entity instanceof Sponge)) {

                HitableEntity entity1 = (HitableEntity)entity;
                AxisAlignedBB bb1 = entity1.aabb();
                if(bb.c(bb1)) {

                    Vector target = baseBlock.getLocation().toVector().add(new Vector(.5, 0, .5));
                    Vector begin = entity1.getLocation().toVector();
                    Vector dir = target.clone().subtract(begin);

                    InstantDamageKnockbackProjectile projectile = new InstantDamageKnockbackProjectile(baseBlock.getLocation(),
                            null, null, dir.clone().multiply(1.3), 20d, getMatch());

                    projectile.setReason(DamageReason.HUMAN_ERROR);
                    projectile.setTeam(owningTeam);
                    if(((HitableEntity) entity).isHit(projectile)) { ((HitableEntity) entity).onProjectileHit(projectile); }

                }

            }

        }

    }

    public void rebuild() {

        if(!backup.isEmpty()) {

            WorldServer server = ((CraftWorld) baseBlock.getWorld()).getHandle();
            for (Map.Entry<BlockPosition, IBlockData> entry : backup.entrySet()) {

                server.setTypeUpdate(entry.getKey(), entry.getValue());
                Block block = getMatch().getWorld().getBlockAt(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());
                BlockFlagManager.BlockFlag flag = getMatch().getBlockFlagManager().getBlock(getMatch().getOffset(), block.getX(), block.getY(), block.getZ());
                flag.setTeamID(owningTeam.getID());

            }
            backup.clear();

        }

        World world = baseBlock.getWorld();
        WorldServer server = ((CraftWorld) baseBlock.getWorld()).getHandle();

        int size = size();
        int minX = Math.min(baseBlock.getX() - size, baseBlock.getX() + size);
        int minY = Math.min(baseBlock.getY() - size, baseBlock.getY() + size);
        int minZ = Math.min(baseBlock.getZ() - size, baseBlock.getZ() + size);
        int maxX = Math.max(baseBlock.getX() - size, baseBlock.getX() + size);
        int maxY = Math.max(baseBlock.getY() - size, baseBlock.getY() + size);
        int maxZ = Math.max(baseBlock.getZ() - size, baseBlock.getZ() + size);

        pushNearbyPlayersOut();

        // TODO Beacon
        for(int x = minX; x <= maxX; x++) {

            for(int y = minY; y <= maxY; y++) {

                for(int z = minZ; z <= maxZ; z++) {

                    Block block = world.getBlockAt(x,y,z);
                    BlockFlagManager.BlockFlag flag = getMatch().getBlockFlagManager().getBlock(getMatch().getOffset(), x,y,z);
                    if(flag.hasSetTeam()) {

                        flag.setTeamID(BlockFlagManager.BlockFlag.NO_TEAM);

                    }

                    BlockPosition position = new BlockPosition(x,y,z);
                    IBlockData data = server.getTypeIfLoaded(position);
                    if(data == null) { data = Blocks.AIR.getBlockData(); }
                    backup.put(position, data);

                    if(owningTeam != null) {

                        BlockFlagManager.BlockFlag flag1 = getMatch().getBlockFlagManager().getBlock(getMatch().getOffset(), block.getX(), block.getY(), block.getZ());
                        flag1.setTeamID(owningTeam.getID());
                        block.setType(owningTeam.getColor().getSponge());

                    } else {

                        block.setType(Material.SPONGE);

                    }

                }

            }

        }

    }

    @Override
    public void onProjectileHit(SplatoonProjectile projectile) {

        if(projectile instanceof DamageDealingProjectile) {

            Team team = projectile.getTeam();
            if(projectile.getShooter() != null) {

                if(map.lastDamage(projectile.getShooter()) > 50) {

                    map.registerDamage(projectile.getShooter());

                } else {

                    return;

                }

            }

            if(health >= MAX_SIZE) {

                if (owningTeam != team) {

                    projectile.remove();

                }

            } else {

                projectile.remove();

            }

            if (team != null) {

                if(lastDamageTicker < 1) {

                    int prevSize = size();
                    boolean add = false;
                    if (owningTeam == null) {

                        owningTeam = team;
                        add = true;

                    } else {

                        add = (owningTeam == projectile.getTeam());

                    }
                    if (add) {

                        health += ((DamageDealingProjectile) projectile).getDamage();
                        if (health > MAX_HP) {

                            health = MAX_HP;

                        }

                    } else {

                        health -= ((DamageDealingProjectile) projectile).getDamage() * .25;
                        if (health < 0) {

                            health = 0;

                        }

                    }
                    rebuild();

                }

            }

        }

    }

    @Override
    public boolean isHit(SplatoonProjectile projectile) {

        if(projectile instanceof RayProjectile || projectile instanceof InstantDamageKnockbackProjectile) {

            return true;

        }

        return projectile.getTeam() != null && projectile.aabb().c(aabb());

    }

    @Override
    public double distance(SplatoonProjectile projectile) {
        return getLocation().distance(projectile.getLocation());
    }

    @Override
    public int getEntityID() {
        return -1;
    }

    @Override
    public Location getLocation() {
        return baseBlock.getLocation().clone().add(.5, .5, .5);
    }

    @Override
    public double height() {
        return size();
    }

    @Override
    public AxisAlignedBB aabb() {

        Vector basePos = baseBlock.getLocation().toVector().clone();
        basePos = basePos.add(new Vector(0.5, 0.5, 0.5));
        double size = size() + .75f;

        return new AxisAlignedBB(basePos.getX() - size, basePos.getY() - size, basePos.getZ() - size, basePos.getX() + size, basePos.getY() + size, basePos.getZ() + size);

    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.SPONGE;
    }

    private int pushTicker = 0;

    @Override
    public void onTick() {

        if(lastDamageTicker > 0) {

            lastDamageTicker--;

        }

        pushTicker++;
        if(pushTicker > 3) {

            pushTicker = 0;
            pushNearbyPlayersOut();

        }

    }

    @Override
    public void reset() {

        health = 0d;
        owningTeam = null;
        rebuild();

    }

    @Override
    public void onRemove() {

        reset();
        baseBlock.setType(Material.AIR);

    }
}
