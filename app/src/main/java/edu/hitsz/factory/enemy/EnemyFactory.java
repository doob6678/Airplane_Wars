package edu.hitsz.factory.enemy;

import edu.hitsz.aircraft.AbstractEnemy;

/**
 * 敌机工厂接口：生成一个敌机实例
 */
public interface EnemyFactory {
    AbstractEnemy createEnemy();
}
