package edu.hitsz.dao;

import java.util.List;

/**
 * 得分数据访问对象接口
 */
public interface ScoreDao {
    /**
     * 添加一条得分记录
     *
     * @param record 得分记录
     */
    void addScore(ScoreRecord record);

    /**
     * 获取所有得分记录
     *
     * @return 得分记录列表
     */
    List<ScoreRecord> getAllScores();

    /**
     * 保存所有得分记录，覆盖现有记录
     *
     * @param scores 得分记录列表
     */
    void saveAll(List<ScoreRecord> scores);
}