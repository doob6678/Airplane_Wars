package edu.hitsz.application;

import edu.hitsz.dao.FileScoreDaoImpl;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class Score extends JPanel {
    private final ScoreDao scoreDao;
    private final DefaultTableModel tableModel;
    private JPanel Panel;
    private JPanel flowLayout;
    private JLabel rank;
    private JLabel difficultyLabel;
    private JLabel difficulty;
    private JScrollPane scrollPane;
    private JTable scoreTable;
    private JButton deleteButton;

    public Score(String difficultyStr) {
        // 将“排行榜”标题居中
        rank.setHorizontalAlignment(SwingConstants.CENTER);
        // 将“排行榜”设置为红色
        rank.setForeground(Color.RED);

        this.scoreDao = new FileScoreDaoImpl("ranklist_db/scores.dat");
        // 将难度信息合并到 difficultyLabel 中
        this.difficultyLabel.setText("难度：" + difficultyStr);
        // 隐藏原有的 difficulty 标签
        this.difficulty.setVisible(false);

        // 初始化表格模型
        String[] columnNames = {"排名", "玩家", "得分", "记录时间"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 设置表格不可编辑
            }
        };
        scoreTable.setModel(tableModel);
        scoreTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 单选模式
        // 设置单元格内容居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        scoreTable.setDefaultRenderer(Object.class, centerRenderer);

        // 加载并显示数据
        loadScoreData();

        // 删除按钮事件监听
        deleteButton.addActionListener(e -> {
            int selectedRow = scoreTable.getSelectedRow();
            if (selectedRow != -1) {
                int choice = JOptionPane.showConfirmDialog(this, "确定要删除这条记录吗？", "确认删除", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    // 从数据源删除
                    List<ScoreRecord> scores = scoreDao.getAllScores();
                    Collections.sort(scores);
                    scores.remove(selectedRow);
                    scoreDao.saveAll(scores);

                    // 刷新表格
                    loadScoreData();
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择要删除的记录！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });

        this.setLayout(new BorderLayout());
        this.add(Panel);
    }

    private void loadScoreData() {
        List<ScoreRecord> scores = scoreDao.getAllScores();
        Collections.sort(scores); // 按分数降序排序

        // 清空现有数据
        tableModel.setRowCount(0);

        // 填充新数据
        for (int i = 0; i < scores.size(); i++) {
            ScoreRecord r = scores.get(i);
            tableModel.addRow(new Object[]{
                    i + 1,
                    r.getPlayerName(),
                    r.getScore(),
                    r.getFormattedTime()
            });
        }
    }
}
