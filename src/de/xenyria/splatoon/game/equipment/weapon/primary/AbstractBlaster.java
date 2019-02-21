package de.xenyria.splatoon.game.equipment.weapon.primary;

import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.projectile.BlastProjectile;
import de.xenyria.splatoon.game.resourcepack.ResourcePackItemOption;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.PacketPlayOutAbilities;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class AbstractBlaster extends SplatoonPrimaryWeapon {

    private float blastRadius;
    private double impactDamage;
    private long timePerShoot;
    private float usage;
    private long lastShoot;
    private float range;

    public AbstractBlaster(int id, String name, float blastRadius, double impactDamage, long timePerShoot, float usage, float range) {

        super(id, name);
        this.blastRadius = blastRadius;
        this.impactDamage = impactDamage;
        this.timePerShoot = timePerShoot;
        this.usage = usage;
        this.range = range;

    }

    public void cleanUp() {

        queuedProjectiles = 0;

    }

    @Override
    public Material getRepresentiveMaterial() {
        return Material.IRON_HOE;
    }

    @Override
    public PrimaryWeaponType getPrimaryWeaponType() {
        return PrimaryWeaponType.BLASTER;
    }

    private int queuedProjectiles = 0;

    public void syncTick() {

        if((System.currentTimeMillis() - lastInteract) < 300 && queuedProjectiles > 0) {

            if(getPlayer() instanceof SplatoonHumanPlayer) {

                Player player1 = ((SplatoonHumanPlayer)getPlayer()).getPlayer();
                if(!player1.hasPotionEffect(PotionEffectType.SLOW)) {

                    player1.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((timePerShoot / 50) + 10), 3));
                    EntityPlayer player = ((CraftPlayer)player1).getHandle();
                    player.playerConnection.sendPacket(new PacketPlayOutAbilities(player.abilities));

                }

            }


        }

        if(isSelected()) {

            int copy = queuedProjectiles;
            for (int i = 0; i < copy; i++) {

                BlastProjectile projectile = new BlastProjectile(getPlayer(), this, getPlayer().getMatch(), blastRadius, range, (float) impactDamage);
                projectile.spawn(0.365d);
                getPlayer().getMatch().queueProjectile(projectile);

                if(getPlayer() instanceof SplatoonHumanPlayer) {

                    Player player1 = ((SplatoonHumanPlayer)getPlayer()).getPlayer();
                    if(!player1.hasPotionEffect(PotionEffectType.SLOW)) {

                        player1.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((timePerShoot / 50) + 10), 3));
                        EntityPlayer player = ((CraftPlayer)player1).getHandle();
                        player.playerConnection.sendPacket(new PacketPlayOutAbilities(player.abilities));

                    }

                }

            }
            queuedProjectiles -= copy;

        }

    }

    private long lastInteract;

    public void asyncTick() {

        lastInteract = System.currentTimeMillis();
        if(getPlayer().isShooting() && isSelected()) {

            long lastShot = System.currentTimeMillis() - lastShoot;
            if(getPlayer().hasEnoughInk(usage) && lastShot > timePerShoot) {

                getPlayer().removeInk(usage);
                lastShoot = System.currentTimeMillis();
                queuedProjectiles++;

            }

        }
    }

}
