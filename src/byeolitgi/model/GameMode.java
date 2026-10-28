package byeolitgi.model;

/** 게임 모드 (FR-02). */
public enum GameMode {
    /** 점수 없이 한 문제씩 푸는 연습 모드. */
    BASIC("기본 문제 모드"),
    /** 관측 조건의 하늘에서 별자리를 찾아 점수를 얻는 모드. */
    NIGHT_SKY("밤하늘 모드");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    /** 화면에 표시할 이름. */
    public String label() { return label; }
}
