package io.hyperfoil.tools.yaup.json.graaljs;

import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import io.hyperfoil.tools.yaup.json.Json;
import org.graalvm.polyglot.proxy.Proxy;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;

public class MapProxyWrapper implements Map<Object,Object> {

    private final Map<Object,Object> map;

    public MapProxyWrapper(Map<Object,Object> map){
        this.map = map == null ? new HashMap<>() : map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return map.containsValue(o);
    }

    @Override
    public Object get(Object o) {
        Object rtrn = map.get(o);
        if(rtrn == null){
            rtrn = Null.instance;
            //rtrn = Undefined.instance;
        }else if(rtrn!=null && rtrn instanceof Json){
            rtrn = JsonProxy.create((Json)rtrn);
        }else if (rtrn!=null && rtrn instanceof Map){
            rtrn = new MapProxyWrapper((Map)rtrn);
        }
        return rtrn;
    }

    @Override
    public Object put(Object k, Object v) {
        return map.put(k,v);
    }

    @Override
    public Object remove(Object o) {
        return map.remove(o);
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> map) {
        this.map.putAll(map);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<Object> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }
}
