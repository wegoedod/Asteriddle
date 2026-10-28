package byeolitgi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 별 목록과 체계별 기준 도안. 프로그램 실행 중에는 변경되지 않는다. */
public final class Catalog {

    private final Map<String, Star> stars;
    private final Map<StarSystem, List<Constellation>> bySystem = new EnumMap<>(StarSystem.class);

    /** 별 목록과 체계별 별자리 목록으로 만든다. 받은 컬렉션은 복사해 보관한다. */
    public Catalog(Map<String, Star> stars, Map<StarSystem, List<Constellation>> constellations) {
        this.stars = Collections.unmodifiableMap(new LinkedHashMap<>(stars));
        for (StarSystem s : StarSystem.values()) {
            bySystem.put(s, List.copyOf(constellations.getOrDefault(s, List.of())));
        }
    }

    /** id 에 해당하는 별. 없으면 null. */
    public Star star(String id) {
        return stars.get(id);
    }

    /** id 에 해당하는 별이 있으면 true. */
    public boolean hasStar(String id) {
        return stars.containsKey(id);
    }

    /** 모든 별. */
    public Collection<Star> stars() {
        return stars.values();
    }

    /** 체계의 모든 별자리. */
    public List<Constellation> constellations(StarSystem system) {
        return bySystem.get(system);
    }

    /** 체계의 별자리 중 해당 난이도인 것. */
    public List<Constellation> constellations(StarSystem system, Difficulty difficulty) {
        List<Constellation> result = new ArrayList<>();
        for (Constellation c : bySystem.get(system)) {
            if (c.difficulty() == difficulty) {
                result.add(c);
            }
        }
        return result;
    }

    /** 없으면 null. */
    public Constellation constellation(StarSystem system, String id) {
        for (Constellation c : bySystem.get(system)) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }
}
