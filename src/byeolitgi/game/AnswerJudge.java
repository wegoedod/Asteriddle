package byeolitgi.game;

import byeolitgi.model.Constellation;
import byeolitgi.model.Edge;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 정답 판정과 힌트 선 선택. 모든 판정은 기준 도안(Constellation.lines)만을 사용한다 (NFR-02).
 */
public final class AnswerJudge {

    private AnswerJudge() {
    }

    /** 기본 문제: 사용자 연결 관계가 기준 도안과 정확히 일치해야 정답 (FR-06). */
    public static boolean exactMatch(Set<Edge> user, Constellation c) {
        return user.equals(c.lines());
    }

    /**
     * 밤하늘: 대상의 구성 별끼리 이은 선만 뽑아 기준 도안과 비교한다.
     * 보완 항목 "밤하늘 판정 세부 기준" 결정:
     * - 구성 별 사이에 기준 도안에 없는 선이 있으면 미완성 (불필요한 선).
     * - 구성 별과 외부 별을 잇는 선은 다른 별자리의 선일 수 있으므로 판정에서 제외한다.
     *   (예: 페가수스의 β Peg–α And 선은 안드로메다 판정에 영향을 주지 않는다.)
     */
    public static boolean completes(Set<Edge> user, Constellation c) {
        return within(user, c.starIds()).equals(c.lines());
    }

    public static Set<Edge> within(Set<Edge> user, Set<String> members) {
        Set<Edge> result = new HashSet<>();
        for (Edge e : user) {
            if (members.contains(e.a()) && members.contains(e.b())) {
                result.add(e);
            }
        }
        return result;
    }

    /** 사용자가 이미 맞게 이은 선의 수 (도움 대상 선택에 사용). */
    public static int correctCount(Set<Edge> user, Constellation c) {
        int n = 0;
        for (Edge e : c.lines()) {
            if (user.contains(e)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 힌트로 추가할 선. 이미 맞게 이은 선에 이어지는 선을 우선한다.
     * 추가할 선이 없으면 null (UC-04: 힌트·차감을 하지 않는다).
     */
    public static Edge hintEdge(Set<Edge> user, Constellation c) {
        Set<String> reached = new LinkedHashSet<>();
        for (Edge e : c.lines()) {
            if (user.contains(e)) {
                reached.add(e.a());
                reached.add(e.b());
            }
        }
        Edge fallback = null;
        for (Edge e : c.lines()) {
            if (user.contains(e)) {
                continue;
            }
            if (reached.contains(e.a()) || reached.contains(e.b())) {
                return e;
            }
            if (fallback == null) {
                fallback = e;
            }
        }
        return fallback;
    }
}
