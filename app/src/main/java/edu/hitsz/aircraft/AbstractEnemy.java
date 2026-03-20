package edu.hitsz.aircraft;

import edu.hitsz.prop.AbstractProp;

import java.util.List;

/**
 * 敌机抽象类，为所有敌机提供共同的行为定义
 * 继承自AbstractAircraft，添加了得分和掉落道具的抽象方法
 */
public abstract class AbstractEnemy extends AbstractAircraft {

    /**
     * 敌机构造方法
     *
     * @param locationX X 坐标
     * @param locationY Y 坐标
     * @param speedX    X 轴速度
     * @param speedY    Y 轴速度
     * @param hp        生命值
     */
    public AbstractEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    /**
     * 获取击败该敌机的得分
     *
     * @return 得分值
     */
    public abstract int getScore();

    /**
     * 敌机可能掉落的道具
     *
     * @return 道具列表，可能为空
     */
    public abstract List<AbstractProp> mayDrop();
}
