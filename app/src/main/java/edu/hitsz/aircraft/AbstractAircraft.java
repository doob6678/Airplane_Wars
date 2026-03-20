package edu.hitsz.aircraft;

import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.strategy.ShootStrategy;

import java.util.List;

/**
 * 所有种类飞机的抽象父类：
 * 敌机（BOSS, ELITE, MOB），英雄飞机
 *
 * @author hitsz
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {
    /**
     * 最大生命值
     */
    protected int maxHp;

    /**
     * 当前生命值
     */
    protected int hp;

    /**
     * 射击策略
     */
    protected ShootStrategy shootStrategy;

    /**
     * 射击方向 (向上: -1, 向下: 1)
     */
    protected int direction = 1;

    /**
     * 子弹数量
     */
    protected int shootNum = 1;

    /**
     * 子弹威力
     */
    protected int power = 10;

    /**
     * 射击间隔周期
     */
    protected int shootCycle = 600;

    /**
     * 射击计时器
     */
    protected int shootTimer = 0;

    /**
     * 飞机构造方法
     *
     * @param locationX X 坐标
     * @param locationY Y 坐标
     * @param speedX    X 轴速度
     * @param speedY    Y 轴速度
     * @param hp        生命值
     */
    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.maxHp = hp;
    }

    /**
     * 减少生命值
     *
     * @param decrease 减少的生命值
     */
    public void decreaseHp(int decrease) {
        hp -= decrease;
        if (hp <= 0) {
            hp = 0;
            vanish();
        }
    }

    public int getHp() {
        return hp;
    }

    /**
     * 设置生命值
     *
     * @param hp 生命值
     */
    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    /**
     * 更新射击计时器，若达到射击周期则重置计时器并返回 true
     *
     * @param timeInterval 时间间隔
     * @return 是否达到射击周期
     */
    public boolean updateShootTimer(int timeInterval) {
        shootTimer += timeInterval;
        if (shootTimer >= shootCycle) {
            shootTimer %= shootCycle;
            return true;
        }
        return false;
    }

    /**
     * 设置射击策略
     *
     * @param shootStrategy 射击策略
     */
    public void setShootStrategy(ShootStrategy shootStrategy) {
        this.shootStrategy = shootStrategy;
    }

    /**
     * 设置射击周期
     *
     * @param shootCycle 射击周期
     */
    public void setShootCycle(int shootCycle) {
        this.shootCycle = shootCycle;
    }

    /**
     * 飞机射击方法，可射击对象必须实现
     *
     * @return 可射击对象需实现，返回子弹，非可射击对象空实现，返回 null
     */
    public List<BaseBullet> shoot() {
        if (shootStrategy == null) {
            return null;
        }
        return shootStrategy.shoot(
                this.getLocationX(),
                this.getLocationY(),
                this.getSpeedX(),
                this.getSpeedY(),
                this.direction,
                this.shootNum,
                this.power);
    }

}
