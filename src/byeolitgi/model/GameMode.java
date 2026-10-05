package byeolitgi.model;

/** 게임 모드 (FR-02). */
public enum GameMode {
    BASIC("기본 문제 모드"),
    NIGHT_SKY("밤하늘 모드");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
