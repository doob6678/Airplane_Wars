package edu.hitsz.dao;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 得分记录类，用于存储玩家得分信息
 * 实现 Serializable 接口以支持对象序列化
 */
public class ScoreRecord implements Serializable, Comparable<ScoreRecord> {
    private static final long serialVersionUID = 1L; // 序列化版本号

    private final long id;
    private final String playerName;
    private final int score;
    private final Date recordTime;

    public ScoreRecord(String playerName, int score) {
        this(0L, playerName, score, new Date());
    }

    public ScoreRecord(long id, String playerName, int score, Date recordTime) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.recordTime = recordTime;
    }

    public long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(recordTime);
    }

    public long getRecordTimeMillis() {
        return recordTime.getTime();
    }

    /**
     * 用于排序，按分数降序排列
     */
    @Override
    public int compareTo(ScoreRecord other) {
        return Integer.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return "Player: " + playerName + ", Score: " + score + ", Time: " + getFormattedTime();
    }
}