package io.hyperfoil.tools.yaup.json;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A map backed by Json that support jsonpath get
 */
public class JsonMap implements Map<Object,Object> {
    private final Json json;
    public JsonMap(Json json){
        this.json = json;
    }

    @Override
    public int size() {
        return json.size();
    }

    @Override
    public boolean isEmpty() {
        return json.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return json.has(key) || Json.find(json,key.toString(),null)!=null;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object key) {
        if(key == null){
            return null;
        }
        Object rtrn = new Json();
        return json.has(key) ? json.get(key) : Json.find(json,key.toString(),null);
    }

    @Override
    public Object put(Object key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<?, ?> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<Object> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Object> values() {
        return Collections.emptySet();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return Collections.emptySet();
    }
}
