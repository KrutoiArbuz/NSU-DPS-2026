(ns task-c5-test (:require [clojure.test :refer :all] [task-c5 :as solution]))
(deftest atomic-pair-acquisition
  (let [pair [(solution/fork) (solution/fork)]
        stats (atom {:calls 0 :starts 0})]
    (is (true? (solution/acquire-forks pair 0 stats)))
    (is (false? (solution/acquire-forks pair 1 stats)))
    (is (= [0 0] (mapv #(get @% :owner) pair)))
    (solution/release-forks pair 0 stats)
    (is (true? (solution/acquire-forks pair 1 stats)))
    (solution/release-forks pair 1 stats)
    (is (= [2 2] (mapv #(get @% :uses) pair)))))
(deftest even-and-odd-tables
  (doseq [number [2 4 5]]
    (let [result (solution/run-table number 1 2 6)]
      (is (= (vec (repeat number 6)) (:meals result)))
      (is (every? #(and (nil? (:owner %)) (= 12 (:uses %))) (:forks result)))
      (is (<= 0 (:restarts result))))))
(deftest bounded-livelock
  (doseq [number [4 5]]
    (is (= (vec (repeat number 0)) (:meals (solution/livelock-demo number 3))))))
(defn -main [& _]
  (try
    (let [result (run-tests 'task-c5-test)]
      (when (pos? (+ (:fail result) (:error result)))
        (throw (ex-info "C5 tests failed" result))))
    (finally (shutdown-agents))))
