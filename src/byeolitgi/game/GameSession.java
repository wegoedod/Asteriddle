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

    public static GameSession of(BasicGame basic) {
        return new GameSession(GameMode.BASIC, basic, null);
    }

    public static GameSession of(NightSkyGame night) {
        return new GameSession(GameMode.NIGHT_SKY, null, night);
    }

    public GameMode mode() { return mode; }
    public BasicGame basic() { return basic; }
    public NightSkyGame night() { return night; }

    public StarSystem system() {
        return mode == GameMode.BASIC ? basic.system() : night.system();
    }

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
