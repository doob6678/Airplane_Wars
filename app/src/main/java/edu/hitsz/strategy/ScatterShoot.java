package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

public class ScatterShoot implements ShootStrategy {
    @Override
    public List<BaseBullet> shoot(int locationX, int locationY, int speedX, int speedY, int direction, int shootNum,
                                  int power) {
        List<BaseBullet> res = new LinkedList<>();
        int y = locationY + direction * 2;
        int bulletSpeedY = speedY + direction * 5;
        int bulletSpeedXStep = 2; // 子弹横向速度增量

        for (int i = 0; i < shootNum; i++) {
            // 散射的子弹拥有不同的横向速度
            int bulletSpeedX = (i - (shootNum - 1) / 2) * bulletSpeedXStep;
            BaseBullet bullet;
            if (direction == -1) { // 英雄机
                bullet = new HeroBullet(locationX, y, bulletSpeedX, bulletSpeedY, power);
            } else { // 敌机
                bullet = new EnemyBullet(locationX, y, bulletSpeedX, bulletSpeedY, power);
            }
            res.add(bullet);
        }
        return res;
    }
}