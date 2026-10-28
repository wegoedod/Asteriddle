package byeolitgi.ui;

import byeolitgi.astro.Astronomy;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.Observation;
import byeolitgi.model.StarSystem;
import byeolitgi.policy.ObservationPolicy;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** 관측 조건 입력과 밤하늘 생성 (UC-03 1~5). */
final class ObservationPanel extends JPanel {

    private record Place(String name, double lat, double lon) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final Place CUSTOM = new Place("직접 입력", Double.NaN, Double.NaN);
    private static final Place[] PRESETS = {
            new Place("광주", 35.1595, 126.8526),
            new Place("서울", 37.5665, 126.9780),
            new Place("부산", 35.1796, 129.0756),
            new Place("제주", 33.4996, 126.5312),
            CUSTOM,
    };
    private static final String[] ZONES = {"Asia/Seoul", "Asia/Tokyo", "UTC", "Europe/London", "America/New_York"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MainFrame frame;
    private final JLabel systemLabel = new JLabel();
    private final JComboBox<Place> preset = new JComboBox<>(PRESETS);
    private final JTextField place = new JTextField(14);
    private final JTextField lat = new JTextField(10);
    private final JTextField lon = new JTextField(10);
    private final JTextField date = new JTextField(10);
    private final JTextField time = new JTextField(6);
    private final JComboBox<String> zone = new JComboBox<>(ZONES);
    private StarSystem system;
    private boolean fromGame;

    /** 장소 프리셋, 위도·경도, 날짜·시간·시간대 입력란을 배치한다. */
    ObservationPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());
        zone.setEditable(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        form.add(Theme.title("밤하늘 관측 조건", 24), c);
        c.gridy++;
        systemLabel.setForeground(Theme.INFO);
        form.add(systemLabel, c);
        c.gridwidth = 1;

        row(form, c, "장소", preset, "");
        row(form, c, "장소 이름", place, "");
        row(form, c, "위도", lat, "북위 +, 남위 -");
        row(form, c, "경도", lon, "동경 +, 서경 -");
        row(form, c, "날짜", date, "예: 2026-10-05");
        row(form, c, "시각", time, "24시간제, 예: 21:30");
        row(form, c, "시간대", zone, "예: Asia/Seoul, UTC, +09:00");

        JLabel note = new JLabel("<html>날씨·광공해가 없는 이상적인 관측 환경을 가정합니다.<br>"
                + "구성 별이 모두 지평선 위에 있는 별자리만 완성 판정·득점 대상입니다.</html>");
        note.setFont(Theme.font(Font.PLAIN, 12));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        form.add(note, c);

        JButton generate = Theme.button("밤하늘 생성");
        JButton cancel = Theme.button("취소");
        generate.addActionListener(e -> generate());
        cancel.addActionListener(e -> cancel());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(generate);
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        form.add(buttons, c);
        add(form);

        preset.addActionListener(e -> applyPreset());
    }

    private static void row(JPanel form, GridBagConstraints c, String label, Component field, String hint) {
        c.gridx = 0;
        c.gridy++;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 2;
        JLabel h = new JLabel(hint);
        h.setForeground(Color.GRAY);
        form.add(h, c);
    }

    /** 입력란을 채운다. fromGame 이면 취소 시 게임 화면으로 돌아간다. */
    void prepare(StarSystem system, Observation prefill, boolean fromGame) {
        this.system = system;
        this.fromGame = fromGame;
        systemLabel.setText("체계: " + system.label());
        if (prefill == null) {
            preset.setSelectedIndex(0);
            applyPreset();
            date.setText(LocalDate.now().toString());
            time.setText("21:00");
            zone.setSelectedItem(ZoneId.systemDefault().getId());
        } else {
            fill(prefill);
        }
    }

    private void applyPreset() {
        Place p = (Place) preset.getSelectedItem();
        if (p == null || p == CUSTOM) {
            return;
        }
        place.setText(p.name());
        lat.setText(Double.toString(p.lat()));
        lon.setText(Double.toString(p.lon()));
    }

