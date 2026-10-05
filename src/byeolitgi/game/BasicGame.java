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

    public Constellation current() {
        return catalog.constellation(system, order.get(index));
    }

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
        state = CompletionState.REVEALED;
        revealed.add(current().id());
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

    public StarSystem system() { return system; }
    public Difficulty difficulty() { return difficulty; }
    public List<String> order() { return Collections.unmodifiableList(order); }
    public int index() { return index; }
    public EdgeBoard board() { return board; }
    public CompletionState state() { return state; }
    public int hintsUsed() { return hintsUsed; }
    public Set<String> solved() { return Collections.unmodifiableSet(solved); }
    public Set<String> revealed() { return Collections.unmodifiableSet(revealed); }
}
