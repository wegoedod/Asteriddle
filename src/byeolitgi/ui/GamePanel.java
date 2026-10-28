package byeolitgi.ui;

import byeolitgi.game.BasicGame;
import byeolitgi.game.GameSession;
import byeolitgi.game.HelpResult;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.CompletionState;
import byeolitgi.model.Constellation;
import byeolitgi.model.Edge;
import byeolitgi.model.GameMode;
import byeolitgi.policy.HelpPolicy;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 문제 풀이·밤하늘 탐색 화면 (UC-02, UC-03, UC-04). */
final class GamePanel extends JPanel implements StarCanvas.Listener {

    private final MainFrame frame;
    private final StarCanvas canvas = new StarCanvas(this);
    private final JLabel heading = Theme.title(" ", 18);
    private final JLabel info = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final JTextArea records = new JTextArea();

    private final JButton undo = Theme.button("실행 취소");
    private final JButton reset = Theme.button("초기화");
    private final JButton submit = Theme.button("제출");
    private final JButton hint = Theme.button("힌트");
    private final JButton reveal = Theme.button("정답 보기");
    private final JButton next = Theme.button("다음 문제");
    private final JButton conditions = Theme.button("관측 조건 변경");

    private GameSession session;
    private String selected;
    private Edge lastHint;

    /** 캔버스와 사이드바(정보·버튼·기록)를 배치한다. */
    GamePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(sidebar(), BorderLayout.EAST);

        undo.addActionListener(e -> undo());
        reset.addActionListener(e -> clearLines());
        submit.addActionListener(e -> submit());
        hint.addActionListener(e -> hint());
        reveal.addActionListener(e -> reveal());
        next.addActionListener(e -> nextProblem());
        conditions.addActionListener(e -> changeConditions());

