package byeolitgi.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 별자리(IAU) 또는 성관(3원 28수). 기준 도안(lines)이 곧 정답이다.
 * 구성 별은 연결선에 등장하는 별로 정의한다.
 */
public final class Constellation {

    private final String id;
    private final StarSystem system;
    private final String name;
    private final String subtitle;
    private final Set<Edge> lines;
    private final Set<String> starIds;
    private final Difficulty difficulty;

    public Constellation(String id, StarSystem system, String name, String subtitle,
                         Set<Edge> lines, Difficulty difficulty) {
        this.id = id;
        this.system = system;
        this.name = name;
        this.subtitle = subtitle;
        this.lines = Collections.unmodifiableSet(new LinkedHashSet<>(lines));
        Set<String> stars = new LinkedHashSet<>();
        for (Edge e : lines) {
            stars.add(e.a());
            stars.add(e.b());
        }
        this.starIds = Collections.unmodifiableSet(stars);
        this.difficulty = difficulty;
    }

    public String id() { return id; }
    public StarSystem system() { return system; }
    public String name() { return name; }
    public String subtitle() { return subtitle; }
    public Set<Edge> lines() { return lines; }
    public Set<String> starIds() { return starIds; }
    public Difficulty difficulty() { return difficulty; }

    public boolean contains(String starId) {
        return starIds.contains(starId);
    }

    /** 정답 공개 시 표시할 전체 이름. 예: "큰곰자리 (Ursa Major)" */
    public String fullName() {
        return subtitle == null ? name : name + " (" + subtitle + ")";
    }

    @Override
    public String toString() {
        return system + ":" + id;
    }
}
