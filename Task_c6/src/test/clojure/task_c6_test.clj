(ns task-c6-test (:require [clojure.test :refer :all] [task-c6 :as solution]))
(deftest cheapest-available-route
  (let [routes (-> solution/empty-map (solution/route "A" "C" 100 1) (solution/route "A" "B" 10 1) (solution/route "B" "C" 20 1))]
    (is (= {:price 30 :path ["B" "C"]} (solution/book-tickets routes "A" "C")))
    (is (= {:price 100 :path ["C"]} (solution/book-tickets routes "A" "C")))
    (is (:error (solution/book-tickets routes "A" "C")))
    (is (identical? (get-in routes [:forward "A" "B" :tickets]) (get-in routes [:backward "B" "A" :tickets])))))
(deftest no-partial-booking
  (let [routes (-> solution/empty-map (solution/route "A" "B" 10 3) (solution/route "B" "C" 20 0))]
    (is (:error (solution/book-tickets routes "A" "C")))
    (is (= 3 @(get-in routes [:forward "A" "B" :tickets])))
    (is (= {:price 0 :path '()} (solution/book-tickets routes "A" "A")))))
(deftest concurrent-limited-tickets
  (let [routes (-> solution/empty-map (solution/route "A" "B" 10 5) (solution/route "B" "C" 20 5))
        start (java.util.concurrent.CountDownLatch. 1)
        jobs (doall (repeatedly 30 #(future (.await start) (solution/book-tickets routes "A" "C"))))]
    (.countDown start)
    (let [bookings (mapv #(deref % 10000 ::timeout) jobs)]
      (is (not-any? #{::timeout} bookings))
      (is (= 5 (count (remove :error bookings))))
      (is (= [0 0] [@(get-in routes [:forward "A" "B" :tickets]) @(get-in routes [:forward "B" "C" :tickets])])))))
(deftest every-demo-customer-is-served
  (let [result (solution/run 10)]
    (is (every? pos? (:customers result)))
    (is (<= 0 (:restarts result)))))
(defn -main [& _]
  (try
    (let [result (run-tests 'task-c6-test)]
      (when (pos? (+ (:fail result) (:error result)))
        (throw (ex-info "C6 tests failed" result))))
    (finally (shutdown-agents))))
