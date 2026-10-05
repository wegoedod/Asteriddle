package byeolitgi;

import byeolitgi.astro.Astronomy;
import byeolitgi.data.CatalogLoader;
import byeolitgi.game.BasicGame;
import byeolitgi.game.GameSession;
import byeolitgi.game.HelpResult;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.Catalog;
import byeolitgi.model.CompletionState;
import byeolitgi.model.Constellation;
import byeolitgi.model.Difficulty;
import byeolitgi.model.Edge;
import byeolitgi.model.Observation;
import byeolitgi.model.StarSystem;
import byeolitgi.policy.DifficultyPolicy;
import byeolitgi.save.SaveManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;

/** UI 없이 핵심 규칙을 확인하는 간단한 점검 프로그램. 실패하면 예외로 종료한다. */
public final class LogicCheck {

    private static int passed;

    public static void main(String[] args) throws Exception {
        Catalog catalog = CatalogLoader.loadDefault();

        System.out.println("== 난이도 분류 ==");
        for (StarSystem s : StarSystem.values()) {
            for (Constellation c : catalog.constellations(s)) {
                System.out.printf("  %-7s %-8s %-10s 별 %2d 선 %2d 점수 %5.1f%n", s, c.id(), c.difficulty().label(),
                        c.starIds().size(), c.lines().size(),
                        DifficultyPolicy.score(c.lines(), starMap(catalog)));
            }
        }

        // 선의 방향·순서는 판정에 영향이 없어야 한다 (FR-06)
        check(new Edge("a", "b").equals(new Edge("b", "a")), "Edge 정규화");

        // 기본 문제: 오답 → 연결 유지, 정답 → 완료
        BasicGame b = BasicGame.start(catalog, StarSystem.IAU, Difficulty.EASY, new Random(1));
        Constellation c = b.current();
        List<Edge> answer = List.copyOf(c.lines());
        for (int i = 0; i < answer.size() - 1; i++) {
            b.board().add(answer.get(i));
        }
        check(!b.submit() && b.board().edges().size() == answer.size() - 1, "오답이면 연결 유지");
        HelpResult h = b.hint();
        check(h.status() == HelpResult.Status.ADDED && b.board().contains(answer.get(answer.size() - 1)),
                "힌트가 남은 선을 추가");
        b.board().undo();
        b.board().clear();
        check(b.hint().status() == HelpResult.Status.ALREADY_USED, "삭제·실행 취소·초기화해도 힌트 복원 안 됨");
        for (Edge e : answer) {
            b.board().add(new Edge(e.b(), e.a()));
        }
        check(b.submit() && b.state() == CompletionState.SOLVED, "역방향으로 이어도 정답");

        // 천문 계산: 광주 2026-01-15 21:00 오리온은 관측 가능, 정오는 낮
        Observation winter = new Observation("광주", 35.16, 126.85, LocalDate.of(2026, 1, 15),
                LocalTime.of(21, 0), ZoneId.of("Asia/Seoul"));
        check(Astronomy.isNight(winter), "겨울 21시는 밤");
        check(!Astronomy.isNight(winter.at(winter.dateTime().withHour(12))), "정오는 낮");
        check(Astronomy.nearestNight(winter.at(winter.dateTime().withHour(12))).isPresent(), "가장 가까운 야간 찾기");
        NightSkyGame n = new NightSkyGame(catalog, StarSystem.IAU, winter);
        List<String> visible = n.observable().stream().map(Constellation::id).toList();
        System.out.println("== 겨울 밤 관측 가능: " + visible);
        check(visible.contains("Ori") && !visible.contains("Sco"), "겨울 밤에는 오리온 O, 전갈 X");

        // 밤하늘 득점: 힌트 50%, 같은 플레이 1회만
        Constellation ori = catalog.constellation(StarSystem.IAU, "Ori");
        n.board().add(new Edge("alf_Ori", "bet_Ori")); // 불필요한 선
        n.board().add(new Edge("alf_Ori", "lam_Ori"));
        HelpResult nh = n.hint("alf_Ori");
        check(nh.status() == HelpResult.Status.ADDED && nh.target() == ori, "선택한 별 기준으로 힌트 대상 선택");
        for (Edge e : ori.lines()) {
            n.board().add(e);
        }
        check(n.submit().isEmpty(), "구성 별 사이 불필요한 선이 있으면 미완성");
        n.board().remove(new Edge("alf_Ori", "bet_Ori"));
        n.board().add(new Edge("alf_Ori", "alf_Tau")); // 외부 별로 이어진 선은 판정 제외
        List<NightSkyGame.Award> awards = n.submit();
        check(awards.size() == 1 && awards.get(0).points() == 15, "어려움 30점 × 힌트 50% = 15점");
        check(n.submit().isEmpty() && n.score() == 15, "같은 플레이에서 중복 득점 없음");
        Constellation target = n.revealTarget(null);
        check(target != null && n.reveal(target) && n.score() == 15, "정답 보기는 0점");

        // 저장·불러오기 왕복
        Path tmp = Files.createTempFile("byeolitgi", ".properties");
        SaveManager sm = new SaveManager(tmp);
        sm.save(GameSession.of(n));
        GameSession loaded = sm.load(catalog);
        check(loaded.night().score() == 15
                && loaded.night().board().edges().equals(n.board().edges())
                && loaded.night().state(ori) == CompletionState.SOLVED
                && loaded.night().hintsUsed(ori) == 1, "밤하늘 저장·복원");
        sm.save(GameSession.of(b));
        GameSession lb = sm.load(catalog);
        check(lb.basic().current().id().equals(b.current().id()) && lb.basic().state() == CompletionState.SOLVED,
                "기본 문제 저장·복원");
        Files.writeString(tmp, "version=1\nmode=NIGHT_SKY\nsystem=IAU\n");
        boolean failed = false;
        try {
            sm.load(catalog);
        } catch (byeolitgi.save.SaveException e) {
            failed = true;
        }
        check(failed, "손상 파일 감지");
        Files.deleteIfExists(tmp);

        System.out.println("모든 점검 통과: " + passed + "개");
    }

    private static java.util.Map<String, byeolitgi.model.Star> starMap(Catalog catalog) {
        java.util.Map<String, byeolitgi.model.Star> m = new java.util.HashMap<>();
        catalog.stars().forEach(s -> m.put(s.id(), s));
        return m;
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError("실패: " + what);
        }
        passed++;
        System.out.println("  [OK] " + what);
    }
}
