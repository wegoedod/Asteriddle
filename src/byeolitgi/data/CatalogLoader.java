package byeolitgi.data;

import byeolitgi.model.Catalog;
import byeolitgi.model.Constellation;
import byeolitgi.model.Edge;
import byeolitgi.model.Star;
import byeolitgi.model.StarSystem;
import byeolitgi.policy.DifficultyPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 클래스패스의 data/*.txt 에서 별 목록과 기준 도안을 읽는다. */
public final class CatalogLoader {

    private CatalogLoader() {
    }

    /** 내장 데이터 파일에서 별 목록과 두 체계의 기준 도안을 읽는다. 난이도는 이때 분류한다. */
    public static Catalog loadDefault() throws IOException {
        Map<String, Star> stars = parseStars(readLines("stars.txt"));
        Map<StarSystem, List<Constellation>> constellations = new EnumMap<>(StarSystem.class);
        for (StarSystem system : StarSystem.values()) {
            constellations.put(system, parseConstellations(system, readLines(system.dataFile()), stars));
        }
        return new Catalog(stars, constellations);
    }

    private static List<String[]> readLines(String file) throws IOException {
        InputStream in = CatalogLoader.class.getResourceAsStream(file);
        if (in == null) {
            throw new IOException("데이터 파일이 없습니다: " + file);
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int no = 0;
            while ((line = r.readLine()) != null) {
                no++;
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split("\\|");
                for (int i = 0; i < cols.length; i++) {
                    cols[i] = cols[i].strip();
                }
                rows.add(prepend(file + ":" + no, cols));
            }
        }
        return rows;
    }

    /** 오류 메시지용 위치 정보를 0번 칸에 붙인다. */
    private static String[] prepend(String where, String[] cols) {
        String[] result = new String[cols.length + 1];
        result[0] = where;
        System.arraycopy(cols, 0, result, 1, cols.length);
        return result;
    }

    private static Map<String, Star> parseStars(List<String[]> rows) throws IOException {
        Map<String, Star> stars = new LinkedHashMap<>();
        for (String[] r : rows) {
            if (r.length != 7) {
                throw new IOException(r[0] + ": 열 개수가 6개가 아닙니다");
            }
            String id = r[1];
            if (id.contains("-") || stars.containsKey(id)) {
                throw new IOException(r[0] + ": 잘못되었거나 중복된 별 id " + id);
            }
            try {
                String name = "-".equals(r[2]) ? null : r[2];
                double ra = sexagesimal(r[4]) * 15.0;
                double dec = sexagesimal(r[5]);
                stars.put(id, new Star(id, name, r[3], ra, dec, Double.parseDouble(r[6])));
            } catch (NumberFormatException e) {
                throw new IOException(r[0] + ": 숫자 형식 오류 (" + e.getMessage() + ")");
            }
        }
        return stars;
    }

    private static List<Constellation> parseConstellations(StarSystem system, List<String[]> rows,
                                                           Map<String, Star> stars) throws IOException {
        List<Constellation> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String[] r : rows) {
            if (r.length != 5) {
                throw new IOException(r[0] + ": 열 개수가 4개가 아닙니다");
            }
            if (!ids.add(r[1])) {
                throw new IOException(r[0] + ": 중복된 별자리 id " + r[1]);
            }
            Set<Edge> lines = new LinkedHashSet<>();
            for (String token : r[4].split("\\s+")) {
                Edge e;
                try {
                    e = Edge.decode(token);
                } catch (IllegalArgumentException ex) {
                    throw new IOException(r[0] + ": " + ex.getMessage());
                }
                if (!stars.containsKey(e.a()) || !stars.containsKey(e.b())) {
                    throw new IOException(r[0] + ": 별 목록에 없는 별 " + token);
                }
                lines.add(e);
            }
            String subtitle = r[3].isEmpty() || "-".equals(r[3]) ? null : r[3];
            result.add(new Constellation(r[1], system, r[2], subtitle, lines,
                    DifficultyPolicy.classify(lines, stars)));
        }
        return result;
    }

    /** "11 03 43.7" 또는 "-00 35 45" 형식을 십진수로 바꾼다. */
    static double sexagesimal(String text) {
        String t = text.strip();
        boolean negative = t.startsWith("-");
        if (negative || t.startsWith("+")) {
            t = t.substring(1);
        }
        String[] p = t.split("\\s+");
        double v = Double.parseDouble(p[0])
                + (p.length > 1 ? Double.parseDouble(p[1]) / 60.0 : 0)
                + (p.length > 2 ? Double.parseDouble(p[2]) / 3600.0 : 0);
        return negative ? -v : v;
    }
}
