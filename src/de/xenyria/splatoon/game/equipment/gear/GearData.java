package de.xenyria.splatoon.game.equipment.gear;

import de.xenyria.splatoon.game.equipment.gear.level.GearLevel;
import de.xenyria.splatoon.game.equipment.gear.level.GearLevelManager;
import de.xenyria.splatoon.game.player.userdata.level.Level;

import java.util.ArrayList;

public class GearData {

    private Gear gear;
    public GearData(Gear base) {

        this.gear = base;
        this.primaryEffect = base.getDefaultEffect();

    }

    private SpecialEffect primaryEffect;
    public SpecialEffect getPrimaryEffect() { return primaryEffect; }
    public void setPrimaryEffect(SpecialEffect effect) { this.primaryEffect = effect; }

    private ArrayList<SpecialEffect> subAbilities = new ArrayList<>();
    public ArrayList<SpecialEffect> getSubAbilities() { return subAbilities; }

    private int experience;
    public int getExperience() { return experience; }
    public void setExperience(int experience) {

        this.experience = experience;

        int maxExp = totalExperience();
        if(experience > maxExp) {

            this.experience = maxExp;

        }

    }
    public int totalExperience() {

        ArrayList<GearLevel> levels = GearLevelManager.getLevels(gear.getMaxSubAbilities());
        return levels.get(levels.size() - 1).getStart() + levels.get(levels.size() - 1).getExperience();

    }

    public double levelPercentage() {

        GearLevel level = GearLevelManager.getCurrentLevel(stars(), experience);
        int start = level.getStart();
        int end = level.getStart() + level.getExperience();
        int relStart = 0;
        int relEnd = end-start;
        int relProgress = experience-start;

        return ((float)relProgress / (float)relEnd) * 100f;

    }

    public GearLevel nextLevel() {

        return GearLevelManager.getNextLevel(stars(), experience);

    }

    public int stars() { return gear.getMaxSubAbilities(); }

    public int experienceToNextRank() {

        GearLevel currentLevel = GearLevelManager.getCurrentLevel(gear.getMaxSubAbilities(), experience);
        int target = currentLevel.getStart() + currentLevel.getExperience();
        return target-experience;

    }

    public boolean isFullyLevelled() {

        GearLevel level = nextLevel();
        if(level == null) {

            GearLevel currentLevel = GearLevelManager.getCurrentLevel(gear.getMaxSubAbilities(), experience);
            return experience >= (currentLevel.getStart() + currentLevel.getExperience());

        }
        return false;

    }

    public int currentLevel() {

        return GearLevelManager.getCurrentLevel(gear.getMaxSubAbilities(), experience).getID();

    }

    public void addSubEffect(SpecialEffect effect) {

        if(subAbilities.size() < gear.getMaxSubAbilities()) {

            subAbilities.add(effect);

        }

    }

    public int firstSubIndex() {

        if(subAbilities.isEmpty()) {

            return 0;

        } else {

            return subAbilities.size() - 1;

        }

    }

}
