package byeolitgi.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 밤하늘 모드의 관측 조건: 위치·날짜·시간·시간대 (FR-08). */
public record Observation(String placeName, double latitude, double longitude,
                          LocalDate date, LocalTime time, ZoneId zone) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ZonedDateTime dateTime() {
        return ZonedDateTime.of(date, time, zone);
    }

    /** 같은 장소에서 시각만 바꾼 관측 조건. */
    public Observation at(ZonedDateTime t) {
        ZonedDateTime z = t.withZoneSameInstant(zone);
        return new Observation(placeName, latitude, longitude, z.toLocalDate(),
                z.toLocalTime().withSecond(0).withNano(0), zone);
    }

    public String describe() {
        return String.format(Locale.ROOT, "%s (%.2f°%s, %.2f°%s) · %s %s",
                placeName,
                Math.abs(latitude), latitude >= 0 ? "N" : "S",
                Math.abs(longitude), longitude >= 0 ? "E" : "W",
                dateTime().format(FORMAT), zone.getId());
    }
}
