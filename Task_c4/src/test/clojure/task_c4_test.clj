(ns task-c4-test (:require [clojure.test :refer :all] [task-c4 :as solution]))
(deftest partial-reservations
  (let [output (solution/storage "Gears" 0)
        processor (solution/factory 4 0 output "Ore" 4)
        stock (atom 2 :validator #(>= % 0))
        waiting (solution/notify-msg @(:worker processor) "Ore" stock 2)]
    (is (= 2 (get-in waiting [:buffer "Ore"])))
    (is (= 0 @stock))
    (swap! stock + 2)
    (let [finished (solution/notify-msg waiting "Ore" stock 2)]
      (is (= 0 (get-in finished [:buffer "Ore"])))
      (is (await-for 2000 (:worker output)))
      (is (= 4 @(:storage output))))))
(deftest concurrent-stock-without-negative-values
  (let [stock (atom 10 :validator #(>= % 0))
        jobs (doall (repeatedly 8 #(future (solution/take-stock stock 3))))]
    (try
      (let [amounts (mapv #(deref % 3000 ::timeout) jobs)]
        (is (not-any? #{::timeout} amounts))
        (when (every? number? amounts)
          (is (= 10 (reduce + amounts)))
          (is (= 0 @stock))
          (is (every? #(<= 0 % 3) amounts))))
      (finally (doseq [job jobs] (future-cancel job))))))
(deftest both-products-and-bulk-processing
  (let [line (solution/production-line 0)]
    (send (:worker (:lumber line)) solution/supply-msg 50)
    (send (:worker (:ore line)) solution/supply-msg 200)
    (solution/drain line)
    (is (pos? @(:storage (:safes line))))
    (is (pos? @(:storage (:clocks line))))
    (is (every? #(zero? (.getQueueCount %)) (:agents line)))
    (is (every? #(nil? (agent-error %)) (:agents line)))))
(defn -main [& _]
  (try
    (let [result (run-tests 'task-c4-test)]
      (when (pos? (+ (:fail result) (:error result)))
        (throw (ex-info "C4 tests failed" result))))
    (finally (shutdown-agents))))
