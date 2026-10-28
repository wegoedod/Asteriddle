package byeolitgi.model;

/** 별자리 체계 (FR-01). */
public enum StarSystem {
    /** 국제천문연맹 88개 별자리. */
    IAU("IAU 88개 별자리", "constellations_iau.txt"),
    /** 천상열차분야지도의 3원 28수 성관. */
    KOREAN("3원 28수 성관 (천상열차분야지도)", "constellations_korean.txt");

    private final String label;
    private final String dataFile;

    StarSystem(String label, String dataFile) {
        this.label = label;
        this.dataFile = dataFile;
    }

    /** 화면에 표시할 이름. */
    public String label() { return label; }
    /** 기준 도안 데이터 파일 이름. */
    public String dataFile() { return dataFile; }
}
