package perf.yaup.json;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JsonValidatorTest {

    @Test
    public void empty_schema(){
        Json schema = Json.fromString(
                """
                {}
                """
        );
        JsonValidator validator = new JsonValidator(schema);
        Json errors = validator.validate(new Json(false));
        assertTrue(errors.isEmpty());
    }
}
