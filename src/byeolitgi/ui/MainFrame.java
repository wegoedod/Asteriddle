package byeolitgi.ui;

import byeolitgi.game.BasicGame;
import byeolitgi.game.GameSession;
import byeolitgi.game.NightSkyGame;
import byeolitgi.model.Catalog;
import byeolitgi.model.Difficulty;
import byeolitgi.model.Observation;
import byeolitgi.model.StarSystem;
import byeolitgi.save.SaveException;
import byeolitgi.save.SaveManager;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Random;

/** 화면 전환과 게임 세션·자동 저장을 관리하는 메인 창. */
public final class MainFrame extends JFrame {

    private static final String START = "start";
    private static final String SETUP = "setup";
    private static final String OBSERVE = "observe";
    private static final String GAME = "game";

    final Catalog catalog;
    final Random random = new Random();
    private final SaveManager saves;
    private GameSession session;
    private boolean saveErrorShown;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final StartPanel startPanel;
    private final SetupPanel setupPanel;
    private final ObservationPanel observationPanel;
    private final GamePanel gamePanel;

    /** 네 화면(시작·설정·관측 조건·게임)을 CardLayout 으로 묶는다. */
    public MainFrame(Catalog catalog, SaveManager saves) {
        super("별잇기");
        this.catalog = catalog;
        this.saves = saves;
        startPanel = new StartPanel(this);
        setupPanel = new SetupPanel(this);
        observationPanel = new ObservationPanel(this);
        gamePanel = new GamePanel(this);
        root.add(startPanel, START);
        root.add(setupPanel, SETUP);
        root.add(observationPanel, OBSERVE);
        root.add(gamePanel, GAME);
        setContentPane(root);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setSize(1240, 820);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /** 실행 직후: 저장 파일을 확인하고 이어 하기 여부를 묻는다 (UC-05 5~7). */
    public void boot() {
        showStart();
        if (!saves.exists()) {
            return;
        }
        try {
            session = saves.load(catalog);
            showStart();
        } catch (SaveException e) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "저장된 진행 상황을 불러오지 못했습니다.\n사유: " + e.getMessage()
                            + "\n\n새 게임을 시작하시겠습니까?\n(읽지 못한 파일은 " + saves.file().getFileName()
                            + ".broken 으로 보관됩니다.)",
                    "불러오기 실패", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                saves.quarantine();
                showSetup();
            }
        }
    }

    /** 진행 중인 세션. 없으면 null. */
    GameSession session() {
        return session;
    }

    /** 진행 중인 세션이나 저장 파일이 있으면 true. */
    boolean hasProgress() {
        return session != null || saves.exists();
    }

    /** 시작 화면(UI-01)으로 간다. */
    void showStart() {
        startPanel.refresh(session);
        cards.show(root, START);
    }

    /** 새 게임 설정 화면(UI-02)으로 간다. */
    void showSetup() {
        setupPanel.reset();
        cards.show(root, SETUP);
    }

    /** 관측 조건 입력 화면으로 간다. prefill 이 있으면 그 값을 채운다. */
    void showObservation(StarSystem system, Observation prefill, boolean fromGame) {
        observationPanel.prepare(system, prefill, fromGame);
        cards.show(root, OBSERVE);
    }

    /** 진행 중인 세션을 게임 화면(UI-03, UI-04)에 연다. */
    void resumeGame() {
        if (session == null) {
            showStart();
            return;
        }
        gamePanel.open(session);
        cards.show(root, GAME);
    }

    /** 문제가 없으면 false (UC-01 7a). */
    boolean startBasic(StarSystem system, Difficulty difficulty) {
        BasicGame game = BasicGame.start(catalog, system, difficulty, random);
        if (game == null) {
            return false;
        }
        begin(GameSession.of(game));
        return true;
    }

    /** 밤하늘 모드 새 게임을 시작한다. */
    void startNight(NightSkyGame game) {
        begin(GameSession.of(game));
    }

    private void begin(GameSession newSession) {
        session = newSession;
        autoSave();
        resumeGame();
    }

    /** 주요 변경 후마다 호출한다 (FR-18). 실패하면 안내하고 기존 파일은 보존된다 (FR-21). */
    void autoSave() {
        if (session == null) {
            return;
        }
        try {
            saves.save(session);
            saveErrorShown = false;
        } catch (IOException e) {
            gamePanel.status("자동 저장 실패: " + e.getMessage(), Theme.ERROR);
            if (!saveErrorShown) {
                saveErrorShown = true;
                JOptionPane.showMessageDialog(this,
                        "진행 상황을 저장하지 못했습니다.\n이전에 저장된 파일은 그대로 보존됩니다.\n\n사유: " + e.getMessage()
                                + "\n위치: " + saves.file(),
                        "저장 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
