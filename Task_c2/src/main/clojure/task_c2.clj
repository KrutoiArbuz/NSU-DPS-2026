(ns task-c2)
(defn prime-numbers []
  (letfn [(step [candidate composites]
            (lazy-seq
              (loop [number candidate table composites]
                (if-let [divisors (get table number)]
                  (recur (inc' number)
                         (reduce (fn [state prime] (update state (+' number prime) (fnil conj []) prime))
                                 (dissoc table number) divisors))
                  (cons number (step (inc' number) (assoc table (*' number number) [number])))))))]
    (step 2 {})))
(def primes (prime-numbers))
(defn -main [& _]
  (println (doall (take 25 primes))))
