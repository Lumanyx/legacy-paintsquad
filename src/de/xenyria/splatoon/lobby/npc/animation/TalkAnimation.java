package de.xenyria.splatoon.lobby.npc.animation;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.util.RandomUtil;
import de.xenyria.splatoon.lobby.npc.RecentPlayerNPC;
import org.bukkit.Bukkit;

import java.util.Random;

public class TalkAnimation extends RecentPlayerAnimation {


    private int lastActionTicks;

    @Override
    public void tick() {

        lastActionTicks++;
        if(lastActionTicks > 30) {

            boolean doAction = RandomUtil.random(5);
            if(doAction) {

                lastActionTicks = 0;
                if(new Random().nextBoolean()) {

                    getNPC().broadcastAnimation((byte)0);

                } else {

                    if(new Random().nextBoolean()) {

                        scheduleLook(0f, 2);
                        scheduleLook(3f, 4);
                        scheduleLook(6f, 6);
                        scheduleLook(9f, 8);
                        scheduleLook(12f, 10);
                        scheduleLook(15f, 12);
                        scheduleLook(15f, 22);
                        scheduleLook(12f, 24);
                        scheduleLook(9f, 26);
                        scheduleLook(6f, 28);
                        scheduleLook(3f, 29);
                        scheduleLook(0, 30);

                    } else {

                        scheduleLook(0f, 2);
                        scheduleLook(-3f, 4);
                        scheduleLook(-6f, 6);
                        scheduleLook(-9f, 8);
                        scheduleLook(-12f, 10);
                        scheduleLook(-15f, 12);
                        scheduleLook(-15f, 22);
                        scheduleLook(-12f, 24);
                        scheduleLook(-9f, 26);
                        scheduleLook(-6f, 28);
                        scheduleLook(-3f, 29);
                        scheduleLook(0, 30);

                    }

                }

            }

        }

    }

    public void scheduleLook(float pitch, int ticks) {

        Bukkit.getScheduler().runTaskLater(XenyriaSplatoon.getPlugin(), () -> {

            float yaw = getNPC().getLocation().getYaw();
            getNPC().updateFacing(yaw, pitch);

        }, ticks);

    }

}
