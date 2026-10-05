package byeolitgi.save;

import byeolitgi.game.BasicGame;
import byeolitgi.game.GameSession;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.Catalog;
import byeolitgi.model.CompletionState;
import byeolitgi.model.Constellation;
import byeolitgi.model.Difficulty;
import byeolitgi.model.Edge;
import byeolitgi.model.GameMode;
import byeolitgi.model.Observation;
import byeolitgi.model.StarSystem;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * 진행 상황을 로컬 파일(Properties, UTF-8)에 저장하고 복원한다 (FR-18~21).
 * 임시 파일에 먼저 쓴 뒤 교체하므로 저장 도중 실패해도 기존 정상 파일은 보존된다.
 */
public final class SaveManager {

    private static final String VERSION = "1";

    private final Path file;

    public SaveManager(Path file) {
        this.file = file;
    }

    /** 기본 저장 위치: ~/.byeolitgi/save.properties (-Dbyeolitgi.save=경로 로 변경 가능). */
    public static Path defaultPath() {
        String custom = System.getProperty("byeolitgi.save");
        if (custom != null && !custom.isBlank()) {
            return Paths.get(custom);
        }
        return Paths.get(System.getProperty("user.home"), ".byeolitgi", "save.properties");
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    public void save(GameSession session) throws IOException {
        Properties p = encode(session);
        Path dir = file.toAbsolutePath().getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            p.store(w, "byeolitgi save");
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public GameSession load(Catalog catalog) throws SaveException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new SaveException("저장 파일을 읽지 못했습니다: " + e.getMessage(), e);
        }
        try {
            return decode(p, catalog);
        } catch (SaveException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SaveException("저장 파일이 손상되었습니다: " + e.getMessage(), e);
        }
    }

