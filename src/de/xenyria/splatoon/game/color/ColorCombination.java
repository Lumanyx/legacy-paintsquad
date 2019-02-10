package de.xenyria.splatoon.game.color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

public class ColorCombination {

    private static ArrayList<ColorCombination> combinations = new ArrayList<>();
    static {

        combinations.add(new ColorCombination(Color.ORANGE, Color.BLUE));
        combinations.add(new ColorCombination(Color.BLUE, Color.GREEN));
        combinations.add(new ColorCombination(Color.Pink, Color.GREEN));
        combinations.add(new ColorCombination(Color.Pink, Color.BLUE));

    }

    public static ColorCombination getColorCombinations(int teamCount) {

        ArrayList<ColorCombination> potential = new ArrayList<>();
        for(ColorCombination combination : combinations) {

            if(combination.colors.length == teamCount) {

                potential.add(combination);

            }

        }
        if(!potential.isEmpty()) {

            return potential.get(new Random().nextInt(potential.size() - 1)).shuffledCopy();

        } else {

            ArrayList<ColorCombination> alternative = new ArrayList<>();
            for(ColorCombination combination : combinations) {

                if(combination.colors.length >= teamCount) {

                    alternative.add(combination);

                }

            }

            ColorCombination combination = alternative.get(new Random().nextInt(alternative.size() - 1)).shuffledCopy();
            return combination;

        }

    }

    private ColorCombination shuffledCopy() {

        ArrayList<Color> colors = new ArrayList<>();
        for(Color color : this.colors) {

            colors.add(color);

        }
        Collections.shuffle(colors);
        return new ColorCombination(colors.toArray(new Color[]{}));

    }

    public ColorCombination(Color... colors) {

        this.colors = colors;

    }

    private Color[] colors;
    public Color color(int i) { return colors[i]; }

}
