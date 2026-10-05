package byeolitgi.model;

/**
 * 별 하나. 좌표는 J2000 기준 적경·적위(단위: 도).
 * 화면 표시, 정답 판정, 힌트, 정답 보기가 모두 같은 id 를 사용한다 (NFR-02).
 */
public record Star(String id, String properName, String designation,
                   double raDeg, double decDeg, double magnitude) {

    /** 정답 공개 시 보여 줄 이름. 고유 이름이 없으면 바이어 표기만 쓴다. */
    public String displayName() {
        return properName == null ? designation : properName + " (" + designation + ")";
    }
}
