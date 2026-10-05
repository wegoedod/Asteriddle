package byeolitgi.model;

/**
 * 두 별을 잇는 무방향 연결선.
 * 항상 (작은 id, 큰 id) 순서로 정규화하므로 연결 순서와 선의 방향이 판정에 영향을 주지 않는다 (FR-06).
 */
public record Edge(String a, String b) {

    public Edge {
        if (a == null || b == null || a.isBlank() || b.isBlank() || a.equals(b)) {
            throw new IllegalArgumentException("잘못된 연결선: " + a + "-" + b);
        }
        if (a.compareTo(b) > 0) {
            String t = a;
            a = b;
            b = t;
        }
    }

    public boolean touches(String starId) {
        return a.equals(starId) || b.equals(starId);
    }

    public String encode() {
        return a + "-" + b;
    }

    public static Edge decode(String text) {
        int i = text.indexOf('-');
        if (i <= 0 || i == text.length() - 1) {
            throw new IllegalArgumentException("잘못된 연결선 표기: " + text);
        }
        return new Edge(text.substring(0, i).trim(), text.substring(i + 1).trim());
    }
}
