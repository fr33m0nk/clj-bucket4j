# fr33m0nk/clj-bucket4j

`clj-bucket4j` is a a simple library that wraps over [Bucket4J](https://github.com/bucket4j/bucket4j/) and offers convenience methods for easy implementation in Clojure code.
For further documentation, do refer [Bucket4J official docs](https://bucket4j.com/).

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.fr33m0nk/clj-bucket4j.svg)](https://clojars.org/net.clojars.fr33m0nk/clj-bucket4j)
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

### Example usage as a [throttler](https://bucket4j.com/8.3.0/toc.html#using-bucket-as-throttler)

#### Require at the REPL with:
```clojure
(require '[fr33m0nk.clj-bucket4j :as b4j])
```

> Suppose you need to have a fresh exchange rate between dollars and euros. To get the rate you continuously poll the third-party provider, and by contract with the provider you should poll not often than 100 times per 1 minute, else provider will block your IP:

```clojure
;; define the limit 100 times per 1 minute
(def simple-bandwidth (b4j/simple-bandwidth 100 60000))

;; construct the bucket
(def bucket (-> (b4j/bucket-builder)
                 (b4j/add-limit simple-bandwidth)
                 (b4j/build)))
                 
(def exchange-rates (atom 0.0))                 

;; do polling in infinite loop
(while true
  ;; Consume a token from the token bucket.
  ;; If a token is not available this method will block until the refill adds one to the bucket.
  (b4j/block-and-consume bucket 1)
  
  (swap! exchange-rate (poll-exchange-rate)))
  
```

## License

Copyright © 2023 Prashant Sinha

Distributed under the MIT License.
