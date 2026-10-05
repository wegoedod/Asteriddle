package byeolitgi.ui;

import byeolitgi.astro.Astronomy;
import byeolitgi.astro.Astronomy.Horizontal;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.Catalog;
import byeolitgi.model.Constellation;
import byeolitgi.model.Star;
import byeolitgi.policy.ObservationPolicy;
import byeolitgi.ui.StarCanvas.PlotStar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 하늘 좌표를 캔버스 단위 좌표로 바꾼다. */
final class SkyProjector {

    /** 문제 별자리가 차지하는 범위(단위 좌표 반폭). */
    private static final double FIT = 0.78;
    /** 이 범위 안의 주변 별까지 함께 보여 준다. */
    private static final double FIELD = 1.9;

    private SkyProjector() {
    }

    /** 기본 문제: 별자리 중심 접평면 투영. 주변 별도 함께 표시해 구분 연습이 되게 한다. */
    static List<PlotStar> problem(Catalog catalog, Constellation c) {
        List<Star> members = new ArrayList<>();
        for (String id : c.starIds()) {
            members.add(catalog.star(id));
        }
        double[] center = Astronomy.centroid(members);
        Map<String, double[]> projected = new HashMap<>();
        for (Star s : catalog.stars()) {
            double[] p = Astronomy.gnomonic(s.raDeg(), s.decDeg(), center[0], center[1]);
            if (p != null) {
                projected.put(s.id(), p);
            }
        }
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Star s : members) {
            double[] p = projected.get(s.id());
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;
        double half = Math.max(Math.max(maxX - minX, maxY - minY) / 2, 0.01);
        double scale = FIT / half;

        List<PlotStar> result = new ArrayList<>();
        for (Star s : catalog.stars()) {
            double[] p = projected.get(s.id());
            if (p == null) {
                continue;
            }
            double x = (p[0] - cx) * scale;
            double y = (p[1] - cy) * scale;
            if (Math.abs(x) <= FIELD && Math.abs(y) <= FIELD) {
                result.add(new PlotStar(s.id(), x, y, s.magnitude()));
            }
        }
        return result;
    }

    /** 밤하늘: 지평선 위의 모든 별을 천정 중심으로 투영. */
    static List<PlotStar> sky(Catalog catalog, NightSkyGame game) {
        List<PlotStar> result = new ArrayList<>();
        for (Star s : catalog.stars()) {
            Horizontal h = game.position(s.id());
            if (h.altitude() > ObservationPolicy.MIN_STAR_ALTITUDE_DEG) {
                double[] p = Astronomy.domeProjection(h);
                result.add(new PlotStar(s.id(), p[0], p[1], s.magnitude()));
            }
        }
        return result;
    }
}
