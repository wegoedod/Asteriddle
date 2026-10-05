package byeolitgi.policy;

import byeolitgi.astro.Astronomy;
import byeolitgi.model.Difficulty;
import byeolitgi.model.Edge;
import byeolitgi.model.Star;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 난이도 분류 정책 (FR-03). 데이터를 불러올 때 한 번 계산하여 사전 분류한다.
 *
 * <pre>
 * 점수 = 별 수 + 0.5 × 선 수 + 1.5 × 고리 수 + 분기 별 수 + 0.3 × 주변 별 수
 *   고리 수   = 선 수 - 별 수 + 연결 요소 수   (구조 복잡도)
 *   분기 별   = 선이 3개 이상 모이는 별         (구조 복잡도)
 *   주변 별   = 별자리 범위 안에 있는 다른 별   (주변 별과의 구분 난이도)
 * 점수 ≤ 10 쉬움, ≤ 16 보통, 그 외 어려움
 * </pre>
 */
public final class DifficultyPolicy {

    public static final double EASY_MAX = 10.0;
    public static final double NORMAL_MAX = 16.0;

    private static final double LINE_WEIGHT = 0.5;
    private static final double CYCLE_WEIGHT = 1.5;
    private static final double BRANCH_WEIGHT = 1.0;
    private static final double CROWD_WEIGHT = 0.3;
    private static final double CROWD_RADIUS_FACTOR = 1.1;

    private DifficultyPolicy() {
    }

    public static Difficulty classify(Set<Edge> lines, Map<String, Star> stars) {
        double s = score(lines, stars);
        if (s <= EASY_MAX) {
            return Difficulty.EASY;
        }
        return s <= NORMAL_MAX ? Difficulty.NORMAL : Difficulty.HARD;
    }

    public static double score(Set<Edge> lines, Map<String, Star> stars) {
        Map<String, Integer> degree = new HashMap<>();
        for (Edge e : lines) {
            degree.merge(e.a(), 1, Integer::sum);
            degree.merge(e.b(), 1, Integer::sum);
        }
        Set<String> members = degree.keySet();
        int n = members.size();
        int m = lines.size();
        int cycles = m - n + components(members, lines);
        long branches = degree.values().stream().filter(d -> d >= 3).count();
        int crowd = crowding(members, stars);
        return n + LINE_WEIGHT * m + CYCLE_WEIGHT * cycles + BRANCH_WEIGHT * branches + CROWD_WEIGHT * crowd;
    }

    private static int components(Set<String> members, Collection<Edge> lines) {
        Map<String, String> parent = new HashMap<>();
        for (String s : members) {
            parent.put(s, s);
        }
        int count = members.size();
        for (Edge e : lines) {
            String ra = find(parent, e.a());
            String rb = find(parent, e.b());
            if (!ra.equals(rb)) {
                parent.put(ra, rb);
                count--;
            }
        }
        return count;
    }

    private static String find(Map<String, String> parent, String s) {
        while (!parent.get(s).equals(s)) {
            s = parent.get(s);
        }
        return s;
    }

    /** 별자리 중심에서 가장 먼 구성 별까지의 거리 안에 들어오는 다른 별의 수. */
    private static int crowding(Set<String> members, Map<String, Star> stars) {
        Set<Star> own = new HashSet<>();
        for (String id : members) {
            own.add(stars.get(id));
        }
        double[] center = Astronomy.centroid(own);
        double radius = 0;
        for (Star s : own) {
            radius = Math.max(radius, Astronomy.angularDistance(center[0], center[1], s.raDeg(), s.decDeg()));
        }
        radius *= CROWD_RADIUS_FACTOR;
        int count = 0;
        for (Star s : stars.values()) {
            if (!members.contains(s.id())
                    && Astronomy.angularDistance(center[0], center[1], s.raDeg(), s.decDeg()) <= radius) {
                count++;
            }
        }
        return count;
    }
}
