package edu.hitsz.difficulty;

import edu.hitsz.application.Game;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;

public class EasyDifficulty extends DifficultyTemplate {

    public EasyDifficulty() {
        super("Easy");
    }

    @Override
    protected void doInit(Game game) {
        game.setEnemyMaxNumber(5);
        game.setBossScoreThreshold(Integer.MAX_VALUE); // 永不触发
        // 敌机属性
        UnifiedEnemyFactory f = game.getEnemyFactory();
        f.configMob(25, 8)
                .configElite(50, 9)
                .configElitePlus(80, 7)
                .configBoss(2000, 5) // 无意义，但保持一致
                .enableRandom(0.25, 0.0);
        // 英雄/敌机射击周期
        game.setHeroShootCycle(200);
        // 存储概率到 Game 方便展示/查询
        game.setEliteProbability(0.25);
        game.setElitePlusProbability(0.0);
        System.out.println("[Easy] 初始化完成：无Boss，难度不随时间变化。");
    }

    @Override
    protected void scaleOverTime(Game game) {
        // Easy 不随时间变化
    }

    @Override
    protected void spawnEnemies(Game game) {
        // 不生成Boss，仅补足普通敌机
        if (game.getEnemyCount() < game.getEnemyMaxNumber()) {
            game.spawnOneEnemy();
        }
    }
}