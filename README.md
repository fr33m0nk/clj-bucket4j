# fr33m0nk/clj-bucket4j

`clj-bucket4j` is a a simple library that wraps over [Bucket4J](https://github.com/bucket4j/bucket4j/) and offers convenience methods for easy implementation in Clojure code.

## Usage

All functions are available through the [`fr33m0nk.clj-bucket4j`](./src/fr33m0nk/clj_bucket4j.clj) namespace.

Add the following to your project dependencies:

- CLI/deps.edn dependency information:
- Leningen/Boot
- Maven

Require at the REPL with:
```clojure
(require '[fr33m0nk.clj-bucket4j :as b4j])
```
Or in your namespace as:
```clojure
(ns mynamespace
  (:require [fr33m0nk.clj-bucket4j :as b4j]))
```

## License

Copyright © 2023 Prashant Sinha

Distributed under the MIT License.
