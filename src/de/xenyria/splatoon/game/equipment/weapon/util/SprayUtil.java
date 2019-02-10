package de.xenyria.splatoon.game.equipment.weapon.util;

import java.util.Random;

public class SprayUtil {

    public static float calculateSpray(float maxSpray) {

        if(new Random().nextBoolean()) {

            return new Random().nextFloat() * maxSpray;

        } else {

            return new Random().nextFloat() * -maxSpray;

        }

    }

    public static float addSpray(float baseValue, float maxSpray) {

        if(new Random().nextBoolean()) {

            return baseValue + (maxSpray * new Random().nextFloat());

        } else {

            return baseValue - (maxSpray * new Random().nextFloat());

        }

    }

}
