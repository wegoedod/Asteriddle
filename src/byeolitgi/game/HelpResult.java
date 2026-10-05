package byeolitgi.game;

import byeolitgi.model.Constellation;
import byeolitgi.model.Edge;

/** 힌트 요청 결과 (UC-04). */
public record HelpResult(Status status, Constellation target, Edge edge) {

    public enum Status {
        /** 선 하나를 추가했다. */
        ADDED,
        /** 대상의 힌트를 이미 사용했다. */
        ALREADY_USED,
        /** 도움 가능한 미완료 대상이 없다. */
        NO_TARGET,
        /** 기준 도안의 선이 이미 모두 연결되어 있어 추가할 선이 없다. */
        NOTHING_TO_ADD,
        /** 이미 정답을 맞혔거나 공개한 문제다. */
        FINISHED
    }

    public static HelpResult of(Status status) {
        return new HelpResult(status, null, null);
    }
}
