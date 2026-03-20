package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.observer.BombPublisher;
import edu.hitsz.observer.BombSubscriber;
import edu.hitsz.prop.AbstractProp;

import java.util.LinkedList;
import java.util.List;

/**
 * 普通敌机
 * 不可射击
 *
 * @author hitsz
 */
public class MobEnemy extends AbstractEnemy implements BombSubscriber {

    /**
     * 普通敌机构造方法
     *
     * @param locationX X 坐标
     * @param locationY Y 坐标
     * @param speedX    X 轴速度
     * @param speedY    Y 轴速度
     * @param hp        生命值
     */
    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 注册为炸弹观察者
        BombPublisher.register(this);
        // 普通敌机不可射击
        this.shootStrategy = null;
    }

    @Override
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= Main.WINDOW_HEIGHT) {
            vanish();
        }
    }

    @Override
    public List<BaseBullet> shoot() {
        // 普通敌机不发射子弹
        return new LinkedList<>();
    }

    @Override
    public int getScore() {
        return 10;
    }

    @Override
    public List<AbstractProp> mayDrop() {
        // 普通敌机不掉落道具
        return new LinkedList<>();
    }

    @Override
    public int onBomb() {
        if (this.notValid()) {
            return 0;
        }
        this.vanish();
        return getScore();
    }
}
