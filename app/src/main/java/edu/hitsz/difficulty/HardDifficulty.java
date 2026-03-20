package edu.hitsz.difficulty;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.application.Game;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;

public class HardDifficulty extends DifficultyTemplate {

    private int stage = 0;
    private int nextStageTimeMs = 8000; // 每8秒提升一次

    public HardDifficulty() {
        super("Hard");
    }

    @Override
    protected void doInit(Game game) {
        game.setEnemyMaxNumber(9);
        game.setBossScoreThreshold(400);
        UnifiedEnemyFactory f = game.getEnemyFactory();
        f.configMob(40, 12)
                .configElite(80, 11)
                .configElitePlus(130, 9)
                .configBoss(3000, 7)
                .enableRandom(0.35, 0.15);
        // 英雄射击更慢
        game.setHeroShootCycle(240);
        game.setEliteProbability(0.35);
        game.setElitePlusProbability(0.15);
        System.out.println("[Hard] 初始化完成：高强度，随时间快速提升难度，并按阈值触发Boss。");
    }

    @Override
    protected void scaleOverTime(Game game) {
        int t = game.getTime();
        if (t >= nextStageTimeMs) {
            stage++;
            nextStageTimeMs += 8000;

            UnifiedEnemyFactory f = game.getEnemyFactory();
            double eliteP = Math.min(0.55, game.getEliteProbability() + 0.04);
            double elitePlusP = Math.min(0.25, game.getElitePlusProbability() + 0.03);
            f.setEliteProbability(eliteP).setElitePlusProbability(elitePlusP);
            game.setEliteProbability(eliteP);
            game.setElitePlusProbability(elitePlusP);

            // 敌机属性增长更快
            f.configMob(40 + stage * 6, 12 + (stage >= 1 ? 1 : 0));
            f.configElite(80 + stage * 10, 11 + (stage >= 2 ? 1 : 0));
            f.configElitePlus(130 + stage * 12, 9 + (stage >= 3 ? 1 : 0));

            // 缩短现存敌机射击周期（不低于400ms）
            int newCycle = Math.max(400, 800 - stage * 50);
            for (AbstractAircraft e : game.getEnemyList()) {
                if (e instanceof EliteEnemy || e instanceof ElitePlusEnemy) {
                    e.setShootCycle(newCycle);
                }
            }

            // 英雄射击再慢一点，但不超过300ms
            int heroCycle = Math.min(300, 240 + stage * 10);
            game.setHeroShootCycle(heroCycle);

            // 提高压迫：动态降低Boss阈值（有下限）
            int th = Math.max(300, game.getBossScoreThreshold() - 20);
            game.setBossScoreThreshold(th);

            System.out.printf("[Hard] 难度提升至 Lv.%d：eliteP=%.2f, elitePlusP=%.2f, 敌机射击周期=%dms, 英雄周期=%dms, Boss阈值=%d%n",
                    stage, eliteP, elitePlusP, newCycle, heroCycle, th);
        }
    }

    @Override
    protected void spawnEnemies(Game game) {
        if (!game.isBossExists() && game.getScore() >= game.getBossScoreThreshold()) {
            game.spawnBoss();
            System.out.println("[Hard] Boss 出现！当前阈值=" + game.getBossScoreThreshold());
        }
        if (!game.isBossExists() && game.getEnemyCount() < game.getEnemyMaxNumber()) {
            game.spawnOneEnemy();
        }
    }
}