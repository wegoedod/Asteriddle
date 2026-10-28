package byeolitgi.game;

import byeolitgi.astro.Astronomy;
import byeolitgi.astro.Astronomy.Horizontal;
import byeolitgi.model.Catalog;
import byeolitgi.model.CompletionState;
import byeolitgi.model.Constellation;
import byeolitgi.model.Edge;
import byeolitgi.model.Observation;
import byeolitgi.model.Star;
import byeolitgi.model.StarSystem;
import byeolitgi.policy.HelpPolicy;
import byeolitgi.policy.ObservationPolicy;
import byeolitgi.policy.ScoringPolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 밤하늘 모드 한 판 (UC-03). 체계와 관측 조건이 유지되는 동안이 같은 플레이이며,
 * 저장 후 이어 하기도 같은 플레이로 취급한다.
 */
public final class NightSkyGame {

    /** 제출로 새로 완성된 별자리와 받은 점수. */
    public record Award(Constellation constellation, int points, boolean hintUsed) {
    }

    private final Catalog catalog;
    private final StarSystem system;
    private final Observation observation;
    private final EdgeBoard board = new EdgeBoard();
    private final Map<String, CompletionState> states = new LinkedHashMap<>();
    private final Map<String, Integer> hints = new HashMap<>();
    private final Map<String, Integer> earned = new LinkedHashMap<>();

    private final Map<String, Horizontal> positions = new HashMap<>();
    private final List<Constellation> observable = new ArrayList<>();

    /** 관측 조건의 별 위치를 계산하고 관측 가능한 별자리를 정한다 (UC-03). */
    public NightSkyGame(Catalog catalog, StarSystem system, Observation observation) {
        this.catalog = catalog;
        this.system = system;
        this.observation = observation;
        computeSky();
    }

    /** 저장 파일의 값으로 진행 중이던 플레이를 되살린다 (UC-05). */
    public static NightSkyGame restore(Catalog catalog, StarSystem system, Observation observation,
                                       Collection<Edge> edges, Map<String, CompletionState> states,
                                       Map<String, Integer> hints, Map<String, Integer> earned) {
        NightSkyGame g = new NightSkyGame(catalog, system, observation);
        g.board.restore(edges);
        g.states.putAll(states);
        g.hints.putAll(hints);
        g.earned.putAll(earned);
        return g;
    }

    /** 별 위치를 계산하고 구성 별이 모두 지평선 위인 별자리를 정한다 (FR-10). */
    private void computeSky() {
        double jd = Astronomy.julianDay(observation);
        for (Star s : catalog.stars()) {
            positions.put(s.id(), Astronomy.toHorizontal(s, observation, jd));
        }
        for (Constellation c : catalog.constellations(system)) {
            boolean allUp = true;
            for (String id : c.starIds()) {
                if (positions.get(id).altitude() <= ObservationPolicy.MIN_STAR_ALTITUDE_DEG) {
                    allUp = false;
                    break;
                }
            }
            if (allUp) {
                observable.add(c);
            }
        }
    }

    /** 제출: 미완료 대상마다 기준 도안과 비교해 새로 완성된 것만 득점한다 (FR-11~13). */
    public List<Award> submit() {
        List<Award> awards = new ArrayList<>();
        for (Constellation c : observable) {
            if (state(c) != CompletionState.INCOMPLETE || !AnswerJudge.completes(board.edges(), c)) {
                continue;
            }
            boolean hinted = hintsUsed(c) > 0;
            int points = ScoringPolicy.points(c.difficulty(), hinted, false);
            states.put(c.id(), CompletionState.SOLVED);
            earned.put(c.id(), points);
            awards.add(new Award(c, points, hinted));
        }
        return awards;
    }

    /**
     * 힌트 (FR-14~16). 선택 중인 별이 미완료 대상의 구성 별이면 그 대상을,
     * 아니면 사용자가 가장 많이 맞게 이은 대상을 고른다. 이름은 공개하지 않는다.
     */
    public HelpResult hint(String selectedStar) {
        List<Constellation> pool = narrow(helpCandidates(), selectedStar);
        if (pool.isEmpty()) {
            return HelpResult.of(HelpResult.Status.NO_TARGET);
        }
        List<Constellation> available = new ArrayList<>();
        for (Constellation c : pool) {
            if (hintsUsed(c) < HelpPolicy.HINTS_PER_TARGET) {
                available.add(c);
            }
        }
        if (available.isEmpty()) {
            return HelpResult.of(HelpResult.Status.ALREADY_USED);
        }
        available.removeIf(c -> AnswerJudge.hintEdge(board.edges(), c) == null);
        if (available.isEmpty()) {
            return HelpResult.of(HelpResult.Status.NOTHING_TO_ADD);
        }
        Constellation target = mostProgressed(available);
        Edge e = AnswerJudge.hintEdge(board.edges(), target);
        board.addFromHint(e);
        hints.merge(target.id(), 1, Integer::sum);
        return new HelpResult(HelpResult.Status.ADDED, target, e);
    }

