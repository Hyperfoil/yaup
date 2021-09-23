package io.hyperfoil.tools.yaup.json.graaljs;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface JsPromiseExecutor {
    void onPromiseCreation(Value onResolve, Value onReject);
}
