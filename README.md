# fr33m0nk/clj-bucket4j

`clj-bucket4j` is a a simple library that wraps over [Bucket4J](https://github.com/bucket4j/bucket4j/) and offers convenience methods for easy implementation in Clojure code.
For further documentation, do refer [Bucket4J officiaal docs](https://bucket4j.com/).

## Usage

All functions are available through the [`fr33m0nk.clj-bucket4j`](./src/fr33m0nk/clj_bucket4j.clj) namespace.

Add the following to your project dependencies:

- CLI/deps.edn dependency information
```
net.clojars.fr33m0nk/clj-bucket4j {:mvn/version "0.1.0"}
```
- Leningen/Boot
```
[net.clojars.fr33m0nk/clj-bucket4j "0.1.0"]
```
- Maven
```xml
<dependency>
  <groupId>net.clojars.fr33m0nk</groupId>
  <artifactId>clj-bucket4j</artifactId>
  <version>0.1.0</version>
</dependency>
```
- Gradle
```groovy
implementation("net.clojars.fr33m0nk:clj-bucket4j:0.1.0")
```

### Require at the REPL with:
```clojure
(require '[fr33m0nk.clj-bucket4j :as b4j])
```
### or  in your namespace as:
```clojure
(ns mynamespace
  (:require [fr33m0nk.clj-bucket4j :as b4j]))
```

## License

Copyright © 2023 Prashant Sinha

Distributed under the MIT License.
