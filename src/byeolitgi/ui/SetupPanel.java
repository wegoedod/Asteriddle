package byeolitgi.ui;

import byeolitgi.model.Difficulty;
import byeolitgi.model.GameMode;
import byeolitgi.model.StarSystem;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 게임 설정 화면 (UC-01): 체계 → 모드 → (기본 문제면) 난이도. */
final class SetupPanel extends JPanel {

    private final MainFrame frame;
    private final Map<StarSystem, JRadioButton> systems = new EnumMap<>(StarSystem.class);
    private final Map<GameMode, JRadioButton> modes = new EnumMap<>(GameMode.class);
    private final Map<Difficulty, JRadioButton> difficulties = new EnumMap<>(Difficulty.class);
    private final ButtonGroup systemGroup = new ButtonGroup();
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final ButtonGroup difficultyGroup = new ButtonGroup();
    private final JLabel modeNote = new JLabel(" ");

    SetupPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = Theme.title("새 게임 설정", 24);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(16));

        JPanel systemBox = group("1. 별자리 체계");
        for (StarSystem s : StarSystem.values()) {
            JRadioButton b = new JRadioButton(s.label() + "  ·  " + frame.catalog.constellations(s).size() + "개 수록");
            b.addActionListener(e -> updateState());
            systemGroup.add(b);
            systems.put(s, b);
            systemBox.add(b);
        }
        column.add(systemBox);

        JPanel modeBox = group("2. 게임 모드");
        String[] notes = {"별자리 하나씩 풀이 · 점수 없이 연습", "위치·날짜·시간의 밤하늘에서 자유 연결 · 점수 획득"};
        for (GameMode m : GameMode.values()) {
            JRadioButton b = new JRadioButton(m.label() + "  ·  " + notes[m.ordinal()]);
            b.addActionListener(e -> updateState());
            modeGroup.add(b);
            modes.put(m, b);
            modeBox.add(b);
        }
        column.add(modeBox);

        JPanel difficultyBox = group("3. 난이도 (기본 문제 모드)");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (Difficulty d : Difficulty.values()) {
            JRadioButton b = new JRadioButton(d.label());
            difficultyGroup.add(b);
            difficulties.put(d, b);
            row.add(b);
            row.add(Box.createHorizontalStrut(14));
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        difficultyBox.add(row);
        modeNote.setFont(Theme.font(Font.PLAIN, 12));
        modeNote.setForeground(Theme.INFO);
        difficultyBox.add(modeNote);
        column.add(difficultyBox);

        JButton confirm = Theme.button("확인");
        JButton cancel = Theme.button("취소");
        confirm.addActionListener(e -> confirm());
        cancel.addActionListener(e -> frame.showStart());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(confirm);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(Box.createVerticalStrut(8));
        column.add(buttons);
        add(column);
    }

    private static JPanel group(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** 화면에 들어올 때마다 선택을 비운다. */
    void reset() {
        systemGroup.clearSelection();
        modeGroup.clearSelection();
        difficultyGroup.clearSelection();
        updateState();
    }

    private void updateState() {
        StarSystem system = selected(systems);
        GameMode mode = selected(modes);
        boolean basic = mode == GameMode.BASIC;
        for (Map.Entry<Difficulty, JRadioButton> e : difficulties.entrySet()) {
            int count = system == null ? -1 : frame.catalog.constellations(system, e.getKey()).size();
            e.getValue().setText(e.getKey().label() + (count < 0 ? "" : " (" + count + "문제)"));
            e.getValue().setEnabled(basic);
        }
        if (mode == GameMode.NIGHT_SKY) {
            difficultyGroup.clearSelection();
            modeNote.setText("밤하늘 모드는 난이도를 고르지 않습니다. 별자리별 난이도는 점수 계산에 쓰입니다.");
        } else {
            modeNote.setText(" ");
        }
    }

    private void confirm() {
        StarSystem system = selected(systems);
        GameMode mode = selected(modes);
        Difficulty difficulty = selected(difficulties);
        List<String> missing = new ArrayList<>();
        if (system == null) {
            missing.add("별자리 체계");
        }
        if (mode == null) {
            missing.add("게임 모드");
        }
        if (mode == GameMode.BASIC && difficulty == null) {
            missing.add("난이도");
        }
        if (!missing.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join(", ", missing) + "을(를) 선택해 주세요.",
                    "설정 누락", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = "체계: " + system.label() + "\n모드: " + mode.label()
                + (mode == GameMode.BASIC ? "\n난이도: " + difficulty.label() : "")
                + "\n\n이 설정으로 시작할까요?";
        if (frame.hasProgress()) {
            text += "\n\n※ 새 게임을 시작하면 기존에 저장된 진행 상황이 새 게임으로 대체됩니다.";
        }
        if (JOptionPane.showConfirmDialog(this, text, "설정 확인", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        if (mode == GameMode.BASIC) {
            if (!frame.startBasic(system, difficulty)) {
                JOptionPane.showMessageDialog(this,
                        system.label() + "에는 '" + difficulty.label() + "' 난이도 문제 데이터가 없어 시작할 수 없습니다.\n"
                                + "다른 난이도나 체계를 선택해 주세요.",
                        "문제 없음", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            frame.showObservation(system, null, false);
        }
    }

    private static <K> K selected(Map<K, JRadioButton> buttons) {
        for (Map.Entry<K, JRadioButton> e : buttons.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }
}
