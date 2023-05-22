# fr33m0nk/clj-bucket4j

`clj-bucket4j` is a a simple library that wraps over [Bucket4J](https://github.com/bucket4j/bucket4j/) and offers convenience methods for easy implementation in Clojure code.
For further documentation, do refer [Bucket4J official docs](https://bucket4j.com/).

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.fr33m0nk/clj-bucket4j.svg)](https://clojars.org/net.clojars.fr33m0nk/clj-bucket4j)
## Usage

All functions are available through the [`fr33m0nk.clj-bucket4j`](./src/fr33m0nk/clj_bucket4j.clj) namespace.

Add the following to your project dependencies:

- CLI/deps.edn dependency information
```
net.clojars.fr33m0nk/clj-bucket4j {:mvn/version "0.1.4"}
```
- Leningen/Boot
```
[net.clojars.fr33m0nk/clj-bucket4j "0.1.4"]
```
- Maven
```xml
<dependency>
  <groupId>net.clojars.fr33m0nk</groupId>
  <artifactId>clj-bucket4j</artifactId>
  <version>0.1.4</version>
</dependency>
```
- Gradle
```groovy
implementation("net.clojars.fr33m0nk:clj-bucket4j:0.1.4")
```

### Example usages

#### Require at the REPL with:
```clojure
(require '[fr33m0nk.clj-bucket4j :as b4j])
```

#### as a limiter for [rate-limiting of heavy work](https://bucket4j.com/8.3.0/toc.html#create-your-first-bucket-limiting-the-rate-of-heavy-work)

>Imagine that you have a thread-pool executor and you want to know what your threads are doing at the moment when thread-pool throws RejectedExecutionException. Printing stack traces of all threads in the JVM will be the best way to know where are all threads have stuck and why the thread pool is overflown. But acquiring stack traces is a very cost operation by itself, and you want to do it not often than 1 time per 10 minutes:
```clojure
(import java.util.concurrent.Executors)
(import java.util.concurrent.RejectedExecutionException)
(import java.lang.management.ManagementFactory)
(import java.util.concurrent.TimeUnit)

;; define the limit 1 time per 10 minutes
(def simple-bandwidth (b4j/simple-bandwidth 1 6000000))

;; construct the bucket
(def bucket (-> (b4j/bucket-builder)
                (b4j/add-limit simple-bandwidth)
                (b4j/build)))

(def executor (Executors/newSingleThreadExecutor))

(try
  (->> (range 10)
       (mapv #(do
                ;; simulate Executor failure
                (when (> % 5)
                  (.shutdown executor)
                  (.awaitTermination executor 1 TimeUnit/MINUTES))
                (.execute executor ^Runnable (fn []
                                               (println "processing this " %)
                                               (Thread/sleep 1000)
                                               (println "I am done"))))))
  (catch RejectedExecutionException ex
    (when (b4j/try-consume bucket 1)
      (let [thread-info-list (.dumpAllThreads (ManagementFactory/getThreadMXBean) true true)]
        ;; Dummy log fn
        (log-somewhere thread-info-list)))
    (throw ex)))


```

#### as a [throttler](https://bucket4j.com/8.3.0/toc.html#using-bucket-as-throttler)

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
  ;; If a token is not available this function will block until the refill adds one to the bucket.
  (b4j/block-and-consume bucket 1)
  
  (swap! exchange-rate #(identity %2) (poll-exchange-rate)))
  
```

## License

Copyright © 2023 Prashant Sinha

Distributed under the MIT License.
