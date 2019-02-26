package de.xenyria.splatoon.game.equipment;

import de.xenyria.splatoon.XenyriaSplatoon;
import de.xenyria.splatoon.game.equipment.gear.Gear;
import de.xenyria.splatoon.game.equipment.gear.boots.FootGear;
import de.xenyria.splatoon.game.equipment.gear.chest.BodyGear;
import de.xenyria.splatoon.game.equipment.gear.head.HeadGear;
import de.xenyria.splatoon.game.equipment.weapon.primary.SplatoonPrimaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.secondary.SplatoonSecondaryWeapon;
import de.xenyria.splatoon.game.equipment.weapon.set.WeaponSet;
import de.xenyria.splatoon.game.equipment.weapon.special.SplatoonSpecialWeapon;
import de.xenyria.splatoon.game.player.SplatoonHumanPlayer;
import de.xenyria.splatoon.game.player.SplatoonPlayer;

public class Equipment {

    private SplatoonPlayer player;
    public SplatoonPlayer getPlayer() { return player; }

    public Equipment(SplatoonPlayer player) {

        this.player = player;
        defaultEquipment();

    }

    private SplatoonPrimaryWeapon primaryWeapon;
    public SplatoonPrimaryWeapon getPrimaryWeapon() { return primaryWeapon; }
    public void setPrimaryWeapon(int weaponID) {

        primaryWeapon = (SplatoonPrimaryWeapon) XenyriaSplatoon.getWeaponRegistry().newInstance(weaponID);
        primaryWeapon.assign(player);
        if(player instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer)player).updateInventory();

        }

    }

    private SplatoonSecondaryWeapon secondaryWeapon;
    public SplatoonSecondaryWeapon getSecondaryWeapon() {
        return secondaryWeapon;
    }
    public void setSecondaryWeapon(int secondaryWeapon1) {
        secondaryWeapon = (SplatoonSecondaryWeapon) XenyriaSplatoon.getWeaponRegistry().newInstance(secondaryWeapon1);
        secondaryWeapon.assign(player);

        if(player instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer)player).updateInventory();

        }
    }

    public void syncTick() {

        if(getPlayer().getMatch() != null && !getPlayer().getMatch().inLobbyPhase() && !getPlayer().getMatch().inIntro() && !getPlayer().getMatch().inOutro()) {

            if(!getPlayer().isSpectator()) {

                if (primaryWeapon != null) {
                    primaryWeapon.syncTick();
                }
                if (secondaryWeapon != null) {
                    secondaryWeapon.syncTick();
                }
                if (specialWeapon != null) {
                    specialWeapon.syncTick();
                }

            }

        }

    }

    public void asyncTick() {

        if(getPlayer().getMatch() != null && !getPlayer().getMatch().inLobbyPhase() && !getPlayer().getMatch().inIntro() && !getPlayer().getMatch().inOutro()) {

            if(!getPlayer().isSpectator()) {

                if (primaryWeapon != null) {
                    primaryWeapon.asyncTick();
                }
                if (secondaryWeapon != null) {
                    secondaryWeapon.asyncTick();
                }
                if (specialWeapon != null) {
                    specialWeapon.asyncTick();
                }

            }

        }

    }

    private SplatoonSpecialWeapon specialWeapon;
    public SplatoonSpecialWeapon getSpecialWeapon() { return specialWeapon; }
    public void setSpecialWeapon(int i) {

        specialWeapon = (SplatoonSpecialWeapon) XenyriaSplatoon.getWeaponRegistry().newInstance(i);
        specialWeapon.assign(player);

        if(player instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer)player).updateInventory();

        }

        player.setRequiredSpecialPoints(specialWeapon.getRequiredPoints());

    }

    public void defaultEquipment() {

        headGear = (HeadGear) XenyriaSplatoon.getGearRegistry().newInstance(1);
        bodyGear = (BodyGear) XenyriaSplatoon.getGearRegistry().newInstance(2);
        footGear = (FootGear) XenyriaSplatoon.getGearRegistry().newInstance(3);

        if(player instanceof SplatoonHumanPlayer) {

            ((SplatoonHumanPlayer) player).updateInventory();

        }

    }

    private HeadGear headGear;
    public HeadGear getHeadGear() { return headGear; }

    private BodyGear bodyGear;
    public BodyGear getBodyGear() { return bodyGear; }

    private FootGear footGear;
    public FootGear getFootGear() { return footGear; }


    public void resetPrimaryWeapon() {

        if(primaryWeapon != null) {

            primaryWeapon.uninitialize();
            primaryWeapon = null;

        }

    }

    public void resetSecondaryWeapon() {

        if(secondaryWeapon != null) {

            secondaryWeapon.uninitialize();
            secondaryWeapon = null;

        }

    }

    public void resetSpecialWeapon() {

        if(specialWeapon != null) {

            specialWeapon.uninitialize();
            specialWeapon = null;

        }

    }

    public void applySet(WeaponSet set) {

        setPrimaryWeapon(set.getPrimaryWeapon());
        setSecondaryWeapon(set.getSecondary());
        setSpecialWeapon(set.getSpecial());

    }

    public void setHeadGear(Gear gear) {

        this.headGear = (HeadGear) gear;
        player.updateEquipment();

    }
    public void setBodyGear(Gear gear) {

        this.bodyGear = (BodyGear) gear;
        player.updateEquipment();

    }
    public void setFootGear(Gear gear) {

        this.footGear = (FootGear) gear;
        player.updateEquipment();

    }

    public void unassignWeapons() {

        if(primaryWeapon != null) { primaryWeapon.uninitialize(); }
        if(secondaryWeapon != null) { secondaryWeapon.uninitialize(); }
        if(specialWeapon != null) { specialWeapon.uninitialize(); }
        primaryWeapon = null; secondaryWeapon = null; specialWeapon = null;

    }

    public void resetWeapons() {

        if(player.getEquipment().getPrimaryWeapon() != null) {

            player.getEquipment().resetPrimaryWeapon();

        }

        if(player.getEquipment().getSecondaryWeapon() != null) {

            player.getEquipment().resetSecondaryWeapon();

        }

        if(player.getEquipment().getSpecialWeapon() != null) {

            player.getEquipment().resetSpecialWeapon();

        }

    }

}
