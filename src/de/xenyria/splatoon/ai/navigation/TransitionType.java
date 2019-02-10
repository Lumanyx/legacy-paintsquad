package de.xenyria.splatoon.ai.navigation;

public enum TransitionType {

    ROLL_ENEMY_TURF(1d),
    ROLL_UNPAINTED(3d),
    ROLL_COVERED(7d),
    WALK(4d),
    WALK_ENEMY(25d),
    CLIMB(14d),
    JUMP_TO(20d),
    SWIM(1d),
    SWIM_BLOCKED(25d),
    INK_RAIL(9d),
    RIDE_RAIL(9d),
    SWIM_WALL_VERTICAL(40d),
    FALL(35d),
    ENTER_FOUNTAIN(1d),
    SWIM_DRY(18d);

    private double weight;
    public double getWeight() { return weight; }

    public boolean isRollNode() {

        return this == ROLL_ENEMY_TURF || this == ROLL_COVERED || this == ROLL_UNPAINTED;

    }

    TransitionType(double weight) {

        this.weight = weight;

    }


}
