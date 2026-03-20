package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.ArrayList;
import java.util.List;

public class CircleShoot implements ShootStrategy {
    @Override
    public List<BaseBullet> shoot(int locationX, int locationY, int speedX, int speedY, int direction, int shootNum,
                                  int power) {
        List<BaseBullet> res = new ArrayList<>();
        int bulletSpeed = 6;
        double angleStep = 360.0 / shootNum;

        for (int i = 0; i < shootNum; i++) {
            double angle = Math.toRadians(i * angleStep);
            int bulletSpeedX = (int) (bulletSpeed * Math.sin(angle));
            int bulletSpeedY = (int) (bulletSpeed * Math.cos(angle));
            BaseBullet bullet;
            if (direction == -1) { // 英雄机
                bullet = new HeroBullet(locationX, locationY, bulletSpeedX, bulletSpeedY, power);
            } else { // 敌机
                bullet = new EnemyBullet(locationX, locationY, bulletSpeedX, bulletSpeedY, power);
            }
            res.add(bullet);
        }
        return res;
    }
}