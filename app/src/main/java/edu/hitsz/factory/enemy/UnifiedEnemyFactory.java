package edu.hitsz.factory.enemy;

import edu.hitsz.aircraft.*;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;

/**
 * 统一敌机工厂，通过传入类型创建不同敌机，或在启用随机后按概率随机创建
 */
public class UnifiedEnemyFactory implements EnemyFactory {

    private EnemyType type = EnemyType.MOB;

    // 随机模式配置
    private boolean randomEnabled = false;
    private double eliteProbability = 0.3;
    private double elitePlusProbability = 0.1;

    // 可配置的默认属性
    private int mobHp = 30;
    private int mobSpeedY = 10;
    private int eliteHp = 60;
    private int eliteBaseSpeedY = 10;
    private int elitePlusHp = 100;
    private int elitePlusBaseSpeedY = 8;
    private int bossHp = 2000;
    private int bossSpeedX = 5;

    public UnifiedEnemyFactory() {
    }

    public UnifiedEnemyFactory(EnemyType type) {
        this.type = type;
    }

    public UnifiedEnemyFactory setType(EnemyType type) {
        this.type = type;
        return this;
    }

    public UnifiedEnemyFactory configMob(int hp, int speedY) {
        this.mobHp = hp;
        this.mobSpeedY = speedY;
        return this;
    }

    public UnifiedEnemyFactory configElite(int hp, int baseSpeedY) {
        this.eliteHp = hp;
        this.eliteBaseSpeedY = baseSpeedY;
        return this;
    }

    public UnifiedEnemyFactory configElitePlus(int hp, int baseSpeedY) {
        this.elitePlusHp = hp;
        this.elitePlusBaseSpeedY = baseSpeedY;
        return this;
    }

    public UnifiedEnemyFactory configBoss(int hp, int speedX) {
        this.bossHp = hp;
        this.bossSpeedX = speedX;
        return this;
    }

    // 启用/配置随机生成
    public UnifiedEnemyFactory enableRandom(double eliteProbability, double elitePlusProbability) {
        this.randomEnabled = true;
        this.eliteProbability = eliteProbability;
        this.elitePlusProbability = elitePlusProbability;
        return this;
    }

    public UnifiedEnemyFactory disableRandom() {
        this.randomEnabled = false;
        return this;
    }

    public UnifiedEnemyFactory setEliteProbability(double eliteProbability) {
        this.eliteProbability = eliteProbability;
        return this;
    }

    public UnifiedEnemyFactory setElitePlusProbability(double elitePlusProbability) {
        this.elitePlusProbability = elitePlusProbability;
        return this;
    }

    @Override
    public AbstractEnemy createEnemy() {
        if (randomEnabled) {
            double r = Math.random();
            if (r < elitePlusProbability) {
                return createElitePlus();
            } else if (r < elitePlusProbability + eliteProbability) {
                return createElite();
            } else {
                return createMob();
            }
        }
        switch (type) {
            case BOSS:
                return createBoss();
            case ELITE_PLUS:
                return createElitePlus();
            case ELITE:
                return createElite();
            case MOB:
            default:
                return createMob();
        }
    }

    private AbstractEnemy createMob() {
        int x = (int) (Math.random() * (Main.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int y = (int) (Math.random() * Main.WINDOW_HEIGHT * 0.05);
        return new MobEnemy(x, y, 0, mobSpeedY, mobHp);
    }

    private AbstractEnemy createElite() {
        int x = (int) (Math.random() * (Main.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth()));
        int y = (int) (Math.random() * Main.WINDOW_HEIGHT * 0.05);
        int speedX = (int) (Math.random() * 20 - 10);
        return new EliteEnemy(x, y, speedX, eliteBaseSpeedY, eliteHp);
    }

    private AbstractEnemy createElitePlus() {
        int x = (int) (Math.random() * (Main.WINDOW_WIDTH - ImageManager.ELITE_PLUS_ENEMY_IMAGE.getWidth()));
        int y = (int) (Math.random() * Main.WINDOW_HEIGHT * 0.05);
        int speedX = (int) (Math.random() * 10 - 5);
        return new ElitePlusEnemy(x, y, speedX, elitePlusBaseSpeedY, elitePlusHp);
    }

    private AbstractEnemy createBoss() {
        int x = (int) (Math.random() * (Main.WINDOW_WIDTH - ImageManager.BOSS_ENEMY_IMAGE.getWidth()));
        int y = (int) (ImageManager.BOSS_ENEMY_IMAGE.getHeight() / 2.0);
        return new BossEnemy(x, y, bossSpeedX, 0, bossHp);
    }
}