    /** 정답 보기 대상. 없으면 null. 공개 전에 사용자 확인을 받기 위해 대상만 고른다. */
    public Constellation revealTarget(String selectedStar) {
        List<Constellation> pool = narrow(helpCandidates(), selectedStar);
        return pool.isEmpty() ? null : mostProgressed(pool);
    }

    /** 정답 공개 완료로 전환하고 이후 득점 불가로 기록한다 (FR-17). */
    public boolean reveal(Constellation c) {
        if (!observable.contains(c) || state(c) != CompletionState.INCOMPLETE) {
            return false;
        }
        board.applyAnswer(wrongLinesAround(c), c.lines());
        states.put(c.id(), CompletionState.REVEALED);
        earned.put(c.id(), 0);
        return true;
    }

    /**
     * 공개 대상의 구성 별에 닿은 선 중 오답인 선.
     * 같은 별을 공유하는 다른 별자리의 기준 선(예: β Peg–α And)은 오답으로 보지 않는다.
     */
    private List<Edge> wrongLinesAround(Constellation c) {
        List<Edge> wrong = new ArrayList<>();
        for (Edge e : board.edges()) {
            if (!(c.contains(e.a()) || c.contains(e.b())) || c.lines().contains(e)) {
                continue;
            }
            boolean otherFigure = false;
            for (Constellation other : catalog.constellations(system)) {
                if (other != c && other.lines().contains(e)) {
                    otherFigure = true;
                    break;
                }
            }
            if (!otherFigure) {
                wrong.add(e);
            }
        }
        return wrong;
    }

    private List<Constellation> helpCandidates() {
        List<Constellation> result = new ArrayList<>();
        for (Constellation c : observable) {
            if (state(c) == CompletionState.INCOMPLETE) {
                result.add(c);
            }
        }
        return result;
    }

    private static List<Constellation> narrow(List<Constellation> candidates, String selectedStar) {
        if (selectedStar == null) {
            return candidates;
        }
        List<Constellation> near = new ArrayList<>();
        for (Constellation c : candidates) {
            if (c.contains(selectedStar)) {
                near.add(c);
            }
        }
        return near.isEmpty() ? candidates : near;
    }

    private Constellation mostProgressed(List<Constellation> pool) {
        Constellation best = pool.get(0);
        int bestCount = AnswerJudge.correctCount(board.edges(), best);
        for (Constellation c : pool) {
            int n = AnswerJudge.correctCount(board.edges(), c);
            if (n > bestCount) {
                best = c;
                bestCount = n;
            }
        }
        return best;
    }

    /** 별자리의 완료 상태. */
    public CompletionState state(Constellation c) {
        return states.getOrDefault(c.id(), CompletionState.INCOMPLETE);
    }

    /** 별자리에 사용한 힌트 수. */
    public int hintsUsed(Constellation c) {
        return hints.getOrDefault(c.id(), 0);
    }

    /** 지금까지 얻은 점수 합계. */
    public int score() {
        int sum = 0;
        for (int p : earned.values()) {
            sum += p;
        }
        return sum;
    }

    /** 관측 가능한 별자리 중 해당 상태인 것의 수. */
    public int count(CompletionState s) {
        int n = 0;
        for (Constellation c : observable) {
            if (state(c) == s) {
                n++;
            }
        }
        return n;
    }

    /** 관측 시각의 별 지평 좌표. */
    public Horizontal position(String starId) {
        return positions.get(starId);
    }

    /** 선택한 체계. */
    public StarSystem system() { return system; }
    /** 관측 조건. */
    public Observation observation() { return observation; }
    /** 사용자가 그린 연결선. */
    public EdgeBoard board() { return board; }
    /** 구성 별이 모두 지평선 위에 있는 별자리. */
    public List<Constellation> observable() { return Collections.unmodifiableList(observable); }
    /** 완료 상태가 바뀐 별자리 (id → 상태). */
    public Map<String, CompletionState> states() { return Collections.unmodifiableMap(states); }
    /** 힌트를 사용한 별자리 (id → 횟수). */
    public Map<String, Integer> hints() { return Collections.unmodifiableMap(hints); }
    /** 완료한 별자리의 점수 (id → 점수). */
    public Map<String, Integer> earned() { return Collections.unmodifiableMap(earned); }
}
