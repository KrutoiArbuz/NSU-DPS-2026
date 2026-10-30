(ns task-c5)
(defn fork []
  (ref {:owner nil :uses 0}))
(defn transaction [statistics operation]
  (swap! statistics update :calls inc)
  (dosync
    (swap! statistics update :starts inc)
    (operation)))
(defn acquire-forks [forks philosopher statistics]
  (transaction statistics
    (fn []
      (if (every? #(nil? (:owner @%)) forks)
        (do (doseq [item forks] (alter item #(-> % (assoc :owner philosopher) (update :uses inc)))) true)
        false))))
(defn release-forks [forks philosopher statistics]
  (transaction statistics
    (fn []
      (doseq [item forks]
        (when (= philosopher (:owner @item))
          (alter item assoc :owner nil))))))
(defn run-table [number thinking-ms dining-ms periods]
  (when-not (and (integer? number) (<= 2 number) (every? #(and (integer? %) (<= 0 %)) [thinking-ms dining-ms periods]))
    (throw (IllegalArgumentException. "Need at least two philosophers and nonnegative integer durations and periods")))
  (let [forks (vec (repeatedly number fork))
        statistics (atom {:calls 0 :starts 0})
        begin (System/nanoTime)
        jobs (mapv (fn [philosopher]
                     (future
                       (let [pair [(forks philosopher) (forks (mod (inc philosopher) number))]]
                         (dotimes [_ periods]
                           (Thread/sleep thinking-ms)
                           (loop []
                             (when-not (acquire-forks pair philosopher statistics)
                               (Thread/sleep 1)
                               (recur)))
                           (try (Thread/sleep dining-ms)
                                (finally (release-forks pair philosopher statistics))))
                         periods)))
                   (range number))]
    (try
      (let [meals (mapv #(deref % 60000 ::timeout) jobs)
            measured @statistics]
        (when (some #{::timeout} meals)
          (throw (ex-info "Philosopher timed out" {})))
        {:philosophers number :meals meals :forks (mapv deref forks)
         :seconds (/ (- (System/nanoTime) begin) 1e9)
         :transactions (:calls measured) :starts (:starts measured) :restarts (- (:starts measured) (:calls measured))})
      (finally (doseq [job jobs] (future-cancel job))))))
(defn livelock-demo [number rounds]
  (when-not (and (integer? number) (<= 2 number) (pos-int? rounds))
    (throw (IllegalArgumentException. "Need at least two philosophers and positive rounds")))
  (let [forks (vec (repeatedly number fork))
        barrier (java.util.concurrent.CyclicBarrier. number)
        jobs (mapv (fn [philosopher]
                     (future
                       (dotimes [_ rounds]
                         (dosync (alter (forks philosopher) assoc :owner philosopher))
                         (.await barrier 5 java.util.concurrent.TimeUnit/SECONDS)
                         (assert (some? (:owner @(forks (mod (inc philosopher) number)))))
                         (.await barrier 5 java.util.concurrent.TimeUnit/SECONDS)
                         (dosync (alter (forks philosopher) assoc :owner nil))
                         (.await barrier 5 java.util.concurrent.TimeUnit/SECONDS))
                       0))
                   (range number))]
    (try {:philosophers number :rounds rounds :meals (mapv #(deref % 10000 ::timeout) jobs)}
         (finally (doseq [job jobs] (future-cancel job))))))
(defn -main [& args]
  (try
    (if (seq args)
      (let [[number thinking dining periods] (mapv #(Long/parseLong %) args)]
        (println (run-table number thinking dining periods)))
      (doseq [number [4 5]]
        (println (run-table number 20 20 10))
        (println "Bounded livelock example:" (livelock-demo number 5))))
    (finally (shutdown-agents))))
