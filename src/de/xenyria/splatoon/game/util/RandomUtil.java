package de.xenyria.splatoon.game.util;

import java.util.Random;

public class RandomUtil {

    public static boolean random(int percentage) {

        float result = new Random().nextFloat() * 100f;
        return result > (100-percentage);

    }

}
