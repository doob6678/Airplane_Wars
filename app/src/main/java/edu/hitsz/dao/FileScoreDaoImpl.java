package edu.hitsz.dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件存储的 ScoreDao 实现
 */
public class FileScoreDaoImpl implements ScoreDao {

    private final String filePath;

    public FileScoreDaoImpl(String filePath) {
        this.filePath = filePath;
        // 确保文件和父目录存在
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    @Override
    public void addScore(ScoreRecord record) {
        List<ScoreRecord> scores = getAllScores();
        scores.add(record);
        saveAll(scores);
    }

    @Override
    public List<ScoreRecord> getAllScores() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // noinspection unchecked
            return (List<ScoreRecord>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // 文件损坏或内容不兼容时返回空列表
            System.err.println("Error reading score file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<ScoreRecord> scores) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            System.err.println("Error saving score file: " + e.getMessage());
        }
    }

    private void saveScores(List<ScoreRecord> scores) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            System.err.println("Error saving score file: " + e.getMessage());
        }
    }
}