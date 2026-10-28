package byeolitgi.policy;

import byeolitgi.model.Difficulty;

/**
 * 밤하늘 모드 점수 정책 (FR-12). 잠정안이므로 값은 이 클래스에서만 관리한다.
 * 기본 문제 모드는 점수를 부여하지 않는다 (FR-07).
 */
public final class ScoringPolicy {

    /** 힌트를 사용한 별자리에 적용하는 배율. */
    public static final double HINT_RATE = 0.5;

    private ScoringPolicy() {
    }

    /** 난이도별 기본 점수: 쉬움 10, 보통 20, 어려움 30. */
    public static int basePoints(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 10;
            case NORMAL -> 20;
            case HARD -> 30;
        };
    }

    /** 별자리 하나의 점수. 힌트를 썼으면 50%, 정답을 공개했으면 0점. */
    public static int points(Difficulty difficulty, boolean hintUsed, boolean revealed) {
        if (revealed) {
            return 0;
        }
        int base = basePoints(difficulty);
        return hintUsed ? (int) Math.round(base * HINT_RATE) : base;
    }
}