        bindKey("ctrl Z", "undo", this::undo);
        bindKey("ESCAPE", "deselect", this::emptyClicked);
    }

    private JComponent sidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        side.setPreferredSize(new Dimension(320, 0));

        info.setVerticalAlignment(JLabel.TOP);
        status.setFont(Theme.font(Font.BOLD, 13));
        records.setEditable(false);
        records.setLineWrap(true);
        records.setWrapStyleWord(true);
        records.setFont(Theme.font(Font.PLAIN, 12));

        JPanel edit = new JPanel(new GridLayout(0, 2, 6, 6));
        edit.add(undo);
        edit.add(reset);
        edit.add(hint);
        edit.add(reveal);
        submit.setFont(Theme.font(Font.BOLD, 15));

        JPanel nav = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton view = Theme.button("보기 초기화");
        JButton menu = Theme.button("메인 메뉴");
        view.addActionListener(e -> canvas.resetView());
        menu.addActionListener(e -> frame.showStart());
        nav.add(next);
        nav.add(conditions);
        nav.add(view);
        nav.add(menu);

        JLabel help = new JLabel("<html><b>조작</b><br>"
                + "· 별 클릭 → 다른 별 클릭: 선 연결 (이어서 계속 연결)<br>"
                + "· 선 위에서 오른쪽 클릭: 선 삭제<br>"
                + "· 빈 곳 클릭 / Esc: 선택 해제 · Ctrl+Z: 실행 취소<br>"
                + "· 드래그: 이동 · 마우스 휠: 확대/축소</html>");
        help.setFont(Theme.font(Font.PLAIN, 11));

        JScrollPane scroll = new JScrollPane(records);
        scroll.setBorder(BorderFactory.createTitledBorder("기록"));
        scroll.setPreferredSize(new Dimension(290, 160));

        for (JComponent c : new JComponent[]{heading, info, status, submit, edit, nav, scroll, help}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        edit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        side.add(heading);
        side.add(Box.createVerticalStrut(8));
        side.add(info);
        side.add(Box.createVerticalStrut(10));
        side.add(status);
        side.add(Box.createVerticalStrut(10));
        side.add(submit);
        side.add(Box.createVerticalStrut(8));
        side.add(edit);
        side.add(Box.createVerticalStrut(14));
        side.add(nav);
        side.add(Box.createVerticalStrut(14));
        side.add(scroll);
        side.add(Box.createVerticalStrut(10));
        side.add(help);
        return side;
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------------ 화면 갱신

    /** 세션을 화면에 연다. 모드에 맞게 버튼과 별 배치를 바꾼다. */
    void open(GameSession s) {
        session = s;
        selected = null;
        lastHint = null;
        boolean basic = s.mode() == GameMode.BASIC;
        next.setVisible(basic);
        conditions.setVisible(!basic);
        heading.setText(s.mode().label());
        loadStars();
        canvas.resetView();
        refresh();
        status(basic ? "별의 배치를 보고 별자리를 유추해 이어 보세요." : "밤하늘에서 별자리를 찾아 이어 보세요.", Theme.INFO);
    }

    private void loadStars() {
        if (session.mode() == GameMode.BASIC) {
            canvas.setStars(SkyProjector.problem(frame.catalog, session.basic().current()), false);
        } else {
            canvas.setStars(SkyProjector.sky(frame.catalog, session.night()), true);
        }
    }

    /** 상태 메시지를 바꾼다. */
    void status(String text, Color color) {
        status.setForeground(color);
        status.setText("<html>" + text + "</html>");
    }

    private void refresh() {
        if (session == null) {
            return;
        }
        canvas.setUserEdges(session.board().edges());
        canvas.setSelected(selected);
        canvas.setHintEdge(lastHint);
        if (session.mode() == GameMode.BASIC) {
            refreshBasic(session.basic());
        } else {
            refreshNight(session.night());
        }
    }

    private void refreshBasic(BasicGame b) {
        Constellation c = b.current();
        List<StarCanvas.Overlay> overlays = new ArrayList<>();
        Map<String, String> labels = new HashMap<>();
        if (b.state() != CompletionState.INCOMPLETE) {
            boolean solved = b.state() == CompletionState.SOLVED;
            overlays.add(new StarCanvas.Overlay(c.lines(), solved ? Theme.SOLVED_LINE : Theme.REVEALED_LINE,
                    c.fullName()));
            addStarLabels(c, labels);
        }
        canvas.setOverlays(overlays);
        canvas.setStarLabels(labels);
        canvas.setEditable(b.editable());

        info.setText("<html>체계: " + b.system().label()
                + "<br>난이도: " + b.difficulty().label()
                + "<br>문제: " + (b.index() + 1) + " / " + b.order().size()
                + "<br>상태: " + b.state().label()
                + "<br>힌트: " + b.hintsUsed() + " / " + HelpPolicy.HINTS_PER_TARGET
                + "<br>연결한 선: " + b.board().edges().size() + "개</html>");

        StringBuilder r = new StringBuilder();
        r.append("정답을 맞힌 별자리 (").append(b.solved().size()).append(")\n");
        for (String id : b.solved()) {
            r.append("  · ").append(frame.catalog.constellation(b.system(), id).fullName()).append('\n');
        }
        r.append("\n정답을 공개한 별자리 (").append(b.revealed().size()).append(")\n");
        for (String id : b.revealed()) {
            r.append("  · ").append(frame.catalog.constellation(b.system(), id).fullName()).append('\n');
        }
        records.setText(r.toString());

        boolean open = b.editable();
        for (JButton button : new JButton[]{undo, reset, submit, hint, reveal}) {
            button.setEnabled(open);
        }
    }

    private void refreshNight(NightSkyGame n) {
        List<StarCanvas.Overlay> overlays = new ArrayList<>();
        Map<String, String> labels = new HashMap<>();
        StringBuilder r = new StringBuilder();
        for (Constellation c : n.observable()) {
            CompletionState s = n.state(c);
            if (s == CompletionState.INCOMPLETE) {
                continue;
            }
            boolean solved = s == CompletionState.SOLVED;
            overlays.add(new StarCanvas.Overlay(c.lines(), solved ? Theme.SOLVED_LINE : Theme.REVEALED_LINE,
                    c.name()));
            addStarLabels(c, labels);
            int points = n.earned().getOrDefault(c.id(), 0);
            r.append(solved ? "[완성] " : "[공개] ").append(c.fullName())
                    .append(" · ").append(c.difficulty().label())
                    .append(" · ").append(points).append("점")
                    .append(solved && n.hintsUsed(c) > 0 ? " (힌트 사용)" : "")
                    .append('\n');
        }
        canvas.setOverlays(overlays);
        canvas.setStarLabels(labels);
        canvas.setEditable(true);
        records.setText(r.length() == 0 ? "아직 완성한 별자리가 없습니다." : r.toString());

        int total = n.observable().size();
        int solved = n.count(CompletionState.SOLVED);
        int revealed = n.count(CompletionState.REVEALED);
        info.setText("<html>체계: " + n.system().label()
                + "<br>관측: " + n.observation().placeName()
                + String.format(Locale.ROOT, " (%.2f°, %.2f°)", n.observation().latitude(), n.observation().longitude())
                + "<br>일시: " + n.observation().date() + " " + n.observation().time() + " (" + n.observation().zone().getId() + ")"
                + "<br>관측 가능한 별자리: " + total + "개"
                + "<br>완성 " + solved + " · 정답 공개 " + revealed + " · 남음 " + (total - solved - revealed)
                + "<br><b style='font-size:13px'>점수: " + n.score() + "점</b></html>");
        boolean remaining = total - solved - revealed > 0;
        undo.setEnabled(true);
        reset.setEnabled(true);
        submit.setEnabled(true);
        hint.setEnabled(remaining);
        reveal.setEnabled(remaining);
    }

    private void addStarLabels(Constellation c, Map<String, String> labels) {
        for (String id : c.starIds()) {
            labels.put(id, frame.catalog.star(id).displayName());
        }
    }

    // ------------------------------------------------------------------ 편집 (FR-05)

    private boolean editable() {
        return session != null && (session.mode() == GameMode.NIGHT_SKY || session.basic().editable());
    }

    /** 연결선이 바뀐 뒤 공통 처리: 힌트 강조 해제, 자동 저장, 화면 갱신. */
    private void edited(String message) {
        lastHint = null;
        frame.autoSave();
        refresh();
        status(message, Theme.INFO);
    }

    @Override
    public void starClicked(String starId) {
        if (!editable()) {
            status("이미 끝난 문제입니다. '다음 문제'를 눌러 주세요.", Theme.INFO);
            return;
        }
        if (selected == null || selected.equals(starId)) {
            selected = selected == null ? starId : null;
            canvas.setSelected(selected);
            return;
        }
        Edge e = new Edge(selected, starId);
        selected = starId;
        if (session.board().add(e)) {
            edited("선을 연결했습니다. 이어서 다음 별을 클릭하거나 빈 곳을 클릭해 선택을 해제하세요.");
        } else {
            canvas.setSelected(selected);
            status("이미 연결된 선입니다.", Theme.INFO);
        }
    }

    @Override
    public void emptyClicked() {
        selected = null;
        canvas.setSelected(null);
    }

    @Override
    public void edgeDeleteRequested(Edge edge) {
        if (editable() && session.board().remove(edge)) {
            edited("선을 삭제했습니다.");
        }
    }

    private void undo() {
        if (!editable()) {
            return;
        }
        if (session.board().undo()) {
            edited("마지막 편집을 취소했습니다.");
        } else {
            status("취소할 편집이 없습니다.", Theme.INFO);
        }
    }

    private void clearLines() {
        if (session.board().clear()) {
            selected = null;
            edited("모든 연결선을 지웠습니다. (실행 취소로 되돌릴 수 있습니다)");
        } else {
            status("지울 연결선이 없습니다.", Theme.INFO);
        }
    }

    // ------------------------------------------------------------------ 제출 (FR-06, FR-11)

    private void submit() {
        if (session.mode() == GameMode.BASIC) {
            BasicGame b = session.basic();
            if (b.submit()) {
                selected = null;
                frame.autoSave();
                refresh();
                status("정답입니다! " + b.current().fullName(), Theme.OK);
            } else {
                status("아직 정답이 아닙니다. 연결을 수정한 뒤 다시 제출해 보세요.", Theme.ERROR);
            }
            return;
        }
        NightSkyGame n = session.night();
        List<NightSkyGame.Award> awards = n.submit();
        if (awards.isEmpty()) {
            status("새로 완성된 별자리가 없습니다.", Theme.ERROR);
            return;
        }
        for (NightSkyGame.Award a : awards) {
            if (selected != null && a.constellation().contains(selected)) {
                selected = null;
            }
        }
        frame.autoSave();
        refresh();
        StringBuilder msg = new StringBuilder("완성! ");
        int sum = 0;
        for (NightSkyGame.Award a : awards) {
            msg.append(a.constellation().name()).append(" +").append(a.points()).append("점 ");
            sum += a.points();
        }
        if (awards.size() > 1) {
            msg.append("(합계 +").append(sum).append("점)");
        }
        if (n.count(CompletionState.INCOMPLETE) == 0) {
            msg.append("<br>관측 가능한 별자리를 모두 처리했습니다!");
        }
        status(msg.toString(), Theme.OK);
    }

    // ------------------------------------------------------------------ 도움 (UC-04)

    private void hint() {
        boolean basic = session.mode() == GameMode.BASIC;
        HelpResult r = basic ? session.basic().hint() : session.night().hint(selected);
        switch (r.status()) {
            case ADDED -> {
                frame.autoSave();
                lastHint = r.edge();
                refresh();
                status("힌트: 주황색 선 하나를 추가했습니다."
                        + (basic ? "" : "<br>이 별자리는 완성하면 점수의 50%만 받습니다."), Theme.HINT_LINE.darker());
            }
            case ALREADY_USED -> status(basic
                    ? "이 문제의 힌트는 이미 사용했습니다."
                    : "해당 별자리의 힌트를 이미 사용했습니다. 다른 별자리의 별을 선택한 뒤 다시 시도해 보세요.",
                    Theme.ERROR);
            case NO_TARGET -> status("도움을 받을 수 있는 미완료 별자리가 없습니다.", Theme.INFO);
            case NOTHING_TO_ADD -> status("기준 도안의 선은 이미 모두 이어져 있습니다. 불필요한 선을 지워 보세요."
                    + " (힌트는 사용되지 않았습니다)", Theme.INFO);
            case FINISHED -> status("이미 끝난 문제입니다.", Theme.INFO);
        }
    }

    private void reveal() {
        if (session.mode() == GameMode.BASIC) {
            BasicGame b = session.basic();
            int answer = JOptionPane.showConfirmDialog(this,
                    "정답 도안과 별 이름을 공개합니다. 잘못 이은 선은 지워집니다.\n이 문제는 '정답 공개 완료'로 기록됩니다.\n\n계속할까요?",
                    "정답 보기", JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION && b.reveal()) {
                selected = null;
                frame.autoSave();
                refresh();
                status("정답: " + b.current().fullName(), Theme.REVEALED_LINE.darker());
            }
            return;
        }
        NightSkyGame n = session.night();
        Constellation target = n.revealTarget(selected);
        if (target == null) {
            status("정답을 볼 수 있는 미완료 별자리가 없습니다.", Theme.INFO);
            return;
        }
        // 공개 전에는 대상이 어떤 별자리인지 보여 주지 않는다. 확인 창에서 대상을 보고 취소하면
        // 점수를 잃지 않고 정답을 알 수 있기 때문이다. 안내 문구도 대상과 관계없이 같다.
        int answer = JOptionPane.showConfirmDialog(this,
                "미완료 별자리 하나의 정답 도안과 별 이름을 공개합니다.\n"
                        + "대상: 선택한 별이 속한 별자리 (없으면 가장 많이 맞게 이은 별자리)\n"
                        + "어떤 별자리인지는 공개한 뒤에 표시됩니다.\n\n"
                        + "이 별자리의 별에 잘못 이은 선은 지워지며, 실행 취소 기록도 초기화됩니다.\n"
                        + "공개한 별자리는 이번 밤하늘 플레이에서 0점 처리되며 다시 득점할 수 없습니다.\n\n계속할까요?",
                "정답 보기", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION && n.reveal(target)) {
            frame.autoSave();
            refresh();
            status("정답 공개: " + target.fullName() + " (0점)", Theme.REVEALED_LINE.darker());
        }
    }

    // ------------------------------------------------------------------ 진행

    private void nextProblem() {
        BasicGame b = session.basic();
        b.next(frame.random);
        selected = null;
        lastHint = null;
        loadStars();
        canvas.resetView();
        frame.autoSave();
        refresh();
        status("새 문제입니다. 별의 배치를 관찰해 보세요.", Theme.INFO);
    }

    /** 조건을 바꾸면 새 플레이가 된다 (UC-03 11a). */
    private void changeConditions() {
        NightSkyGame n = session.night();
        int answer = JOptionPane.showConfirmDialog(this,
                "관측 조건을 바꾸면 새 밤하늘 플레이가 시작됩니다.\n"
                        + "현재 점수(" + n.score() + "점)와 완료·도움 기록은 새 플레이로 대체됩니다.\n\n계속할까요?",
                "관측 조건 변경", JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.OK_OPTION) {
            frame.showObservation(n.system(), n.observation(), true);
        }
    }
}
