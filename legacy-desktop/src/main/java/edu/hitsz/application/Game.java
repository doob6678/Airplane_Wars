package edu.hitsz.application;

import android.graphics.Bitmap;

import edu.hitsz.aircraft.*;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.dao.FileScoreDaoImpl;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;
import edu.hitsz.difficulty.DifficultyTemplate;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;
import edu.hitsz.observer.BombPublisher;
import edu.hitsz.observer.BombSubscriber;
import edu.hitsz.prop.AbstractProp;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 游戏主面板，游戏启动
 *
 * @author hitsz
 */
public class Game extends JPanel {

    private final Bitmap backgroundImage;
    /**
     * Scheduled 线程池，用于任务调度
     */
    private final ScheduledExecutorService executorService;
    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircraft;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;
    // 统一敌机工厂（内聚随机生成能力）
    private final UnifiedEnemyFactory enemyFactory;
    /**
     * 得分数据访问对象
     */
    private final ScoreDao scoreDao;
    // 使用模板方法模式
    private final DifficultyTemplate difficulty;
    private final boolean soundEnabled;
    /**
     * 时间间隔(ms)，控制刷新频率
     */
    private final int timeInterval = 40;
    /**
     * 周期（ms)
     * 指示子弹的发射、敌机的产生频率
     */
    private final int cycleDuration = 600;
    private int backGroundTop = 0;
    /**
     * 屏幕中出现的敌机最大数量
     */
    private int enemyMaxNumber = 5;
    /**
     * 当前得分
     */
    private int score = 0;
    /**
     * 当前时刻
     */
    private int time = 0;
    private int cycleTime = 0;
    /**
     * 精英敌机出现概率
     */
    private double eliteProbability = 0.3;
    private double elitePlusProbability = 0.1;
    /**
     * Boss 存在标志和阈值
     */
    private boolean bossExists = false;
    private int bossScoreThreshold = 500;
    /**
     * 游戏结束标志
     */
    private boolean gameOverFlag = false;
    private MusicThread bgmThread;
    private MusicThread bossBgmThread;

    public Game(DifficultyTemplate difficulty, boolean soundEnabled) {
        this.difficulty = difficulty;
        this.soundEnabled = soundEnabled;
        switch (difficulty.getName()) {
            case "Easy":
                this.backgroundImage = ImageManager.BACKGROUND_IMAGE_EASY;
                break;
            case "Hard":
                this.backgroundImage = ImageManager.BACKGROUND_IMAGE_HARD;
                break;
            case "Normal":
            default:
                this.backgroundImage = ImageManager.BACKGROUND_IMAGE_NORMAL;
                break;
        }
        heroAircraft = HeroAircraft.getInstance();

        enemyAircraft = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();

        props = new LinkedList<>();

        // 初始化 DAO
        scoreDao = new FileScoreDaoImpl("ranklist_db/scores.dat");

        // 初始化统一敌机工厂并启用随机模式
        enemyFactory = new UnifiedEnemyFactory().enableRandom(eliteProbability, elitePlusProbability);

        if (soundEnabled) {
            bgmThread = new MusicThread("src/videos/bgm.wav");
            bgmThread.setLoop(true);
            bgmThread.start();
        }

        /*
         * Scheduled 线程池，用于定时任务调度
         * 关于alibaba code guide：可命名的 ThreadFactory 一般需要第三方包
         * apache 第三方库： org.apache.commons.lang3.concurrent.BasicThreadFactory
         */
        this.executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("game-action-%d").daemon(true).build());