    /** 읽을 수 없는 파일을 옆에 보관해 둔다. 새 게임 저장 시 덮어쓰이지 않게 하기 위함. */
    public void quarantine() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // 보관에 실패해도 새 게임 진행에는 지장이 없다.
        }
    }

    // ----------------------------------------------------------------- encode

    private static Properties encode(GameSession s) {
        Properties p = new Properties();
        p.setProperty("version", VERSION);
        p.setProperty("mode", s.mode().name());
        p.setProperty("system", s.system().name());
        if (s.mode() == GameMode.BASIC) {
            BasicGame b = s.basic();
            p.setProperty("basic.difficulty", b.difficulty().name());
            p.setProperty("basic.order", join(b.order()));
            p.setProperty("basic.index", Integer.toString(b.index()));
            p.setProperty("basic.edges", joinEdges(b.board().edges()));
            p.setProperty("basic.state", b.state().name());
            p.setProperty("basic.hintsUsed", Integer.toString(b.hintsUsed()));
            p.setProperty("basic.solved", join(b.solved()));
            p.setProperty("basic.revealed", join(b.revealed()));
        } else {
            NightSkyGame n = s.night();
            Observation o = n.observation();
            p.setProperty("night.place", o.placeName());
            p.setProperty("night.lat", Double.toString(o.latitude()));
            p.setProperty("night.lon", Double.toString(o.longitude()));
            p.setProperty("night.date", o.date().toString());
            p.setProperty("night.time", o.time().toString());
            p.setProperty("night.zone", o.zone().getId());
            p.setProperty("night.edges", joinEdges(n.board().edges()));
            p.setProperty("night.states", joinMap(n.states()));
            p.setProperty("night.hints", joinMap(n.hints()));
            p.setProperty("night.earned", joinMap(n.earned()));
            p.setProperty("night.score", Integer.toString(n.score()));
        }
        return p;
    }

    private static String join(Collection<String> items) {
        return String.join(",", items);
    }

    private static String joinEdges(Collection<Edge> edges) {
        StringJoiner j = new StringJoiner(",");
        for (Edge e : edges) {
            j.add(e.encode());
        }
        return j.toString();
    }

    private static String joinMap(Map<String, ?> map) {
        StringJoiner j = new StringJoiner(",");
        map.forEach((k, v) -> j.add(k + ":" + (v instanceof Enum<?> e ? e.name() : v)));
        return j.toString();
    }

    // ----------------------------------------------------------------- decode

    private static GameSession decode(Properties p, Catalog catalog) throws SaveException {
        if (!VERSION.equals(p.getProperty("version"))) {
            throw new SaveException("지원하지 않는 저장 형식입니다.");
        }
        GameMode mode = GameMode.valueOf(required(p, "mode"));
        StarSystem system = StarSystem.valueOf(required(p, "system"));
        if (mode == GameMode.BASIC) {
            Difficulty difficulty = Difficulty.valueOf(required(p, "basic.difficulty"));
            List<String> order = constellationIds(catalog, system, required(p, "basic.order"));
            int index = Integer.parseInt(required(p, "basic.index"));
            if (order.isEmpty() || index < 0 || index >= order.size()) {
                throw new SaveException("저장된 문제 위치가 올바르지 않습니다.");
            }
            BasicGame b = BasicGame.restore(catalog, system, difficulty, order, index,
                    edges(catalog, p.getProperty("basic.edges", "")),
                    CompletionState.valueOf(required(p, "basic.state")),
                    Integer.parseInt(required(p, "basic.hintsUsed")),
                    constellationIds(catalog, system, p.getProperty("basic.solved", "")),
                    constellationIds(catalog, system, p.getProperty("basic.revealed", "")));
            return GameSession.of(b);
        }
        Observation o = new Observation(required(p, "night.place"),
                Double.parseDouble(required(p, "night.lat")),
                Double.parseDouble(required(p, "night.lon")),
                LocalDate.parse(required(p, "night.date")),
                LocalTime.parse(required(p, "night.time")),
                ZoneId.of(required(p, "night.zone")));
        Map<String, CompletionState> states = new LinkedHashMap<>();
        splitMap(catalog, system, p.getProperty("night.states", ""))
                .forEach((k, v) -> states.put(k, CompletionState.valueOf(v)));
        Map<String, Integer> hints = new LinkedHashMap<>();
        splitMap(catalog, system, p.getProperty("night.hints", ""))
                .forEach((k, v) -> hints.put(k, Integer.parseInt(v)));
        Map<String, Integer> earned = new LinkedHashMap<>();
        splitMap(catalog, system, p.getProperty("night.earned", ""))
                .forEach((k, v) -> earned.put(k, Integer.parseInt(v)));
        NightSkyGame n = NightSkyGame.restore(catalog, system, o,
                edges(catalog, p.getProperty("night.edges", "")), states, hints, earned);
        // 점수와 완료 기록이 서로 맞지 않으면 중복 득점·기록 소실이 생길 수 있으므로 거부한다 (NFR-03).
        if (!earned.keySet().equals(states.keySet())
                || n.score() != Integer.parseInt(required(p, "night.score"))) {
            throw new SaveException("저장된 점수와 완료 기록이 일치하지 않습니다.");
        }
        return GameSession.of(n);
    }

    private static String required(Properties p, String key) throws SaveException {
        String v = p.getProperty(key);
        if (v == null) {
            throw new SaveException("저장 파일에 '" + key + "' 항목이 없습니다.");
        }
        return v.strip();
    }

    private static List<String> split(String text) {
        List<String> result = new ArrayList<>();
        for (String s : text.split(",")) {
            if (!s.isBlank()) {
                result.add(s.strip());
            }
        }
        return result;
    }

    private static List<String> constellationIds(Catalog catalog, StarSystem system, String text)
            throws SaveException {
        List<String> ids = split(text);
        for (String id : ids) {
            requireConstellation(catalog, system, id);
        }
        return ids;
    }

    private static void requireConstellation(Catalog catalog, StarSystem system, String id)
            throws SaveException {
        Constellation c = catalog.constellation(system, id);
        if (c == null) {
            throw new SaveException("알 수 없는 별자리: " + id);
        }
    }

    private static List<Edge> edges(Catalog catalog, String text) throws SaveException {
        List<Edge> result = new ArrayList<>();
        for (String s : split(text)) {
            Edge e = Edge.decode(s);
            if (!catalog.hasStar(e.a()) || !catalog.hasStar(e.b())) {
                throw new SaveException("알 수 없는 별: " + s);
            }
            result.add(e);
        }
        return result;
    }

    private static Map<String, String> splitMap(Catalog catalog, StarSystem system, String text)
            throws SaveException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String s : split(text)) {
            int i = s.indexOf(':');
            if (i <= 0) {
                throw new SaveException("잘못된 기록 형식: " + s);
            }
            String id = s.substring(0, i);
            requireConstellation(catalog, system, id);
            result.put(id, s.substring(i + 1));
        }
        return result;
    }
}
