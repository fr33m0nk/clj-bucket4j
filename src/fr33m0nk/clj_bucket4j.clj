(ns fr33m0nk.clj-bucket4j
  (:import (io.github.bucket4j Bandwidth BlockingBucket BlockingStrategy Bucket ConsumptionProbe Refill SchedulingBucket TimeMeter UninterruptibleBlockingStrategy)
           (io.github.bucket4j.local LocalBucketBuilder SynchronizationStrategy)
           (java.time Duration Instant)
           (java.util.concurrent ScheduledExecutorService)))

(defn simple-bandwidth
  ([capacity interval-ms]
   (Bandwidth/simple capacity (Duration/ofMillis interval-ms)))
  ([capacity interval-ms ^String id]
   (-> (Bandwidth/simple capacity (Duration/ofMillis interval-ms))
       (.withId id)))
  ([capacity interval-ms ^String id initial-token-count]
   (-> (Bandwidth/simple capacity (Duration/ofMillis interval-ms))
       (.withId id)
       (.withInitialTokens initial-token-count))))

(defn classic-bandwidth
  ([capacity ^Refill refill]
   (Bandwidth/classic capacity refill))
  ([capacity ^Refill refill ^String id]
   (-> (Bandwidth/classic capacity refill)
       (.withId id)))
  ([capacity ^Refill refill ^String id initial-token-count]
   (-> (Bandwidth/classic capacity refill)
       (.withId id)
       (.withInitialTokens initial-token-count))))

(defn refill-greedy
  [token-quantity interval-ms]
  (Refill/greedy token-quantity (Duration/ofMillis interval-ms)))

(defn refill-intervally
  [token-quantity interval-ms]
  (Refill/intervally token-quantity (Duration/ofMillis interval-ms)))

(defn refill-intervally-aligned
  [token-quantity interval-ms ^Instant time-of-first-refill use-adaptive-initial-tokens?]
  (Refill/intervallyAligned token-quantity (Duration/ofMillis interval-ms) time-of-first-refill use-adaptive-initial-tokens?))

(defprotocol ILocalBucketBuilder
  (add-limit [bucket-builder ^Bandwidth bandwidth])
  (with-nano-second-precision [bucket-builder])
  (with-milli-second-precision [bucket-builder])
  (with-custom-time-precision [bucket-builder ^TimeMeter custom-time-meter])
  (with-synchronization-strategy [bucket-builder ^SynchronizationStrategy synchronization-strategy])
  (build [bucket-builder]))

(extend-type LocalBucketBuilder
  ILocalBucketBuilder
  (add-limit [this bandwidth]
    (.addLimit this bandwidth))
  (with-nano-second-precision [this]
    (.withNanosecondPrecision this))
  (with-milli-second-precision [this]
    (.withMillisecondPrecision this))
  (with-custom-time-precision [this custom-time-meter]
    (.withCustomTimePrecision this custom-time-meter))
  (with-synchronization-strategy [this synchronization-strategy]
    (.withSynchronizationStrategy this synchronization-strategy))
  (build [this]
    (.build this)))

(defn bucket-builder
  []
  (Bucket/builder))

