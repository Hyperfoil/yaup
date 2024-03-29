package perf.yaup.json;

import io.hyperfoil.tools.yaup.PopulatePatternException;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JsonMapTest {


    @Test
    public void get_jsonpath_keys(){
        Json foo = Json.fromJs("{ foo: { bar: { biz: 'buz' } }, found: { foo: true } }");
        JsonMap map = new JsonMap(foo);
        try{

            String response = StringUtil.populatePattern("${{=found.foo ? `${foo.bar.biz}` : 'fail' }}",map,"${{","__","}}","=");
            assertEquals("buz",response);
        }catch (PopulatePatternException e){
            fail(e.getMessage());
        }
    }
    @Test
    public void get_jsonpath_keys_missing(){
        Json foo = Json.fromJs("{ foo: { bar: { biz: 'buz' } }, found: { foo: true } }");
        JsonMap map = new JsonMap(foo);
        try{

            String response = StringUtil.populatePattern("${{=found.bar ? `${foo.bar.buz}` : 'fail' }}",map,"${{","__","}}","=");
            assertEquals("fail",response);
        }catch (PopulatePatternException e){
            fail(e.getMessage());
        }
    }
}
