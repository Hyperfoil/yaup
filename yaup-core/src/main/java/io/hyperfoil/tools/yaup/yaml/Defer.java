package io.hyperfoil.tools.yaup.yaml;

public interface Defer {

    public Object deferAs(Object obj,Class<?> clazz);
    public Object defer(Object obj);
}
