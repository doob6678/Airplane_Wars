package edu.hitsz.difficulty;

import edu.hitsz.application.Game;

public abstract class DifficultyTemplate {
    private final String name;

    protected DifficultyTemplate(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    // 模板方法：初始化一次（设置初始参数）
    public final void init(Game game) {
        doInit(game);
    }

    // 模板方法：每个生成周期调用（随时间提升 & 刷怪）
    public final void onCycle(Game game) {
        scaleOverTime(game);
        spawnEnemies(game);
    }

    protected abstract void doInit(Game game);

    protected abstract void scaleOverTime(Game game);

    protected abstract void spawnEnemies(Game game);
}