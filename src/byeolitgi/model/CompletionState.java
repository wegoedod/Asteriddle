package byeolitgi.model;

/** 완료 상태: 미완료, 정답 완성, 정답 공개 완료 (공통 운영 규칙). */
public enum CompletionState {
    /** 아직 풀고 있다. */
    INCOMPLETE("풀이 중"),
    /** 사용자가 정답을 맞혔다. */
    SOLVED("정답 완성"),
    /** 정답 보기로 공개했다. 점수는 0점이다. */
    REVEALED("정답 공개 완료");

    private final String label;

    CompletionState(String label) {
        this.label = label;
    }

    /** 화면에 표시할 이름. */
    public String label() { return label; }
}
