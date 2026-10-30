(ns task-c3)
(defn parallel-filter
  ([predicate input] (parallel-filter predicate input 64 4))
  ([predicate input block-size workers]
   (when-not (and (pos-int? block-size) (pos-int? workers))
     (throw (IllegalArgumentException. "Block size and workers must be positive integers")))
   (letfn [(step [blocks]
             (lazy-seq
               (when-let [remaining (seq blocks)]
                 (let [batch (vec (take workers remaining))
                       jobs (mapv #(future (doall (filter predicate %))) batch)]
                   (concat (mapcat deref jobs) (step (drop (count batch) remaining)))))))]
     (step (partition-all block-size input)))))
(defn demonstrate []
  (let [input (range 400)
        predicate (fn [value] (Thread/sleep 1) (even? value))]
    (println "Sequential filter:")
    (let [sequential (time (vec (filter predicate input)))]
      (println "Parallel filter:")
      (let [parallel (time (vec (parallel-filter predicate input 25 4)))]
        (assert (= sequential parallel))
        {:elements (count parallel) :equal true}))))
(defn -main [& _]
  (try (println (demonstrate))
       (finally (shutdown-agents))))
