package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.basic.AbstractFlyingObject;

public abstract class AbstractProp extends AbstractFlyingObject {

    /**
     * 道具构造方法
     *
     * @param locationX X 坐标
     * @param locationY Y 坐标
     * @param speedX    X 轴速度
     * @param speedY    Y 轴速度
     */
    public AbstractProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    /**
     * 道具向下飞行
     * 超出屏幕底部边界时消失
     */
    @Override
    public void forward() {
        super.forward();
        // 道具超出屏幕底部时消失
        if (locationY >= edu.hitsz.application.Main.WINDOW_HEIGHT) {
            vanish();
        }
    }

    /**
     * 道具生效的抽象方法
     * 具体道具类需要实现此方法定义道具效果
     *
     * @param heroAircraft 英雄机对象
     */
    public abstract void effect(HeroAircraft heroAircraft);
}
