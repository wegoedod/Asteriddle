package byeolitgi.model;

/** 완료 상태: 미완료, 정답 완성, 정답 공개 완료 (공통 운영 규칙). */
public enum CompletionState {
    INCOMPLETE("풀이 중"),
    SOLVED("정답 완성"),
    REVEALED("정답 공개 완료");

    private final String label;

    CompletionState(String label) {
        this.label = label;
    }

    public String label() { return label; }
}
