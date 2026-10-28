package byeolitgi.model;

/** 난이도 (FR-03). 분류 기준은 policy.DifficultyPolicy 에 있다. */
public enum Difficulty {
    /** 쉬움. 밤하늘 기본 점수 10점. */
    EASY("쉬움"),
    /** 보통. 밤하늘 기본 점수 20점. */
    NORMAL("보통"),
    /** 어려움. 밤하늘 기본 점수 30점. */
    HARD("어려움");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    /** 화면에 표시할 이름. */
    public String label() { return label; }
}
