package edu.hitsz.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 炸弹事件发布器（全局）
 */
public final class BombPublisher {
    private static final List<BombSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private static final AtomicInteger lastScore = new AtomicInteger(0);

    private BombPublisher() {
    }

    public static void register(BombSubscriber s) {
        if (s != null) {
            subscribers.add(s);
        }
    }

    public static void unregister(BombSubscriber s) {
        subscribers.remove(s);
    }

    /**
     * 触发炸弹事件，返回累计分数，并缓存到 lastScore
     */
    public static int notifyBomb() {
        int total = 0;
        for (BombSubscriber s : subscribers) {
            try {
                total += s.onBomb();
            } catch (Exception ignored) {
            }
        }
        lastScore.set(total);
        return total;
    }

    /**
     * 取出并清空最近一次炸弹事件累计分数
     */
    public static int drainLastScore() {
        return lastScore.getAndSet(0);
    }
}