(ns task-c1)
(defn strings-without-neighbours [alphabet n]
  (when-not (and (integer? n) (<= 0 n))
    (throw (IllegalArgumentException. "N must be a nonnegative integer")))
  (when-not (every? #(and (string? %) (= 1 (count %))) alphabet)
    (throw (IllegalArgumentException. "Alphabet must contain one-character strings")))
  (let [letters (reduce (fn [result letter] (if (some #{letter} result) result (conj result letter))) [] alphabet)]
    (reduce (fn [prefixes _]
              (reduce (fn [result prefix]
                        (into result (map #(str prefix %)
                                          (remove #(= (last prefix) (first %)) letters))))
                      [] prefixes))
            [""] (range n))))
(defn -main [& _]
  (println (strings-without-neighbours '("a" "b" "c") 2)))
