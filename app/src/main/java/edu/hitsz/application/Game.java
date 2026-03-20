package edu.hitsz.application;

import java.util.LinkedList;
import java.util.List;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.factory.enemy.EnemyType;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;

/**
 * Lightweight Android compatibility Game model.
 *
 * <p>This class only preserves APIs referenced by difficulty templates and other
 * gameplay components during migration from desktop Swing to Android.</p>
 */
public class Game {

    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircraft;
    private final UnifiedEnemyFactory enemyFactory;

    private int time;
    private int score;
    private int enemyMaxNumber = 5;
    private int bossScoreThreshold = 500;
    private boolean bossExists;
    private double eliteProbability = 0.3;
    private double elitePlusProbability = 0.1;

    public Game() {
        this.heroAircraft = HeroAircraft.getInstance();
        this.enemyAircraft = new LinkedList<>();
        this.enemyFactory = new UnifiedEnemyFactory().enableRandom(eliteProbability, elitePlusProbability);
    }

    public int getTime() {
        return time;
    }

    public int getScore() {
        return score;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isBossExists() {
        return bossExists;
    }

    public int getBossScoreThreshold() {
        return bossScoreThreshold;
    }

    public void setBossScoreThreshold(int bossScoreThreshold) {
        this.bossScoreThreshold = bossScoreThreshold;
    }

    public int getEnemyCount() {
        return enemyAircraft.size();
    }

    public int getEnemyMaxNumber() {
        return enemyMaxNumber;
    }

    public void setEnemyMaxNumber(int enemyMaxNumber) {
        this.enemyMaxNumber = enemyMaxNumber;
    }

    public double getEliteProbability() {
        return eliteProbability;
    }

    public void setEliteProbability(double eliteProbability) {
        this.eliteProbability = eliteProbability;
    }

    public double getElitePlusProbability() {
        return elitePlusProbability;
    }

    public void setElitePlusProbability(double elitePlusProbability) {
        this.elitePlusProbability = elitePlusProbability;
    }

    public List<AbstractAircraft> getEnemyList() {
        return enemyAircraft;
    }

    public UnifiedEnemyFactory getEnemyFactory() {
        return enemyFactory;
    }

    public void setHeroShootCycle(int ms) {
        heroAircraft.setShootCycle(ms);
    }

    public void spawnOneEnemy() {
        enemyAircraft.add(enemyFactory.createEnemy());
    }

    public void spawnBoss() {
        enemyFactory.disableRandom();
        enemyAircraft.add(enemyFactory.setType(EnemyType.BOSS).createEnemy());
        enemyFactory.enableRandom(eliteProbability, elitePlusProbability);
        bossExists = true;
    }

    public void markBossDefeated() {
        bossExists = false;
    }
}
