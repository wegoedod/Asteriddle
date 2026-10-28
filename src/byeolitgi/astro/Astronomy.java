package byeolitgi.astro;

import byeolitgi.model.Observation;
import byeolitgi.model.Star;
import byeolitgi.policy.ObservationPolicy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

/**
 * 밤하늘 생성에 필요한 천문 계산 (FR-09, FR-10).
 * 이상적인 관측 환경을 가정하므로 대기 굴절·세차·고유운동은 무시한다 (오차 1° 이내).
 */
public final class Astronomy {

    private Astronomy() {
    }

    /** 지평 좌표: 고도와 방위각(북=0°, 동=90°), 단위 도. */
    public record Horizontal(double altitude, double azimuth) {
    }

    /** 시각을 율리우스일로 바꾼다. */
    public static double julianDay(Instant t) {
        return t.toEpochMilli() / 86_400_000.0 + 2_440_587.5;
    }

    /** 관측 시각의 율리우스일. */
    public static double julianDay(Observation obs) {
        return julianDay(obs.dateTime().toInstant());
    }

    /** 그리니치 평균 항성시(도). */
    public static double gmstDeg(double jd) {
        double d = jd - 2_451_545.0;
        double t = d / 36_525.0;
        return normalize(280.46061837 + 360.98564736629 * d + 0.000387933 * t * t - t * t * t / 38_710_000.0);
    }

    /** 적경·적위(도)를 관측지의 지평 좌표로 바꾼다. */
    public static Horizontal toHorizontal(double raDeg, double decDeg,
                                          double latDeg, double lonDeg, double jd) {
        double lst = gmstDeg(jd) + lonDeg;
        double ha = toRadians(normalize(lst - raDeg));
        double dec = toRadians(decDeg);
        double lat = toRadians(latDeg);
        double sinAlt = sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(ha);
        double alt = asin(Math.max(-1, Math.min(1, sinAlt)));
        double az = atan2(-cos(dec) * sin(ha), sin(dec) * cos(lat) - cos(dec) * sin(lat) * cos(ha));
        return new Horizontal(toDegrees(alt), normalize(toDegrees(az)));
    }

    /** 별 하나의 관측 시각 지평 좌표. */
    public static Horizontal toHorizontal(Star star, Observation obs, double jd) {
        return toHorizontal(star.raDeg(), star.decDeg(), obs.latitude(), obs.longitude(), jd);
    }

    /** 저정밀 태양 위치 (적경, 적위; 도). 정확도 약 0.01°. */
    public static double[] sunEquatorial(double jd) {
        double n = jd - 2_451_545.0;
        double l = normalize(280.460 + 0.9856474 * n);
        double g = toRadians(normalize(357.528 + 0.9856003 * n));
        double lambda = toRadians(l + 1.915 * sin(g) + 0.020 * sin(2 * g));
        double eps = toRadians(23.439 - 0.0000004 * n);
        double ra = toDegrees(atan2(cos(eps) * sin(lambda), cos(lambda)));
        double dec = toDegrees(asin(sin(eps) * sin(lambda)));
        return new double[]{normalize(ra), dec};
    }

    /** 관측 조건에서의 태양 고도(도). */
    public static double sunAltitude(Observation obs) {
        double jd = julianDay(obs);
        double[] sun = sunEquatorial(jd);
        return toHorizontal(sun[0], sun[1], obs.latitude(), obs.longitude(), jd).altitude();
    }

    /** 태양 고도가 야간 기준(ObservationPolicy)보다 낮으면 true. */
    public static boolean isNight(Observation obs) {
        return sunAltitude(obs) < ObservationPolicy.NIGHT_SUN_ALTITUDE_DEG;
    }

    /** 입력 시각과 가장 가까운 야간 시각. 백야 등으로 범위 안에 밤이 없으면 빈 값. */
    public static Optional<Observation> nearestNight(Observation obs) {
        ZonedDateTime t = obs.dateTime();
        int step = ObservationPolicy.NIGHT_SEARCH_STEP_MINUTES;
        int steps = ObservationPolicy.NIGHT_SEARCH_RANGE_HOURS * 60 / step;
        for (int k = 1; k <= steps; k++) {
            Duration d = Duration.ofMinutes((long) k * step);
            Observation later = obs.at(t.plus(d));
            if (isNight(later)) {
                return Optional.of(later);
            }
            Observation earlier = obs.at(t.minus(d));
            if (isNight(earlier)) {
                return Optional.of(earlier);
            }
        }
        return Optional.empty();
    }

    /** 별들의 방향 벡터 평균으로 구한 중심 (적경, 적위; 도). */
    public static double[] centroid(Collection<Star> stars) {
        double x = 0, y = 0, z = 0;
        for (Star s : stars) {
            double ra = toRadians(s.raDeg());
            double dec = toRadians(s.decDeg());
            x += cos(dec) * cos(ra);
            y += cos(dec) * sin(ra);
            z += sin(dec);
        }
        double ra = toDegrees(atan2(y, x));
        double dec = toDegrees(atan2(z, Math.hypot(x, y)));
        return new double[]{normalize(ra), dec};
    }

    /** 두 천구 좌표 사이의 각거리(도). */
    public static double angularDistance(double ra1, double dec1, double ra2, double dec2) {
        double d1 = toRadians(dec1);
        double d2 = toRadians(dec2);
        double c = sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(toRadians(ra1 - ra2));
        return toDegrees(Math.acos(Math.max(-1, Math.min(1, c))));
    }

    /**
     * 접평면(gnomonic) 투영. 하늘을 올려다본 모습처럼 동쪽이 왼쪽, 북쪽이 위가 되도록
     * 화면 좌표계(x 오른쪽, y 아래쪽)로 돌려준다. 중심에서 너무 멀면 null.
     */
    public static double[] gnomonic(double raDeg, double decDeg, double ra0Deg, double dec0Deg) {
        double a = toRadians(raDeg - ra0Deg);
        double d = toRadians(decDeg);
        double d0 = toRadians(dec0Deg);
        double cosc = sin(d0) * sin(d) + cos(d0) * cos(d) * cos(a);
        if (cosc < 0.2) {
            return null;
        }
        double xi = cos(d) * sin(a) / cosc;
        double eta = (cos(d0) * sin(d) - sin(d0) * cos(d) * cos(a)) / cosc;
        return new double[]{-xi, -eta};
    }

    /**
     * 천정 중심 등거리 방위 투영. 지평선이 반지름 1인 원, 북쪽이 위, 동쪽이 왼쪽.
     */
    public static double[] domeProjection(Horizontal h) {
        double r = (90.0 - h.altitude()) / 90.0;
        double az = toRadians(h.azimuth());
        return new double[]{-r * sin(az), -r * cos(az)};
    }

    /** 각도를 0 이상 360 미만으로 맞춘다. */
    public static double normalize(double deg) {
        double r = deg % 360.0;
        return r < 0 ? r + 360.0 : r;
    }
}
