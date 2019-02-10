package de.xenyria.splatoon.game.util;

import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

public class NMSUtil {

    public static BoundingBoxFilter dragFilter = new BoundingBoxFilter() {
        @Override
        public IBlockData processDataAtLocation(IBlockData iBlockData, int i, int i1, int i2) {
            return Blocks.AIR.getBlockData();
        }
    };

    public static BoundingBoxFilter filter = new BoundingBoxFilter() {
        @Override
        public IBlockData processDataAtLocation(IBlockData iBlockData, int i, int i1, int i2) {

            if(AABBUtil.isPassable(CraftBlockData.createData(iBlockData).getMaterial())) {

                return Blocks.AIR.getBlockData();

            }
            return iBlockData;

        }
    };

    public static void broadcastEntityRemovalToSquids(Entity entity) {

        for(SplatoonHumanPlayer player : SplatoonHumanPlayer.getHumanPlayers()) {

            if(player.isSquid() && player.getLocation().distance(entity.getBukkitEntity().getLocation()) < 96) {

                ((CraftPlayer)player.getPlayer()).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(entity.getId()));

            }

        }

    }

    public static void broadcastEntityRemovalToSquids(org.bukkit.entity.Entity craftEntity) {

        broadcastEntityRemovalToSquids(((CraftEntity)craftEntity).getHandle());

    }

}
