package edu.hitsz.observer;

/**
 * 炸弹事件观察者
 * 返回因炸弹导致销毁而应结算的分数（否则返回 0）
 */
public interface BombSubscriber {
    int onBomb();
}