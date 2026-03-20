package edu.hitsz.difficulty;

public final class DifficultyFactory {
    private DifficultyFactory() {
    }

    public static DifficultyTemplate create(String name) {
        switch (name) {
            case "Easy":
                return new EasyDifficulty();
            case "Hard":
                return new HardDifficulty();
            case "Normal":
            default:
                return new NormalDifficulty();
        }
    }
}