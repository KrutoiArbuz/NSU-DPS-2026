(ns task-c1-test (:require [clojure.test :refer :all] [task-c1 :as solution]))
(deftest example-and-boundaries
  (is (= #{"ab" "ac" "ba" "bc" "ca" "cb"} (set (solution/strings-without-neighbours '("a" "b" "c") 2))))
  (is (= [""] (solution/strings-without-neighbours [] 0)))
  (is (= [] (solution/strings-without-neighbours [] 2)))
  (is (= [] (solution/strings-without-neighbours ["x"] 2)))
  (is (= ["a" "b"] (solution/strings-without-neighbours ["a" "a" "b"] 1))))
(deftest larger-result
  (let [result (solution/strings-without-neighbours ["a" "b" "c"] 4)]
    (is (= 24 (count result)))
    (is (every? #(and (= 4 (count %)) (every? (fn [[a b]] (not= a b)) (partition 2 1 %))) result))))
(deftest invalid-input
  (is (thrown? IllegalArgumentException (solution/strings-without-neighbours ["ab"] 2)))
  (is (thrown? IllegalArgumentException (solution/strings-without-neighbours ["a"] -1))))
(defn -main [& _]
  (let [result (run-tests 'task-c1-test)]
    (when (pos? (+ (:fail result) (:error result)))
      (throw (ex-info "C1 tests failed" result)))))
