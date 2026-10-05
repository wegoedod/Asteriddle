package byeolitgi.model;

/** 난이도 (FR-03). 분류 기준은 policy.DifficultyPolicy 에 있다. */
public enum Difficulty {
    EASY("쉬움"),
    NORMAL("보통"),
    HARD("어려움");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
