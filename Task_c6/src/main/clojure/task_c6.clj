(ns task-c6)
(def empty-map {:forward {} :backward {}})
(def transaction-starts (atom 0))
(def transaction-calls (atom 0))
(defn route [route-map from to price tickets-num]
  (when-not (and (number? price) (<= 0 price) (integer? tickets-num) (<= 0 tickets-num))
    (throw (IllegalArgumentException. "Prices and ticket counts must be nonnegative")))
  (let [tickets (ref tickets-num :validator #(>= % 0))
        descriptor {:price price :tickets tickets}]
    (-> route-map
        (assoc-in [:forward from to] descriptor)
        (assoc-in [:backward to from] descriptor))))
(defn cheapest-path [route-map from to]
  (loop [distances {from 0} paths {from []} visited #{}]
    (let [remaining (remove #(contains? visited (key %)) distances)]
      (when (seq remaining)
        (let [[city cost] (apply min-key val remaining)]
          (if (= city to)
            {:price cost :path (get paths city)}
            (let [[next-distances next-paths]
                  (reduce-kv (fn [[prices routes] destination descriptor]
                               (let [candidate (+ cost (:price descriptor))
                                     previous (get prices destination)]
                                 (if (and (not (contains? visited destination)) (pos? @(:tickets descriptor))
                                          (or (nil? previous) (< candidate previous)))
                                   [(assoc prices destination candidate) (assoc routes destination (conj (get paths city) destination))]
                                   [prices routes])))
                             [distances paths] (get-in route-map [:forward city] {}))]
              (recur next-distances next-paths (conj visited city)))))))))
(defn book-tickets [route-map from to]
  (if (= from to)
    {:path '() :price 0}
    (do
      (swap! transaction-calls inc)
      (dosync
        (swap! transaction-starts inc)
        (if-let [booking (cheapest-path route-map from to)]
          (do (doseq [[origin destination] (partition 2 1 (cons from (:path booking)))]
                (alter (get-in route-map [:forward origin destination :tickets]) dec))
              booking)
          {:error "No route with available tickets"})))))
(defn make-spec []
  (-> empty-map
      (route "City1" "Capital" 200 5)
      (route "Capital" "City1" 250 5)
      (route "City2" "Capital" 200 5)
      (route "Capital" "City2" 250 5)
      (route "City3" "Capital" 300 3)
      (route "Capital" "City3" 400 3)
      (route "City1" "Town1_X" 50 2)
      (route "Town1_X" "City1" 150 2)
      (route "Town1_X" "TownX_2" 50 2)
      (route "TownX_2" "Town1_X" 150 2)
      (route "TownX_2" "City2" 50 3)
      (route "City2" "TownX_2" 150 3)
      (route "City2" "Town2_3" 50 2)
      (route "Town2_3" "City2" 150 2)
      (route "Town2_3" "City3" 50 3)
      (route "City3" "Town2_3" 150 2)))
(def spec1 (make-spec))
(defn booking-future
  ([route-map from to init-delay loop-delay] (booking-future route-map from to init-delay loop-delay nil))
  ([route-map from to init-delay loop-delay first-round]
   (future
     (Thread/sleep init-delay)
     (loop [bookings [] first? true]
       (Thread/sleep loop-delay)
       (let [booking (book-tickets route-map from to)]
         (when (and first? first-round)
           (.countDown first-round)
           (when-not (.await first-round 5 java.util.concurrent.TimeUnit/SECONDS)
             (throw (ex-info "First booking round timed out" {}))))
         (if (:error booking)
           bookings
           (recur (conj bookings booking) false)))))))
(defn run [loop-delay]
  (reset! transaction-starts 0)
  (reset! transaction-calls 0)
  (let [routes (make-spec)
        first-round (java.util.concurrent.CountDownLatch. 3)
        begin (System/nanoTime)
        jobs [(booking-future routes "City1" "City3" 0 loop-delay first-round)
              (booking-future routes "City1" "City2" 0 loop-delay first-round)
              (booking-future routes "City2" "City3" 0 loop-delay first-round)]]
    (try
      (let [bookings (mapv #(deref % 10000 ::timeout) jobs)]
        (when (some #{::timeout} bookings)
          (throw (ex-info "Booking timed out" {})))
        {:delay-ms loop-delay :bookings bookings :customers (mapv count bookings)
         :seconds (/ (- (System/nanoTime) begin) 1e9)
         :starts @transaction-starts :transactions @transaction-calls :restarts (- @transaction-starts @transaction-calls)})
      (finally (doseq [job jobs] (future-cancel job))))))
(defn -main [& args]
  (try
    (doseq [delay (if (seq args) [(Long/parseLong (first args))] [1 10 50])]
      (println (run delay)))
    (finally (shutdown-agents))))
