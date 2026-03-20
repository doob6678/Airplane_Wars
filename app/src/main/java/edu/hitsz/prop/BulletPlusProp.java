package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.strategy.CircleShoot;

public class BulletPlusProp extends AbstractProp {
    private static final int DURATION = 4000;

    public BulletPlusProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void effect(HeroAircraft heroAircraft) {
        System.out.println("SuperFireSupply active! Change to CircleShoot.");
        heroAircraft.activatePowerUp(new CircleShoot(), 12, DURATION);
    }
}