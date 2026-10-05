package byeolitgi.save;

/** 저장 파일이 없거나 손상되어 불러올 수 없을 때 (UC-05 5b). */
public final class SaveException extends Exception {

    public SaveException(String message) {
        super(message);
    }

    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
