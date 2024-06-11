package perf.yaup.json;

import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ValueConverterTest {

    public class TestProxy implements ProxyObject{

        Map<String,Object> map = new HashMap<>();

        @Override
        public Object getMember(String key) {
            return map.get(key);
        }

        @Override
        public Object getMemberKeys() {
            return map.keySet().toArray();
        }

        @Override
        public boolean hasMember(String key) {
            return map.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            map.put(key, ValueConverter.convert(value));
        }
    };

    @Test
    public void convert_empty_object(){
        TestProxy proxy = new TestProxy();
        StringUtil.jsEval("(map)=>{map['foo']={}}",proxy);
        Object foo = proxy.getMember("foo");
        assertNotNull("foo should exist",foo);
        assertTrue("foo should be json",foo instanceof Json);
        Json json = (Json)foo;
        assertFalse("foo should be an object, not array",json.isArray());
    }
    @Test
    public void convert_empty_array(){
        TestProxy proxy = new TestProxy();
        StringUtil.jsEval("(map)=>{map['foo']=[]}",proxy);
        Object foo = proxy.getMember("foo");
        assertNotNull("foo should exist",foo);
        assertTrue("foo should be json",foo instanceof Json);
        Json json = (Json)foo;
        assertTrue("foo should be an array",json.isArray());
    }
}
