package perf.yaup.yaml;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.yaml.OverloadConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.StringReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverloadConstructorTest {

    static Yaml yaml = null;

    @BeforeClass
    public static void setupYaml(){
        OverloadConstructor constructor = new  OverloadConstructor();
        yaml = new Yaml(constructor);
    }


    @Test
    public void json_value_object(){
        OverloadConstructor constructor = new  OverloadConstructor();
        Node n = yaml.compose(new StringReader("key: {}"));
        Json json = OverloadConstructor.json(n);
        assertTrue("json[key] should exist",json.has("key"));
        Object key = json.get("key");
        assertTrue("key should be json",key instanceof Json);
        Json asJson = (Json)key;
        assertFalse("key should be an object, not array",asJson.isArray());

    }


}
