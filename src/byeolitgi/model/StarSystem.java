package byeolitgi.model;

/** 별자리 체계 (FR-01). */
public enum StarSystem {
    IAU("IAU 88개 별자리", "constellations_iau.txt"),
    KOREAN("3원 28수 성관 (천상열차분야지도)", "constellations_korean.txt");

    private final String label;
    private final String dataFile;

    StarSystem(String label, String dataFile) {
        this.label = label;
        this.dataFile = dataFile;
    }

    public String label() { return label; }
    public String dataFile() { return dataFile; }
}
