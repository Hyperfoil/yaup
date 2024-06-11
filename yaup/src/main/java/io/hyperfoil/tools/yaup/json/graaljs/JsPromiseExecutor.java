package io.hyperfoil.tools.yaup.json.graaljs;

import org.graalvm.polyglot.Value;

/**
 * Functional interface necessary for Graalvm to see that the implementing class can be used when creating a promise
 */
@FunctionalInterface
public interface JsPromiseExecutor {
    void onPromiseCreation(Value onResolve, Value onReject);
}
