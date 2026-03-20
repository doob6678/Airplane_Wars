package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.observer.BombPublisher;

public class BombProp extends AbstractProp {
    public BombProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void effect(HeroAircraft heroAircraft) {
        System.out.println("BombSupply active!");
        // 触发炸弹事件（累计分数由 Game 在拾取逻辑中提取并结算）
        BombPublisher.notifyBomb();
    }
}
