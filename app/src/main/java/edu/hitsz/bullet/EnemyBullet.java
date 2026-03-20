package edu.hitsz.bullet;

import edu.hitsz.observer.BombPublisher;
import edu.hitsz.observer.BombSubscriber;

/**
 * @author hitsz
 */
public class EnemyBullet extends BaseBullet implements BombSubscriber {

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
        // 注册为炸弹观察者
        BombPublisher.register(this);
    }

    @Override
    public int onBomb() {
        if (this.notValid()) {
            return 0;
        }
        this.vanish();
        return 0;
    }
}
