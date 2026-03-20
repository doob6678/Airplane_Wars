package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

public class DirectShoot implements ShootStrategy {
    @Override
    public List<BaseBullet> shoot(int locationX, int locationY, int speedX, int speedY, int direction, int shootNum,
                                  int power) {
        List<BaseBullet> res = new LinkedList<>();
        int y = locationY + direction * 2;
        int bulletSpeedY = speedY + direction * 5;
        BaseBullet bullet;
        for (int i = 0; i < shootNum; i++) {
            // 子弹发射位置相对飞机位置向前偏移
            // 多个子弹横向分散
            if (direction == -1) { // 英雄机
                bullet = new HeroBullet(locationX + (i * 2 - shootNum + 1) * 10, y, speedX, bulletSpeedY, power);
            } else { // 敌机
                bullet = new EnemyBullet(locationX + (i * 2 - shootNum + 1) * 10, y, speedX, bulletSpeedY, power);
            }
            res.add(bullet);
        }
        return res;
    }
}