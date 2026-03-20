package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;

public class BloodProp extends AbstractProp {

    private static final int BLOOD_INCREASE = 25; // 加血量

    public BloodProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void effect(HeroAircraft heroAircraft) {
        int currentHp = heroAircraft.getHp();
        int maxHp = heroAircraft.getMaxHp();
        int newHp = Math.min(currentHp + BLOOD_INCREASE, maxHp);
        heroAircraft.setHp(newHp);
    }
}
