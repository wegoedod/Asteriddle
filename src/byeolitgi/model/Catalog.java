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

    public Catalog(Map<String, Star> stars, Map<StarSystem, List<Constellation>> constellations) {
        this.stars = Collections.unmodifiableMap(new LinkedHashMap<>(stars));
        for (StarSystem s : StarSystem.values()) {
            bySystem.put(s, List.copyOf(constellations.getOrDefault(s, List.of())));
        }
    }

    public Star star(String id) {
        return stars.get(id);
    }

    public boolean hasStar(String id) {
        return stars.containsKey(id);
    }

    public Collection<Star> stars() {
        return stars.values();
    }

    public List<Constellation> constellations(StarSystem system) {
        return bySystem.get(system);
    }

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
