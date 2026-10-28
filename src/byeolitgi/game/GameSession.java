package byeolitgi.game;

import byeolitgi.model.GameMode;
import byeolitgi.model.StarSystem;

/** 진행 중인 게임 하나. 모드에 따라 basic 또는 night 중 하나만 존재한다. */
public final class GameSession {

    private final GameMode mode;
    private final BasicGame basic;
    private final NightSkyGame night;

    private GameSession(GameMode mode, BasicGame basic, NightSkyGame night) {
        this.mode = mode;
        this.basic = basic;
        this.night = night;
    }

    /** 기본 문제 모드 세션. */
    public static GameSession of(BasicGame basic) {
        return new GameSession(GameMode.BASIC, basic, null);
    }

    /** 밤하늘 모드 세션. */
    public static GameSession of(NightSkyGame night) {
        return new GameSession(GameMode.NIGHT_SKY, null, night);
    }

    /** 게임 모드. */
    public GameMode mode() { return mode; }
    /** 기본 문제 게임. 밤하늘 모드면 null. */
    public BasicGame basic() { return basic; }
    /** 밤하늘 게임. 기본 문제 모드면 null. */
    public NightSkyGame night() { return night; }

    /** 선택한 체계. */
    public StarSystem system() {
        return mode == GameMode.BASIC ? basic.system() : night.system();
    }

    /** 현재 모드의 연결선. */
    public EdgeBoard board() {
        return mode == GameMode.BASIC ? basic.board() : night.board();
    }

    /** 시작 화면에 보여 줄 요약. */
    public String summary() {
        if (mode == GameMode.BASIC) {
            return String.format("%s · %s · 난이도 %s · 문제 %d/%d",
                    mode.label(), basic.system().label(), basic.difficulty().label(),
                    basic.index() + 1, basic.order().size());
        }
        return String.format("%s · %s · %s · 점수 %d",
                mode.label(), night.system().label(), night.observation().describe(), night.score());
    }
}
