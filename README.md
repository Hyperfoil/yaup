Yaup is a utility project (Yet Another Utility Project)

There are two modules: the yaup library and a cli tool. The yaup library can be compiled with any jdk > 22.
The cli module can compile to a native executable with graalce > 22. Yaup uses graaljs which Mandrel does not support

Building the native cli utility

```bash
sdk use java 22-graalce
export GRAALVM_HOME=${JAVA_HOME}
mvn clean
quarkus build --native --no-tests -Dquarkus.native.container-build=false
```

