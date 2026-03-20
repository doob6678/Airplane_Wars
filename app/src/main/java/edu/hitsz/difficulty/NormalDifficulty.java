package edu.hitsz.difficulty;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.application.Game;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;

public class NormalDifficulty extends DifficultyTemplate {

    private int stage = 0;
    private int nextStageTimeMs = 10000; // 每10秒提升一次

    public NormalDifficulty() {
        super("Normal");
    }

    @Override
    protected void doInit(Game game) {
        game.setEnemyMaxNumber(7);
        game.setBossScoreThreshold(500);
        // 敌机属性
        UnifiedEnemyFactory f = game.getEnemyFactory();
        f.configMob(30, 10)
                .configElite(60, 10)
                .configElitePlus(100, 8)
                .configBoss(2500, 6)
                .enableRandom(0.30, 0.10);
        // 英雄/敌机射击周期
        game.setHeroShootCycle(200);
        game.setEliteProbability(0.30);
        game.setElitePlusProbability(0.10);
        System.out.println("[Normal] 初始化完成：将随时间逐步提升难度，并按阈值触发Boss。");
    }

    @Override
    protected void scaleOverTime(Game game) {
        int t = game.getTime();
        if (t >= nextStageTimeMs) {
            stage++;
            nextStageTimeMs += 10000;

            // 提升概率与属性
            UnifiedEnemyFactory f = game.getEnemyFactory();
            double eliteP = Math.min(0.45, game.getEliteProbability() + 0.03);
            double elitePlusP = Math.min(0.20, game.getElitePlusProbability() + 0.02);
            f.setEliteProbability(eliteP).setElitePlusProbability(elitePlusP);
            game.setEliteProbability(eliteP);
            game.setElitePlusProbability(elitePlusP);

            // 基础属性成长
            f.configMob(30 + stage * 4, 10 + (stage >= 2 ? 1 : 0));
            f.configElite(60 + stage * 6, 10 + (stage >= 3 ? 1 : 0));
            f.configElitePlus(100 + stage * 8, 8 + (stage >= 4 ? 1 : 0));

            // 缩短现存精英类敌机射击周期（不低于500ms）
            int newCycle = Math.max(500, 800 - stage * 30);
            for (AbstractAircraft e : game.getEnemyList()) {
                if (e instanceof EliteEnemy || e instanceof ElitePlusEnemy) {
                    e.setShootCycle(newCycle);
                }
            }

            System.out.printf("[Normal] 难度提升至 Lv.%d：eliteP=%.2f, elitePlusP=%.2f, 敌机射击周期=%dms%n",
                    stage, eliteP, elitePlusP, newCycle);
        }
    }

    @Override
    protected void spawnEnemies(Game game) {
        // Boss 触发
        if (!game.isBossExists() && game.getScore() >= game.getBossScoreThreshold()) {
            game.spawnBoss();
            System.out.println("[Normal] Boss 出现！当前阈值=" + game.getBossScoreThreshold());
        }

        // 常规敌机补足
        if (!game.isBossExists() && game.getEnemyCount() < game.getEnemyMaxNumber()) {
            game.spawnOneEnemy();
        }
    }
}