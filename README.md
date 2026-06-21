# Autonomous Logic Commons Java

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/autonomouslogic/commons-java)](https://github.com/autonomouslogic/commons-java/releases)
[![javadoc](https://javadoc.io/badge2/com.autonomouslogic.commons/commons-java/javadoc.svg)](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java)
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/autonomouslogic/commons-java/Test/main)](https://github.com/autonomouslogic/commons-java/actions)
[![GitHub](https://img.shields.io/github/license/autonomouslogic/commons-java)](https://spdx.org/licenses/MIT-0.html)

Common Java functionality.
The implementations in this library are intended to solve simple problems in simple ways.

## Usage

Gradle:
```
implementation 'com.autonomouslogic.commons:commons-java:version'
```

Maven:
```
<dependency>
  <groupId>com.autonomouslogic.commons</groupId>
  <artifactId>commons-java</artifactId>
  <version>version</version>
</dependency>
```

## CachedSupplier

Wraps a `Supplier` and caches its result, so the underlying logic runs only once.
Useful for expensive operations like opening database connections or loading large datasets.

```java
Supplier<DatabaseConnection> supplier = () -> new DatabaseConnection();
CachedSupplier<DatabaseConnection> cached = new CachedSupplier<>(supplier);

cached.get(); // Creates connection
cached.get(); // Returns cached connection (no new creation)
```

- [Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/cache/CachedSupplier.html)
- [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/cache/CachedSupplier.java)

## Collection Utilities

#### ListUtil

Utilities for combining lists. Two approaches depending on your needs:

- **`concat()`** — Combines lists into a read-only view without copying elements. Useful for iteration and searching across multiple lists as one logical sequence.
  ```java
  List<String> first = List.of("a", "b");
  List<String> second = List.of("c", "d");
  List<String> combined = ListUtil.concat(first, second);  // lightweight, no copy
  combined.size();   // 4
  combined.get(2);   // "c"
  ```

- **`concatCopy()`** — Copies all elements from multiple lists into a new ArrayList. Useful when you need a mutable list or standalone data.
  ```java
  List<String> combined = ListUtil.concatCopy(first, second);  // new ArrayList with all elements
  combined.add("e");  // works, unlike concat()
  ```

[Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/ListUtil.html) | [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/ListUtil.java)

#### SetUtil

Utilities for combining sets.

- **`mergeCopy()`** — Merges multiple sets into a new LinkedHashSet. Duplicates are automatically handled.
  ```java
  Set<String> colors1 = Set.of("red", "blue");
  Set<String> colors2 = Set.of("blue", "green");
  Set<String> merged = SetUtil.mergeCopy(colors1, colors2);  // {red, blue, green}
  ```

- **`addAll()`** — Adds elements from multiple sets to an existing target set (in-place).
  ```java
  Set<String> existing = new HashSet<>();
  SetUtil.addAll(existing, Set.of("apple"), Set.of("banana"));
  ```

[Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/SetUtil.html) | [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/SetUtil.java)

## Config

Builder-based configuration reader that parses environment variables into typed values with fallback defaults.
Supports many types out-of-the-box (Integer, Boolean, Duration, URI, etc.) and file-based secret injection.

```java
// Define configs statically
public class AppConfig {
    public static final Config<String> ENVIRONMENT = Config.<String>builder()
        .name("ENVIRONMENT")
        .type(String.class)
        .defaultValue("dev")
        .build();

    public static final Config<Integer> PORT = Config.<Integer>builder()
        .name("PORT")
        .type(Integer.class)
        .defaultValue(8080)
        .build();
}

// Read values
Optional<String> env = AppConfig.ENVIRONMENT.get();  // with default
Integer port = AppConfig.PORT.getRequired();         // throws if not set
```

File-based secrets: set `PORT_FILE=/run/secrets/port` to read from a file instead of the `PORT` variable directly.

- [Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/config/Config.html)
- [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/config/Config.java)

## ResourceUtil

Loads resources (files) from the classpath and throws clear exceptions if resources are not found.
Particularly useful for loading configuration files and test fixtures, with contextual loading for organizing test data by test class.

```java
// Load from classpath
try (var in = ResourceUtil.loadResource("/config.json")) {
    var config = new String(in.readAllBytes());
}

// Load test fixtures organized by test class
// For class com.example.MyTest, loads from /com/example/MyTest/data.json
try (var in = ResourceUtil.loadContextual(MyTest.class, "/data.json")) {
    var data = new String(in.readAllBytes());
}
```

Throws `FileNotFoundException` with the missing path if a resource doesn't exist, unlike standard Java
resource loading which returns null. Two methods: `loadResource()` for absolute/package-relative paths,
`loadContextual()` for paths relative to a test class's directory.

- [Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/ResourceUtil.html)
- [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/ResourceUtil.java)

## Rx3Util

RxJava 3 utilities for non-blocking async operations and stream processing.

**Key methods:**

- `toSingle()`, `toMaybe()`, `toCompletable()` — Convert CompletionStage to RxJava types (non-blocking, unlike fromFuture)
- `retryWithDelayFlowable()` — Retry streams with configurable delay and error filtering
- `orderedMerge()` — Merge sorted streams while maintaining order
- `zipAllFlowable()` — Zip streams until all complete (vs standard zip which stops at shortest)
- `wrapTransformerErrors()` — Wrap transformer errors with context for debugging
- `windowSort()` — Sort stream items within a sliding window
- `checkOrder()` — Verify stream is strictly ordered, error if not

- [Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/rxjava3/Rx3Util.html)
- [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/rxjava3/Rx3Util.java)

## Stopwatch

Simple stopwatch for measuring elapsed time with nanosecond precision using `System.nanoTime()`.
Supports multiple start/stop cycles with accumulated time, useful for benchmarking and performance monitoring.

```java
Stopwatch watch = Stopwatch.start();
doWork();
watch.stop();

System.out.println("Elapsed: " + watch.getDuration());

// Resume measuring and accumulate more time
watch.restart();
doMoreWork();
watch.stop();

System.out.println("Total: " + watch.getDuration());  // combined time from both cycles
```

Key methods: `start()` creates and starts a stopwatch, `stop()` accumulates time, `restart()` resumes measurement,
`getNanos()` gets total nanoseconds, `getDuration()` gets total as Duration. Idempotent: calling stop/restart
multiple times won't cause errors.

- [Javadoc](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/Stopwatch.html)
- [Source](https://github.com/autonomouslogic/commons-java/blob/main/src/main/java/com/autonomouslogic/commons/Stopwatch.java)

## Other Common Libraries

* [Apache Commons](https://commons.apache.org/)
* [Guava](https://github.com/google/guava)
* [Durian](https://github.com/diffplug/durian)

## Versioning
This project follows [semantic versioning](https://semver.org/).

## Code Style
This project follows Palantir with tabs.
Automatic code formatting can be done by running `./gradlew spotlessApply`.

## License
This project is licensed under the [MIT-0 license](https://spdx.org/licenses/MIT-0.html).

## Status
| Type          | Status                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CodeClimate   | [![Maintainability](https://api.codeclimate.com/v1/badges/28e13c606dc431c7a1fa/maintainability)](https://codeclimate.com/github/autonomouslogic/commons-java/maintainability)                                                                                                                                                                                                                                     |
| SonarCloud    | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_commons-java&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_commons-java) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_commons-java&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_commons-java) |
| Libraries.io  | ![Libraries.io dependency status for latest release](https://img.shields.io/librariesio/release/maven/com.autonomouslogic.commons:commons-java)                                                                                                                                                                                                                                                                   |
| Snyk          | [![Known Vulnerabilities](https://snyk.io/test/github/autonomouslogic/commons-java/badge.svg)](https://snyk.io/test/github/autonomouslogic/commons-java)                                                                                                                                                                                                                                                          |
| Codecov       | [![codecov](https://codecov.io/gh/autonomouslogic/commons-java/branch/main/graph/badge.svg?token=C5CO3GPGV3)](https://codecov.io/gh/autonomouslogic/commons-java)                                                                                                                                                                                                                                                 |
| Synatype Lift | [link](https://lift.sonatype.com/)                                                                                                                                                                                                                                                                                                                                                                                |
