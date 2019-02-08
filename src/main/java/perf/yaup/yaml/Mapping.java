package perf.yaup.yaml;

import java.util.Map;

public interface Mapping<T> {
    Map<Object,Object> getMap(T data);
}
