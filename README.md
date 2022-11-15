# Autonomous Logic Commons Java

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/autonomouslogic/commons-java)](https://github.com/autonomouslogic/commons-java/releases)
[![javadoc](https://javadoc.io/badge2/com.autonomouslogic.commons/commons-java/javadoc.svg)](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java)
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/autonomouslogic/commons-java/Test/main)](https://github.com/autonomouslogic/commons-java/actions)
[![GitHub](https://img.shields.io/github/license/autonomouslogic/commons-java)](https://spdx.org/licenses/MIT-0.html)

Common Java functionality.
The implementations in this library are intended to solve simple problems in simple ways.

* [ListUtil](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/ListUtil.html)
* [SetUtil](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/SetUtil.html)
* [ResourceUtil](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/ResourceUtil.html)
* [CachedSupplier](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/cache/CachedSupplier.html)
* [Stopwatch](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/Stopwatch.html)
* [Rx3Util](https://javadoc.io/doc/com.autonomouslogic.commons/commons-java/latest/com/autonomouslogic/commons/rxjava3/Rx3Util.html)

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
