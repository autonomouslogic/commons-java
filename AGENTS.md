# AGENTS.md

Keep this file current: update it in the same change whenever something it describes changes, and remove stale info rather than appending to it.

Read `README.md` first: it describes each public component and its usage. This file covers what the README doesn't.

## Project

- Single-module Gradle library, published to Maven Central as `com.autonomouslogic.commons:commons-java`.
- Compiles to Java 21; CI builds on JDK 25.
- Version comes from git tags (git-sensitive semantic versioning plugin); don't set it in `build.gradle`.
- Dependencies:
  - Guava is an `implementation` dependency (used by `ConcatCollection`).
  - RxJava 3 is `compileOnly`: consumers bring their own. Nothing outside `rxjava3` may reference RxJava.
  - No other runtime dependencies. Avoid adding any.
- CI (`build.yml`) runs `test`, `javadoc` and `jacocoTestReport`; `spotless.yml` runs `spotlessCheck`. Javadoc runs with `-Xdoclint:all,-html,-missing`, so broken `{@link}`s and bad tags fail the build. Run `./gradlew javadoc` after touching Javadoc.

## Components

All code lives under `com.autonomouslogic.commons`. Utility classes are static-only with a private constructor.

- `ListUtil`, `SetUtil`: static helpers. `ListUtil.concat` returns a `ConcatList`.
- `collection.ConcatCollection` (package-private, abstract) and `collection.ConcatList`: read-only views over several lists. The outer list is copied, the inner lists are not, so changes to them show through. `get` and `size` walk all lists (O(number of lists)). `containsAll`, `toArray`, `listIterator`, `subList` and all mutators throw `UnsupportedOperationException`.
- `ResourceUtil`: thin wrapper over `Class.getResourceAsStream` that throws `FileNotFoundException` instead of returning null.
- `Stopwatch`: not thread-safe. Starts running on creation.
- `cache.CachedSupplier`: not thread-safe; caches null.
- `config.Config`: Lombok `@Builder`. Reads `System.getenv()` on every `get()`, nothing is cached. Setting both `NAME` and `NAME_FILE` throws. File values are trimmed; empty values count as unset. A default value and a default method together throw only when the default is reached.
- `config.ConfigParser`: functional interface for custom types. `config.DefaultConfigParsers` maps the built-in types; add new built-in types there and to the `Config` Javadoc.
- `concurrent.VirtualThreads`: `callAll` and `runAll` keep at most `maxConcurrency` tasks in flight, pulling lazily from the iterator. On the first failure they `shutdownNow`, attach other failures as suppressed and rethrow the `ExecutionException`. `onVirtualThread` rethrows task exceptions unwrapped via `sneakyThrow`. `checkIsVirtual` throws `NotVirtualThreadException`.
- `rxjava3.Rx3Util`: the public entry point for everything RxJava. Implementations live in `rxjava3.internal` (public for access, not API) or as standalone transformers:
  - `toSingle`, `toMaybe`, `toCompletable`: wrap failures in `ExecutionException`; disposing cancels a `CompletableFuture`. `toCompletable` only wraps failures that have a cause.
  - `internal.ErrorWrap*Transformer`: tracks upstream errors in an instance field, so each `wrapTransformerErrors` result should be applied to only one stream.
  - `internal.OrderedMerger`: turns each source into a blocking iterator on the `io` scheduler and emits on `computation`. Disposal is not implemented.
  - `internal.ZipAll`: pads each source with endless `Optional.empty()` and stops when all are empty.
  - `WindowSort`: buffers `minWindowSize` items, sorts and emits everything except the last `minWindowSize`. Output is fully sorted only if no item is more than `minWindowSize` positions out of order.
  - `internal.CheckOrder`: errors when an item is less than the previous one; equal items pass.

## Tests

- JUnit 6 Jupiter, with Mockito, commons-io and junit-pioneer available. Tests mirror the main package layout, one `<Class>Test` per class (`Rx3Util_OrderedMergeTest` splits out `orderedMerge`).
- `ConfigTest` sets environment variables with junit-pioneer's `@SetEnvironmentVariable`; this needs the `--add-opens` JVM args in `build.gradle`.
- Contextual fixtures go in `src/test/resources/<package path>/<TestClass>/` (see `ResourceUtilTest`).

## Adding a component

- Add a README section in the existing format (description, short example, Javadoc and source links), kept in alphabetical order.
- Public API needs Javadoc that passes doclint.
- Follow the existing style: static utilities, simple implementations, no new dependencies.

## Workflow

- After every change, run `./gradlew spotlessApply` and make sure everything compiles.
- Run tests with `./gradlew test`.
- Never edit `CHANGELOG.md`; semantic-release writes it.
- Never run `publish.sh`.

## Code style

- Write self-explanatory code with sensible function and variable names.
- Comments are rare and short. Add one only when something needs clarifying or the code is complicated.
- Be concise, in code, docs and messages alike.
- Lombok is configured with chained accessors (setters return `this`); follow `lombok.config`.

## Git and PRs

- Commit messages are a single-line conventional commit, concise, e.g. `fix: Decode a decimal N without throwing NumberFormatException`.
- The commit type drives the release: `fix` is a patch, `feat` is a minor, and `!` is a major.
- No commit footers, and no `Co-Authored-By` lines.
- Never force-push. Preserve branch history on PRs.
- PR titles are conventional commits (enforced by `semantic-pr.yml`).
- PR bodies state the problem and the solution concisely, with evidence where relevant, and assume the reader knows the existing code.
