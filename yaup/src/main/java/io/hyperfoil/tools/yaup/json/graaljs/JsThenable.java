package io.hyperfoil.tools.yaup.json.graaljs;

import org.graalvm.polyglot.Value;

/**
 * functional interface to show graalvm that the implementing class has a then method
 */
@FunctionalInterface
public interface JsThenable {
    void then(Value onResolve, Value onReject);
}
