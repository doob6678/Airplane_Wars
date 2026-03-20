package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;

import java.util.List;

/**
 * 射击策略接口
 */
public interface ShootStrategy {
    /**
     * @param locationX 子弹生成位置 x
     * @param locationY 子弹生成位置 y
     * @param speedX    子弹速度 x
     * @param speedY    子弹速度 y
     * @param direction 子弹射击方向 (1:向下, -1:向上)
     * @param shootNum  子弹数量
     * @param power     子弹威力
     * @return 子弹列表
     */
    List<BaseBullet> shoot(int locationX, int locationY, int speedX, int speedY, int direction, int shootNum,
                           int power);
}