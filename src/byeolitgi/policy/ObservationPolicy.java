package byeolitgi.policy;

/**
 * 밤하늘 관측 정책 (FR-09, FR-10).
 * 보완 항목 "야간 판정"은 항해박명 종료(태양 고도 -12°)를 기준으로 확정한다.
 */
public final class ObservationPolicy {

    /** 태양 고도가 이 값보다 낮으면 야간으로 본다. */
    public static final double NIGHT_SUN_ALTITUDE_DEG = -12.0;

    /** 구성 별이 모두 이 고도보다 높아야 관측 가능한 별자리로 본다. */
    public static final double MIN_STAR_ALTITUDE_DEG = 0.0;

    /** 낮 시각을 야간으로 바꿀 때의 탐색 간격(분). */
    public static final int NIGHT_SEARCH_STEP_MINUTES = 5;

    /** 낮 시각을 야간으로 바꿀 때의 앞뒤 탐색 범위(시간). */
    public static final int NIGHT_SEARCH_RANGE_HOURS = 24;

    private ObservationPolicy() {
    }
}