(defprotocol IBucket
  (as-blocking [bucket] "Returns the blocking API for this bucket, that provides operations which are able to block caller thread in case of lack of tokens")
  (try-consume [bucket token-quantity] "Tries to consume a specified number of tokens from this bucket")
  (block-and-consume [bucket token-quantity] "Tries to consume a specified number using blocking API")
  (consume-ignoring-rate-limits [bucket token-quantity] "Consumes {@code token-quantity} from bucket ignoring all limits")
  (try-consuming-and-return-remaining [bucket token-quantity] "Tries to consume a specified number of tokens from this bucket")
  (estimate-ability-to-consume [bucket token-quantity] "Estimates ability to consume a specified number of tokens")
  (try-consume-as-much-as-possible [bucket] [bucket token-quantity] "Tries to consume as many tokens from this bucket as available at the moment of invocation
  Returns number of tokens which have been consumed, or zero if nothing was consumed")
  (add-tokens [bucket token-quantity] "Add <tt>token-quantity</tt> to bucket.")
  (force-add-tokens [bucket token-quantity] "Add <tt>token-quantity</tt> to bucket. In opposite to {@link #(add-tokens long)} usage of this method can lead to overflow bucket capacity.")
  (reset [bucket] "Reset all tokens up to maximum capacity")
  (get-available-token-count [bucket] "Returns the amount of available tokens in this bucket"))

(defprotocol IBlockingBucket
  (consume-blocking-bucket
    [blocking-bucket token-quantity]
    [blocking-bucket token-quantity ^BlockingStrategy blocking-strategy]
    "Consumes a specified number of tokens from the bucket")
  (try-consume-blocking-bucket
    [blocking-bucket token-quantity max-wait-time-ms]
    [blocking-or-scheduling-bucket token-quantity max-wait-time-ms ^BlockingStrategy blocking-strategy]
    "Tries to consume a specified number of tokens from the bucket")
  (try-consume-blocking-bucket-uninterruptibly
    [blocking-bucket token-quantity max-wait-time-ms]
    [blocking-bucket token-quantity max-wait-time-ms ^UninterruptibleBlockingStrategy uninterruptible-blocking-strategy]
    "Has same semantic with {@link (try-consume-blocking-bucket token-quantity, max-wait-time-ms, blocking-strategy)} but ignores interrupts(just restores interruption flag on exit)")
  (consume-blocking-bucket-uninterruptibly
    [blocking-bucket token-quantity max-wait-time-ms]
    [blocking-bucket token-quantity max-wait-time-ms ^UninterruptibleBlockingStrategy uninterruptible-blocking-strategy]
    "Has same semantic with {@link #(consume-blocking-bucket token-quantity blocking-strategy)} but ignores interrupts(just restores interruption flag on exit)"))

(defprotocol ISchedulingBucket
  (consume-scheduling-bucket [scheduling-bucket token-quantity ^ScheduledExecutorService scheduler]
    "Consumes the specified number of tokens from the bucket")
  (try-consume-scheduling-bucket [scheduling-bucket token-quantity max-wait-time-ms ^ScheduledExecutorService scheduler]
    "Tries to consume the specified number of tokens from the bucket"))

(defprotocol IConsumptionProbe
  (consumed? [consumption-probe])
  (get-remaining-tokens [consumption-probe])
  (get-nanos-to-wait-for-refill [consumption-probe])
  (get-nanos-to-wait-for-reset [consumption-probe]))

(extend-type ConsumptionProbe
  IConsumptionProbe
  (consumed? [this]
    (.isConsumed this))
  (get-remaining-tokens [this]
    (.getRemainingTokens this))
  (get-nanos-to-wait-for-refill [this]
    (.getNanosToWaitForRefill this))
  (get-nanos-to-wait-for-reset [this]
    (.getNanosToWaitForReset this)))

(extend-type Bucket
  IBucket
  (as-blocking [this]
    (.asBlocking this))
  (try-consume [this token-quantity]
    (.tryConsume this token-quantity))
  (block-and-consume [this token-quantity]
    (-> this as-blocking (consume-blocking-bucket token-quantity)))
  (consume-ignoring-rate-limits [this token-quantity]
    (.consumeIgnoringRateLimits this token-quantity))
  (add-tokens [this token-quantity]
    (.addTokens this token-quantity))
  (force-add-tokens [this token-quantity]
    (.forceAddTokens this token-quantity))
  (try-consuming-and-return-remaining [this token-quantity]
    (.tryConsumeAndReturnRemaining this token-quantity))
  (estimate-ability-to-consume [this token-quantity]
    (.estimateAbilityToConsume this token-quantity))
  (try-consume-as-much-as-possible
    ([this]
     (.tryConsumeAsMuchAsPossible this))
    ([this token-quantity]
     (.tryConsumeAsMuchAsPossible this token-quantity)))
  (reset [this]
    (.reset this))
  (get-available-token-count [this]
   (.getAvailableTokens this)))

(extend-type BlockingBucket
  IBlockingBucket
  (try-consume-blocking-bucket
    ([this token-quantity max-wait-time-ms]
     (.tryConsume this ^long token-quantity (Duration/ofMillis max-wait-time-ms)))
    ([this token-quantity max-wait-time-ms ^BlockingStrategy blocking-strategy]
     (.tryConsume this ^long token-quantity (Duration/ofMillis max-wait-time-ms) blocking-strategy)))
  (consume-blocking-bucket
    ([this token-quantity]
     (.consume this token-quantity))
    ([this token-quantity blocking-strategy]
     (.consume this token-quantity blocking-strategy)))
  (try-consume-blocking-bucket-uninterruptibly
    ([this token-quantity max-wait-time-ms]
     (.tryConsumeUninterruptibly this ^long token-quantity (Duration/ofMillis max-wait-time-ms)))
    ([this token-quantity max-wait-time-ms ^UninterruptibleBlockingStrategy uninterruptible-blocking-strategy]
     (.tryConsumeUninterruptibly this ^long token-quantity (Duration/ofMillis max-wait-time-ms) uninterruptible-blocking-strategy)))
  (consume-blocking-bucket-uninterruptibly
    ([this token-quantity]
     (.consumeUninterruptibly this ^long token-quantity))
    ([this token-quantity ^UninterruptibleBlockingStrategy uninterruptible-blocking-strategy]
     (.consumeUninterruptibly this ^long token-quantity uninterruptible-blocking-strategy))))

(extend-type SchedulingBucket
  ISchedulingBucket
  (try-consume-scheduling-bucket [this token-quantity max-wait-time-ms ^ScheduledExecutorService scheduler]
    (.tryConsume this ^long token-quantity (Duration/ofMillis max-wait-time-ms) scheduler))
  (consume-scheduling-bucket [this token-quantity ^ScheduledExecutorService scheduler]
    (.consume this token-quantity scheduler)))
