package de.xenyria.splatoon.lobby.npc.animation;

import de.xenyria.servercore.spigot.util.DirectionUtil;
import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.util.VectorUtil;
import de.xenyria.splatoon.lobby.npc.RecentPlayerNPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Random;

public class WalkAnimation extends RecentPlayerAnimation {

    private Location[] locations = null;

    public WalkAnimation(Location... locations) {

        this.locations = locations;

    }

    private boolean init = false;

    private Location startLocation = null;
    private Location targetLocation = null;
    private Location nextFreeLocation() {

        lastIndex++;
        if(lastIndex > (locations.length - 1)) {

            lastIndex = 0;

        }
        return locations[lastIndex];

    }

    private int lastIndex = 0;
    private int ticksToWalk = 0;
    private boolean flag = false;

    @Override
    public void tick() {

        if(!init) {

            startLocation = getNPC().getLocation();
            targetLocation = locations[0];
            init = true;

        }

        if(targetLocation != null) {

            Vector direction = targetLocation.toVector().subtract(getNPC().getLocation().toVector()).normalize();
            if(VectorUtil.isValid(direction)) {

                float[] direction1 = DirectionUtil.directionToYawPitch(direction);
                getNPC().updateFacing(direction1[0], direction1[1]);
                Vector movement = direction.clone().multiply(0.08);
                getNPC().move(movement.getX(), movement.getY(), movement.getZ());

                if(getNPC().getLocation().distance(targetLocation) <= 0.2d) {

                    targetLocation = null;
                    startLocation = null;
                    flag = true;

                }

            } else {

                targetLocation = null;
                flag = true;

            }

        } else {

            if(flag) {

                int ticksTo = 0;
                float pitch = -(10 + new Random().nextInt(11));

                float yawBefore = getNPC().getLocation().getYaw();

                boolean negative = new Random().nextBoolean();

                for (float yawOffset = 0; yawOffset < (40 + new Random().nextInt(64)); yawOffset += 6) {

                    ticksTo += 2;
                    float targetYaw = yawBefore;

                    if (!negative) {

                        targetYaw+=yawOffset;

                    } else {

                        targetYaw-=yawOffset;

                    }
                    final float y1 = targetYaw;
                    Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

                        getNPC().updateFacing(y1, pitch);

                    }, ticksTo);


                }
                ticksTo += 4;
                ticksToWalk = ticksTo;
                flag = false;

            }


            ticksToWalk--;
            if(ticksToWalk < 1) {

                startLocation = getNPC().getLocation().clone();
                targetLocation = nextFreeLocation();

            }

        }

    }
}