        // 交由难度模板做初始化（参数、概率、阈值等）
        this.difficulty.init(this);

    }

    // ---------------- 新增对外暴露的 Hook/Getter/Setter ----------------
    public UnifiedEnemyFactory getEnemyFactory() {
        return enemyFactory;
    }

    public int getTime() {
        return time;
    }

    public int getScore() {
        return score;
    }

    public boolean isBossExists() {
        return bossExists;
    }

    public int getBossScoreThreshold() {
        return bossScoreThreshold;
    }

    public void setBossScoreThreshold(int v) {
        this.bossScoreThreshold = v;
    }

    public int getEnemyCount() {
        return enemyAircraft.size();
    }

    public int getEnemyMaxNumber() {
        return enemyMaxNumber;
    }

    public void setEnemyMaxNumber(int n) {
        this.enemyMaxNumber = n;
    }

    public double getEliteProbability() {
        return eliteProbability;
    }

    public void setEliteProbability(double p) {
        this.eliteProbability = p;
    }

    public double getElitePlusProbability() {
        return elitePlusProbability;
    }

    public void setElitePlusProbability(double p) {
        this.elitePlusProbability = p;
    }

    public List<AbstractAircraft> getEnemyList() {
        return enemyAircraft;
    }

    public void setHeroShootCycle(int ms) {
        heroAircraft.setShootCycle(ms);
    }

    // 由难度模板调用：生成一架普通敌机（遵循当前随机概率）
    public void spawnOneEnemy() {
        enemyAircraft.add(enemyFactory.createEnemy());
    }

    // 由难度模板调用：触发生成 Boss（含音乐切换）
    public void spawnBoss() {
        // 临时关闭随机模式，强制创建 Boss
        enemyFactory.disableRandom();
        enemyAircraft.add(
                enemyFactory
                        .setType(edu.hitsz.factory.enemy.EnemyType.BOSS)
                        .createEnemy());
        // 还原随机模式
        enemyFactory.enableRandom(eliteProbability, elitePlusProbability);
        bossExists = true;
        if (soundEnabled) {
            if (bgmThread != null) {
                bgmThread.stopMusic();
            }
            bossBgmThread = new MusicThread("src/videos/bgm_boss.wav");
            bossBgmThread.setLoop(true);
            bossBgmThread.start();
        }
    }

    /**
     * 游戏启动入口，执行游戏逻辑
     */
    public void action() {

        // 定时任务：绘制、对象产生、碰撞判定、击毁及结束判定
        Runnable task = () -> {

            time += timeInterval;

            // 周期性：生成、随时间难度调整（委托给难度模板）
            if (timeCountAndNewCycleJudge()) {
                difficulty.onCycle(this);
            }

            // 飞机射出子弹
            shootAction();

            // 子弹移动
            bulletsMoveAction();

            // 飞机移动
            aircraftMoveAction();

            // 道具移动
            propsMoveAction();

            // 撞击检测
            crashCheckAction();

            // 后处理
            postProcessAction();

            // 每个时刻重绘界面
            repaint();

            // 游戏结束检查英雄机是否存活
            if (heroAircraft.getHp() <= 0) {
                // 游戏结束
                executorService.shutdown();
                gameOverFlag = true;
                if (soundEnabled) {
                    if (bgmThread != null) {
                        bgmThread.stopMusic();
                    }
                    if (bossBgmThread != null) {
                        bossBgmThread.stopMusic();
                    }
                    new MusicThread("src/videos/game_over.wav").start();
                }
                System.out.println("Game Over!");

                // 处理得分和排行榜
                handleGameOver();
            }

        };

        /*
         * 以固定延迟时间进行执行
         * 本次任务执行完成后，需要延迟设定的延迟时间，才会执行新的任务
         */
        executorService.scheduleWithFixedDelay(task, timeInterval, timeInterval, TimeUnit.MILLISECONDS);

    }

    // ***********************
    // Action 各部分
    // ***********************

    private void handleGameOver() {
        // 弹出输入框获取玩家姓名
        String playerName = JOptionPane.showInputDialog(this, "游戏结束! 请输入你的名字:", "匿名玩家");
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "匿名玩家";
        }

        // 创建并保存得分记录
        ScoreRecord record = new ScoreRecord(playerName, this.score);
        scoreDao.addScore(record);

        // 切换到排行榜界面
        SwingUtilities.invokeLater(() -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.getContentPane().removeAll();
            Score scorePanel = new Score(this.difficulty.getName());
            frame.add(scorePanel);
            frame.revalidate();
            frame.repaint();
        });

        // 获取、排序并打印排行榜
        List<ScoreRecord> scores = scoreDao.getAllScores();
        Collections.sort(scores); // 按分数降序排序

        System.out.println("***************** 得分排行榜 *****************");
        for (int i = 0; i < scores.size(); i++) {
            ScoreRecord r = scores.get(i);
            System.out.printf("第 %d 名: %s，%d，%s%n",
                    i + 1, r.getPlayerName(), r.getScore(), r.getFormattedTime());
        }
        System.out.println("*******************************************");
        if (soundEnabled) {
            if (bgmThread != null) {
                bgmThread.stopMusic();
            }
            if (bossBgmThread != null) {
                bossBgmThread.stopMusic();
            }
        }
    }

    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            // 跨越到新的周期
            cycleTime %= cycleDuration;
            return true;
        } else {
            return false;
        }
    }

    private void shootAction() {
        // 敌机射击 - 每个敌机独立计时
        for (AbstractAircraft enemyAircraft : enemyAircraft) {
            if (enemyAircraft instanceof EliteEnemy || enemyAircraft instanceof ElitePlusEnemy
                    || enemyAircraft instanceof BossEnemy) {
                if (enemyAircraft.updateShootTimer(timeInterval)) {
                    enemyBullets.addAll(enemyAircraft.shoot());
                }
            }
        }

        // 英雄射击 - 独立计时
        if (heroAircraft.updateShootTimer(timeInterval)) {
            heroBullets.addAll(heroAircraft.shoot());
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircraft) {
            enemyAircraft.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    /**
     * 碰撞检测：
     * 1. 敌机攻击英雄
     * 2. 英雄攻击/撞击敌机
     * 3. 英雄获得补给
     */
    private void crashCheckAction() {
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                // 英雄机被敌机子弹击中
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }
        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircraft) {
                if (enemyAircraft.notValid()) {
                    // 已被其他子弹击毁的敌机，不再检测
                    // 避免多个子弹重复击毁同一敌机的判定
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    // 敌机撞击到英雄机子弹
                    // 敌机损失一定生命值
                    enemyAircraft.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    if (soundEnabled) {
                        new MusicThread("src/videos/bullet_hit.wav").start();
                    }
                    if (enemyAircraft.notValid()) {
                        if (enemyAircraft instanceof AbstractEnemy) {
                            AbstractEnemy enemy = (AbstractEnemy) enemyAircraft;
                            score += enemy.getScore();
                            // 敌机掉落道具
                            props.addAll(enemy.mayDrop());

                            // 如果击败的是Boss，重置标志并提高下一次出现的阈值
                            if (enemy instanceof edu.hitsz.aircraft.BossEnemy) {
                                bossExists = false;
                                bossScoreThreshold += 500; // 增加下一次出现所需分数
                                if (soundEnabled) {
                                    if (bossBgmThread != null) {
                                        bossBgmThread.stopMusic();
                                    }
                                    bgmThread = new MusicThread("src/videos/bgm.wav");
                                    bgmThread.setLoop(true);
                                    bgmThread.start();
                                }
                                System.out.println("Boss has been defeated!");
                            }
                        }
                    }
                }
                // 英雄机与敌机相撞，均损毁
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }
            if (heroAircraft.crash(prop)) {
                if (soundEnabled) {
                    if (prop instanceof edu.hitsz.prop.BombProp) {
                        new MusicThread("src/videos/bomb_explosion.wav").start();
                    } else {
                        new MusicThread("src/videos/get_supply.wav").start();
                    }
                }
                prop.effect(heroAircraft);

                // 若为炸弹，结算本次炸弹累计分数
                if (prop instanceof edu.hitsz.prop.BombProp) {
                    this.score += BombPublisher.drainLastScore();
                }

                prop.vanish();
            }
        }
    }

    /**
     * 后处理：
     * 1. 删除无效的子弹
     * 2. 删除无效的敌机
     * <p>
     * 无效的原因可能是撞击或者飞出边界
     */
    private void postProcessAction() {
        // 敌机子弹：移除并注销炸弹观察者
        for (Iterator<BaseBullet> it = enemyBullets.iterator(); it.hasNext();) {
            BaseBullet b = it.next();
            if (b.notValid()) {
                if (b instanceof BombSubscriber) {
                    BombPublisher.unregister((BombSubscriber) b);
                }
                it.remove();
            }
        }
        // 我方子弹
        heroBullets.removeIf(AbstractFlyingObject::notValid);

        // 敌机：移除并注销炸弹观察者
        for (Iterator<AbstractAircraft> it = enemyAircraft.iterator(); it.hasNext();) {
            AbstractAircraft a = it.next();
            if (a.notValid()) {
                if (a instanceof BombSubscriber) {
                    BombPublisher.unregister((BombSubscriber) a);
                }
                it.remove();
            }
        }
        // 道具
        props.removeIf(AbstractFlyingObject::notValid);
    }

    // ***********************
    // Paint 各部分
    // ***********************

    /**
     * 重写paint方法
     * 通过重复调用paint方法，实现游戏动画
     *
     * @param g Graphics 类，可以理解为一支画笔
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // 绘制背景,图片滚动
        g.drawImage(backgroundImage, 0, this.backGroundTop - Main.WINDOW_HEIGHT, null);
        g.drawImage(backgroundImage, 0, this.backGroundTop, null);
        this.backGroundTop += 1;
        if (this.backGroundTop == Main.WINDOW_HEIGHT) {
            this.backGroundTop = 0;
        }

        // 先绘制子弹，后绘制飞机
        // 这样子弹显示在飞机的下层
        paintImageWithPositionRevised(g, enemyBullets);
        paintImageWithPositionRevised(g, heroBullets);

        paintImageWithPositionRevised(g, enemyAircraft);

        // 绘制道具
        paintImageWithPositionRevised(g, props);

        g.drawImage(ImageManager.HERO_IMAGE, heroAircraft.getLocationX() - ImageManager.HERO_IMAGE.getWidth() / 2,
                heroAircraft.getLocationY() - ImageManager.HERO_IMAGE.getHeight() / 2, null);

        // 绘制得分和生命值
        paintScoreAndLife(g);

    }

    private void paintImageWithPositionRevised(Graphics g, List<? extends AbstractFlyingObject> objects) {
        if (objects.isEmpty()) {
            return;
        }

        for (AbstractFlyingObject object : objects) {
            Bitmap image = object.getImage();
            assert image != null : objects.getClass().getName() + " has no image! ";
            g.drawImage(image, object.getLocationX() - image.getWidth() / 2,
                    object.getLocationY() - image.getHeight() / 2, null);
        }
    }

    private void paintScoreAndLife(Graphics g) {
        int x = 10;
        int y = 25;
        g.setColor(new Color(16711680));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("SCORE:" + this.score, x, y);
        y = y + 20;
        g.drawString("LIFE:" + this.heroAircraft.getHp(), x, y);
    }

}
