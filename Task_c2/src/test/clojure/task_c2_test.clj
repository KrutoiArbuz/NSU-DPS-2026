(ns task-c2-test (:require [clojure.test :refer :all] [task-c2 :as solution]))
(deftest known-primes
  (is (= [2 3 5 7 11 13 17 19 23 29] (vec (take 10 solution/primes))))
  (is (= 7919 (nth (solution/prime-numbers) 999))))
(deftest independent-primality-check
  (let [trial-prime? (fn [n] (and (> n 1) (not-any? #(zero? (mod n %)) (range 2 (inc (long (Math/sqrt n)))))))
        expected (vec (filter trial-prime? (range 2 1000)))
        actual (vec (take-while #(< % 1000) (solution/prime-numbers)))]
    (is (= expected actual))))
(defn -main [& _]
  (let [result (run-tests 'task-c2-test)]
    (when (pos? (+ (:fail result) (:error result)))
      (throw (ex-info "C2 tests failed" result)))))
