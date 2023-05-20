(ns fr33m0nk.clj-bucket4j-test
  (:require [clojure.test :refer :all]
            [fr33m0nk.clj-bucket4j :as b4j])
  (:import (java.time Instant)))

(deftest rate-limit-test
  (testing "Parks for 10 seconds"
    (let [simple-bandwidth (b4j/simple-bandwidth 1 10000)
          bucket (-> (b4j/bucket-builder)
                     (b4j/add-limit simple-bandwidth)
                     (b4j/build))
          tracker (volatile! [])]
      (loop [tracker tracker
             i 0]
        (when (< i 10)
          (b4j/block-and-consume bucket 1)
          (vswap! tracker conj {:run i :time (.getEpochSecond (Instant/now))})
          (recur tracker (inc i))))
      (is (every? #(>= % 10)
                  (->> @tracker
                     (map :time)
                     (#(map (fn [x1 x2]
                              (- x2 x1)) % (rest %)))))))))
