(ns fr33m0nk.clj-bucket4j
  (:import (io.github.bucket4j Bandwidth BlockingBucket BlockingStrategy Bucket BucketConfiguration ConfigurationBuilder ConsumptionProbe Refill SchedulingBucket TimeMeter TokensInheritanceStrategy UninterruptibleBlockingStrategy VerboseBucket VerboseResult)
           (io.github.bucket4j.distributed.proxy RecoveryStrategy RemoteBucketBuilder)
           (io.github.bucket4j.distributed.proxy.generic.compare_and_swap AbstractCompareAndSwapBasedProxyManager)
           (io.github.bucket4j.distributed.proxy.optimization Optimization)
           (io.github.bucket4j.local LocalBucketBuilder SynchronizationStrategy)
           (java.time Duration Instant)
           (java.util Optional)
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

(defprotocol IBuilder
  (build
    [bucket-builder]
    [remote-bucket-builder ^String key ^BucketConfiguration bucket-configuration]
    "Builds builder instance to concrete object"))

(defprotocol IBucketBuilder
  (add-limit [bucket-builder ^Bandwidth bandwidth] "Adds limit to Builder object of LocalBucketBuilder or ConfigurationBuilder")
  (with-nano-second-precision [bucket-builder] "Sets nanosecond precision for Builder object of LocalBucketBuilder")
  (with-milli-second-precision [bucket-builder] "Sets millisecond precision for Builder object of LocalBucketBuilder")
  (with-custom-time-precision [bucket-builder ^TimeMeter custom-time-meter] "Sets custom time precision for Builder object of LocalBucketBuilder using supplied TimeMeter object")
  (with-synchronization-strategy [bucket-builder ^SynchronizationStrategy synchronization-strategy] "Sets Synchronization for Builder object of LocalBucketBuilder using supplied SynchronizationStrategy object"))

(extend-type LocalBucketBuilder
  IBucketBuilder
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
  IBuilder
  (build [this]
    (.build this)))

(defn bucket-builder
  "returns new builder instance for customizing Bucket"
  ^LocalBucketBuilder
  []
  (Bucket/builder))

(defprotocol IBucket
  (as-blocking [bucket] "Returns the blocking API for this bucket, that provides operations which are able to block caller thread in case of lack of tokens")
  (as-scheduler [bucket] "Returns the Scheduler API for this bucket")
  (as-verbose [bucket] "Returns the Verbose API for this bucket")
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
  (consumed? [consumption-probe] "Returns Boolean from ConsumptionProbe indicating whether consumption was a success")
  (get-remaining-tokens [consumption-probe] "Returns remaining tokens from ConsumptionProbe after consumption")
  (get-nanos-to-wait-for-refill [consumption-probe] "Returns Nano seconds to wait from ConsumptionProbe for a complete refill")
  (get-nanos-to-wait-for-reset [consumption-probe] "Returns Nano seconds to wait from ConsumptionProbe for a complete reset"))

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
  (as-scheduler [this]
    (.asScheduler this))
  (as-verbose [this]
    (.asVerbose this))
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

(extend-type VerboseBucket
  IBucket
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

(defprotocol IVerboseResult
  (get-value [verbose-result] "Value from VerboseResult")
  (get-configuration [verbose-result] "BucketConfiguration Object from VerboseResult")
  (get-state [verbose-result] "BucketState Object from VerboseResult")
  (get-operation-time-nanos [verbose-result] "Operation time from VerboseResult")
  (get-diagnostics [verbose-result] "Diagnostics map from VerboseResult")
  (get-verbose-result-map [verbose-result] "VerboseResult as a map"))

(extend-type VerboseResult
  IVerboseResult
  (get-value [this]
    (.getValue this))
  (get-configuration [this]
    (.getConfiguration this))
  (get-state [this]
    (.getState this))
  (get-operation-time-nanos [this]
    (.getOperationTimeNanos this))
  (get-diagnostics [this]
    (let [diagnostics (.getDiagnostics this)]
      {:full-refilling-time-nanos (.calculateFullRefillingTime diagnostics)
       :available-tokens (.getAvailableTokens diagnostics)
       :available-tokens-per-bandwidth (vec (.getAvailableTokensPerEachBandwidth diagnostics))}))
  (get-verbose-result-map [this]
    {:value (get-value this)
     :configuration (get-configuration this)
     :state (get-state this)
     :operation-time-nanos (get-operation-time-nanos this)
     :diagnostics (get-diagnostics this)}))

(extend-type ConfigurationBuilder
  IBucketBuilder
  (add-limit [this ^Bandwidth bandwidth]
    (.addLimit this bandwidth))
  IBuilder
  (build [this]
    (.build this)))

(defprotocol IRemoteBucketBuilder
  (with-recovery-strategy [remote-bucket-builder ^RecoveryStrategy recovery-strategy] "Recovery strategy for RemoteBucketBuilder")
  (with-optimization [remote-bucket-builder ^Optimization optimization] "Optimization strategy for RemoteBucketBuilder")
  (with-implicit-configuration-replacement [remote-bucket-builder ^long desired-configuration-revision ^TokensInheritanceStrategy token-inheritance-strategy] "Configuration replacement strategy for RemoteBucketBuilder"))

(extend-type RemoteBucketBuilder
  IRemoteBucketBuilder
  (with-recovery-strategy [this ^RecoveryStrategy recovery-strategy]
    (.withRecoveryStrategy this recovery-strategy))
  (with-optimization [this ^Optimization optimization]
    (.withOptimization this optimization))
  (with-implicit-configuration-replacement [this ^long desired-configuration-revision ^TokensInheritanceStrategy token-inheritance-strategy]
    (.withImplicitConfigurationReplacement this desired-configuration-revision token-inheritance-strategy))
  IBuilder
  (build [this ^String key ^BucketConfiguration bucket-configuration]
    (.build this key bucket-configuration)))

(defn bucket-configuration-builder
  "returns new Bucket Configuration Builder instance for customizing Bucket Configuration"
  ^ConfigurationBuilder
  []
  (BucketConfiguration/builder))

(defprotocol ICompareAndSwapBasedProxyManager
  (get-remote-bucket-builder [proxy-manager] "Returns RemoteBucketBuilder instance from AbstractCompareAndSwapBasedProxyManager")
  (get-proxy-configuration [proxy-manager bucket-key]) "Returns BucketConfiguration for provided key from AbstractCompareAndSwapBasedProxyManager")

(extend-type AbstractCompareAndSwapBasedProxyManager
  ICompareAndSwapBasedProxyManager
  (get-remote-bucket-builder [this]
    (.builder this))
  (get-proxy-configuration [this bucket-key]
    (let [^Optional proxy-configuration (.getProxyConfiguration this bucket-key)]
      (.orElse proxy-configuration nil))))
