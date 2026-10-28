package byeolitgi.game;

import byeolitgi.model.Catalog;
import byeolitgi.model.CompletionState;
import byeolitgi.model.Constellation;
import byeolitgi.model.Difficulty;
import byeolitgi.model.Edge;
import byeolitgi.model.StarSystem;
import byeolitgi.policy.HelpPolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 기본 문제 모드 (UC-02). 점수 없이 연습하는 학습 모드다. */
public final class BasicGame {

    private final Catalog catalog;
    private final StarSystem system;
    private final Difficulty difficulty;
    private final List<String> order;
    private int index;
    private final EdgeBoard board = new EdgeBoard();
    private CompletionState state = CompletionState.INCOMPLETE;
    private int hintsUsed;
    private final Set<String> solved = new LinkedHashSet<>();
    private final Set<String> revealed = new LinkedHashSet<>();

    private BasicGame(Catalog catalog, StarSystem system, Difficulty difficulty, List<String> order, int index) {
        this.catalog = catalog;
        this.system = system;
        this.difficulty = difficulty;
        this.order = new ArrayList<>(order);
        this.index = index;
    }

    /** 선택한 체계·난이도에 문제가 없으면 null (UC-01 7a). */
    public static BasicGame start(Catalog catalog, StarSystem system, Difficulty difficulty, Random random) {
        List<String> ids = new ArrayList<>();
        for (Constellation c : catalog.constellations(system, difficulty)) {
            ids.add(c.id());
        }
        if (ids.isEmpty()) {
            return null;
        }
        Collections.shuffle(ids, random);
        return new BasicGame(catalog, system, difficulty, ids, 0);
    }

    /** 저장 파일의 값으로 진행 중이던 게임을 되살린다 (UC-05). */
    public static BasicGame restore(Catalog catalog, StarSystem system, Difficulty difficulty,
                                    List<String> order, int index, Collection<Edge> edges,
                                    CompletionState state, int hintsUsed,
                                    Collection<String> solved, Collection<String> revealed) {
        BasicGame g = new BasicGame(catalog, system, difficulty, order, index);
        g.board.restore(edges);
        g.state = state;
        g.hintsUsed = hintsUsed;
        g.solved.addAll(solved);
        g.revealed.addAll(revealed);
        return g;
    }

    /** 지금 풀고 있는 문제의 별자리. */
    public Constellation current() {
        return catalog.constellation(system, order.get(index));
    }

    /** 현재 문제가 풀이 중이면 true. 정답 완성·공개 후에는 선을 편집할 수 없다. */
    public boolean editable() {
        return state == CompletionState.INCOMPLETE;
    }

    /** 제출 (FR-06, FR-07). 오답이면 연결 상태를 그대로 둔다. */
    public boolean submit() {
        if (!editable()) {
            return false;
        }
        if (AnswerJudge.exactMatch(board.edges(), current())) {
            state = CompletionState.SOLVED;
            solved.add(current().id());
            return true;
        }
        return false;
    }

    /** 힌트 (FR-14~16). 현재 문제의 기준 선 하나를 추가한다. 문제당 1회. */
    public HelpResult hint() {
        if (!editable()) {
            return HelpResult.of(HelpResult.Status.FINISHED);
        }
        if (hintsUsed >= HelpPolicy.HINTS_PER_TARGET) {
            return HelpResult.of(HelpResult.Status.ALREADY_USED);
        }
        Constellation c = current();
        Edge e = AnswerJudge.hintEdge(board.edges(), c);
        if (e == null) {
            return HelpResult.of(HelpResult.Status.NOTHING_TO_ADD);
        }
        board.addFromHint(e);
        hintsUsed++;
        return new HelpResult(HelpResult.Status.ADDED, c, e);
    }

    /** 정답 보기 (FR-17). */
    public boolean reveal() {
        if (!editable()) {
            return false;
        }
        Constellation c = current();
        List<Edge> wrong = new ArrayList<>(board.edges());
        wrong.removeAll(c.lines());
        board.applyAnswer(wrong, c.lines());
        state = CompletionState.REVEALED;
        revealed.add(c.id());
        return true;
    }

    /** 다음 문제. 한 바퀴를 다 돌면 순서를 다시 섞는다. */
    public void next(Random random) {
        String last = order.get(index);
        index++;
        if (index >= order.size()) {
            Collections.shuffle(order, random);
            if (order.size() > 1 && order.get(0).equals(last)) {
                Collections.swap(order, 0, 1);
            }
            index = 0;
        }
        board.restore(List.of());
        state = CompletionState.INCOMPLETE;
        hintsUsed = 0;
    }

    /** 선택한 체계. */
    public StarSystem system() { return system; }
    /** 선택한 난이도. */
    public Difficulty difficulty() { return difficulty; }
    /** 섞은 문제 순서 (별자리 id). */
    public List<String> order() { return Collections.unmodifiableList(order); }
    /** order 에서 현재 문제의 위치. */
    public int index() { return index; }
    /** 사용자가 그린 연결선. */
    public EdgeBoard board() { return board; }
    /** 현재 문제의 완료 상태. */
    public CompletionState state() { return state; }
    /** 현재 문제에서 사용한 힌트 수. */
    public int hintsUsed() { return hintsUsed; }
    /** 지금까지 맞힌 별자리 id. */
    public Set<String> solved() { return Collections.unmodifiableSet(solved); }
    /** 지금까지 정답을 공개한 별자리 id. */
    public Set<String> revealed() { return Collections.unmodifiableSet(revealed); }
}