    private void fill(Observation o) {
        preset.setSelectedItem(CUSTOM);
        for (Place p : PRESETS) {
            if (p.name().equals(o.placeName()) && p.lat() == o.latitude() && p.lon() == o.longitude()) {
                preset.setSelectedItem(p);
            }
        }
        place.setText(o.placeName());
        lat.setText(Double.toString(o.latitude()));
        lon.setText(Double.toString(o.longitude()));
        date.setText(o.date().toString());
        time.setText(o.time().format(TIME));
        zone.setSelectedItem(o.zone().getId());
    }

    /** 입력 유효성 검사 (FR-08). 오류가 있으면 수정 사항을 모두 안내하고 null. */
    private Observation readInput() {
        List<String> errors = new ArrayList<>();
        String name = place.getText().strip();
        if (name.isEmpty()) {
            name = "사용자 지정 위치";
        }
        Double la = number(lat.getText(), -90, 90, "위도는 -90 ~ 90 사이의 숫자여야 합니다.", errors);
        Double lo = number(lon.getText(), -180, 180, "경도는 -180 ~ 180 사이의 숫자여야 합니다.", errors);
        LocalDate d = null;
        try {
            d = LocalDate.parse(date.getText().strip());
            if (d.getYear() < 1900 || d.getYear() > 2100) {
                errors.add("날짜는 1900 ~ 2100년 사이여야 합니다.");
            }
        } catch (DateTimeParseException e) {
            errors.add("날짜 형식이 올바르지 않습니다. 예: 2026-10-05");
        }
        LocalTime t = null;
        try {
            t = LocalTime.parse(time.getText().strip(), TIME);
        } catch (DateTimeParseException e) {
            errors.add("시각 형식이 올바르지 않습니다. 예: 21:30");
        }
        ZoneId z = null;
        try {
            Object item = zone.getEditor().getItem();
            z = ZoneId.of(String.valueOf(item).strip());
        } catch (DateTimeException e) {
            errors.add("시간대를 인식할 수 없습니다. 예: Asia/Seoul, UTC, +09:00");
        }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "입력을 확인해 주세요.\n\n· " + String.join("\n· ", errors),
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Observation(name, la, lo, d, t, z);
    }

    private static Double number(String text, double min, double max, String error, List<String> errors) {
        try {
            double v = Double.parseDouble(text.strip());
            if (v >= min && v <= max) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            // 아래에서 오류로 처리
        }
        errors.add(error);
        return null;
    }

    private void generate() {
        Observation obs = readInput();
        if (obs == null) {
            return;
        }
        if (!Astronomy.isNight(obs)) {
            Optional<Observation> night = Astronomy.nearestNight(obs);
            if (night.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "이 위치와 날짜에는 하루 종일 하늘이 충분히 어두워지지 않습니다 (백야).\n날짜나 위치를 바꿔 주세요.",
                        "야간 없음", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Observation n = night.get();
            String msg = String.format(Locale.ROOT,
                    "입력한 시각은 낮 또는 박명 시간입니다 (태양 고도 %.1f°).%n"
                            + "별을 보려면 태양 고도가 %.0f° 아래인 야간이어야 합니다.%n%n"
                            + "가장 가까운 야간 시각 %s %s 로 변경할까요?",
                    Astronomy.sunAltitude(obs), ObservationPolicy.NIGHT_SUN_ALTITUDE_DEG,
                    n.date(), n.time().format(TIME));
            if (JOptionPane.showConfirmDialog(this, msg, "야간 시간으로 변경", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            obs = n;
            fill(obs);
        }
        NightSkyGame game = new NightSkyGame(frame.catalog, system, obs);
        if (game.observable().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "이 조건에서는 구성 별이 모두 지평선 위에 있는 별자리가 없습니다.\n시각·날짜·위치를 바꿔 보세요.",
                    "관측 가능한 별자리 없음", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        frame.startNight(game);
    }

    private void cancel() {
        if (fromGame) {
            frame.resumeGame();
        } else {
            frame.showSetup();
        }
    }
}
