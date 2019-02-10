package de.xenyria.splatoon.game.equipment.weapon.util;

public class ProgressBarUtil {

    public static String generateProgressBar(double percentage, int width, String color1, String color2) {

        String bar = color1;
        String uncolored = color2;

        for(int i = 0; i < width; i++) {

            double curPercentage = ((double)i / (double)width) * 100d;
            if(curPercentage < percentage) {

                bar+="█";

            } else {

                uncolored+="█";

            }

        }

        return bar+uncolored;

    }

}
