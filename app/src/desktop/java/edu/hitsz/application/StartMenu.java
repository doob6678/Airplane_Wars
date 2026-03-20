package edu.hitsz.application;

import edu.hitsz.difficulty.DifficultyFactory;
import edu.hitsz.difficulty.DifficultyTemplate;

import javax.swing.*;

public class StartMenu extends JPanel {

    private final JFrame mainFrame;
    private boolean soundEnabled = true;
    private JPanel panel1;
    private JButton easy;
    private JButton normal;
    private JButton hard;
    private JLabel title;
    private JComboBox<String> soundComboBox;
    private JLabel soundLabel;

    public StartMenu(JFrame frame) {
        this.mainFrame = frame;

        this.add(panel1);

        // 为按钮添加事件监听器
        easy.addActionListener(e -> startGame("Easy"));
        normal.addActionListener(e -> startGame("Normal"));
        hard.addActionListener(e -> startGame("Hard"));

        // 为下拉框添加事件监听器
        soundComboBox.addActionListener(e -> {
            String selected = (String) soundComboBox.getSelectedItem();
            soundEnabled = "开".equals(selected);
            System.out.println("Sound " + (soundEnabled ? "Enabled" : "Disabled"));
        });
    }

    private void startGame(String difficultyName) {
        mainFrame.getContentPane().removeAll();
        DifficultyTemplate difficulty = DifficultyFactory.create(difficultyName);
        Game game = new Game(difficulty, soundEnabled);
        mainFrame.add(game);
        mainFrame.revalidate();
        mainFrame.repaint();
        game.action();
    }
}