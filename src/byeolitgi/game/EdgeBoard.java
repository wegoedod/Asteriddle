package byeolitgi.game;

import byeolitgi.model.Edge;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 사용자가 그린 연결선과 편집 기록 (FR-05).
 * 힌트로 추가된 선은 편집 기록에 넣지 않으므로 실행 취소해도 힌트 사용 횟수는 복원되지 않는다 (FR-16).
 */
public final class EdgeBoard {

    private enum Kind { ADD, REMOVE, CLEAR }

    private record Action(Kind kind, Edge edge, List<Edge> snapshot) {
    }

    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Deque<Action> history = new ArrayDeque<>();

    /** 이미 있는 선이면 추가하지 않고 false. */
    public boolean add(Edge e) {
        if (!edges.add(e)) {
            return false;
        }
        history.push(new Action(Kind.ADD, e, null));
        return true;
    }

    /** 없는 선이면 false. */
    public boolean remove(Edge e) {
        if (!edges.remove(e)) {
            return false;
        }
        history.push(new Action(Kind.REMOVE, e, null));
        return true;
    }

    /** 모든 선을 지운다. 지울 선이 없으면 false. */
    public boolean clear() {
        if (edges.isEmpty()) {
            return false;
        }
        history.push(new Action(Kind.CLEAR, null, List.copyOf(edges)));
        edges.clear();
        return true;
    }

    /** 취소할 편집이 없으면 false (UC-02 5a). */
    public boolean undo() {
        Action a = history.poll();
        if (a == null) {
            return false;
        }
        switch (a.kind()) {
            case ADD -> edges.remove(a.edge());
            case REMOVE -> edges.add(a.edge());
            case CLEAR -> edges.addAll(a.snapshot());
        }
        return true;
    }

    /** 힌트가 추가하는 선. 편집 기록에 남기지 않는다. */
    public void addFromHint(Edge e) {
        edges.add(e);
    }

    /**
     * 정답 보기: 오답 선을 지우고 정답 선으로 바꾼다.
     * 실행 취소로 지운 오답 선이 되살아나지 않도록 편집 기록을 비운다.
     */
    public void applyAnswer(Collection<Edge> wrong, Collection<Edge> answer) {
        edges.removeAll(wrong);
        edges.addAll(answer);
        history.clear();
    }

    /** 저장 파일에서 복원. 편집 기록은 저장하지 않으므로 비운다. */
    public void restore(Collection<Edge> saved) {
        edges.clear();
        edges.addAll(saved);
        history.clear();
    }

    /** 이미 그린 선이면 true. */
    public boolean contains(Edge e) {
        return edges.contains(e);
    }

    /** 현재 연결선 (읽기 전용). */
    public Set<Edge> edges() {
        return Collections.unmodifiableSet(edges);
    }
}
