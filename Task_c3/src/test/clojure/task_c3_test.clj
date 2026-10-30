(ns task-c3-test (:require [clojure.test :refer :all] [task-c3 :as solution]))
(deftest finite-and-infinite-input
  (is (= (filter odd? (range 103)) (solution/parallel-filter odd? (range 103) 7 3)))
  (is (= [] (vec (solution/parallel-filter even? [] 4 2))))
  (is (= [0 2 4 6 8] (vec (take 5 (solution/parallel-filter even? (iterate inc 0) 3 2))))))
(deftest bounded-laziness
  (let [calls (atom 0)
        result (solution/parallel-filter (fn [_] (swap! calls inc) true) (iterate inc 0) 3 2)]
    (is (= 0 @calls))
    (is (= 0 (first result)))
    (is (<= @calls 6))))
(deftest blocks-really-run-concurrently
  (let [entered (java.util.concurrent.CountDownLatch. 2)
        release (java.util.concurrent.CountDownLatch. 1)
        predicate (fn [_] (.countDown entered) (.await release) true)
        reading (future (vec (solution/parallel-filter predicate [1 2] 1 2)))]
    (try
      (is (.await entered 3 java.util.concurrent.TimeUnit/SECONDS))
      (.countDown release)
      (is (= [1 2] (deref reading 3000 ::timeout)))
      (finally (.countDown release) (future-cancel reading)))))
(deftest rejects-invalid-settings
  (is (thrown? IllegalArgumentException (solution/parallel-filter even? [1] 0 2))))
(defn -main [& _]
  (try
    (let [result (run-tests 'task-c3-test)]
      (when (pos? (+ (:fail result) (:error result)))
        (throw (ex-info "C3 tests failed" result))))
    (finally (shutdown-agents))))
