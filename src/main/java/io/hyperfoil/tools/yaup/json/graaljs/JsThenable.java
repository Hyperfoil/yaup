package io.hyperfoil.tools.yaup.json.graaljs;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface JsThenable {
    void then(Value onResolve, Value onReject);
}
