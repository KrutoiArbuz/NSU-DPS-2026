(ns task-c4)
(declare supply-msg notify-msg)
(defn dispatch [sender worker message & arguments]
  (if-let [pending (:pending (meta worker))]
    (do
      (swap! pending inc)
      (try
        (apply sender worker (fn [state & values]
                               (try (apply message state values)
                                    (finally (swap! pending dec)))) arguments)
        (catch Throwable error
          (swap! pending dec)
          (throw error))))
    (apply sender worker message arguments)))
(defn storage [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0))
        state {:storage counter :ware ware :notify-step notify-step :consumers consumers}]
    {:storage counter :ware ware :worker (agent state)}))
(defn factory [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts)
        buffer (reduce-kv (fn [result ware _] (assoc result ware 0)) {} bill)]
    {:worker (agent {:amount amount :duration duration :target-storage target-storage :bill bill :buffer buffer :inputs {}})}))
(defn take-stock [stock needed]
  (let [[before after] (swap-vals! stock #(max 0 (- % needed)))]
    (- before after)))
(defn supply-msg [state amount]
  (let [[before after] (swap-vals! (:storage state) #(+ % amount))]
    (when (and (pos? (:notify-step state))
               (> (quot after (:notify-step state)) (quot before (:notify-step state))))
      (locking *out* (println (:ware state) "amount:" after)))
    (doseq [consumer (shuffle (or (:consumers state) []))]
      (dispatch send-off (:worker consumer) notify-msg (:ware state) (:storage state) amount)))
  state)
(defn notify-msg [state ware storage-atom amount]
  (if-not (contains? (:bill state) ware)
    state
    (let [needed (- (get-in state [:bill ware]) (get-in state [:buffer ware]))
          taken (take-stock storage-atom needed)
          stocked (-> state (update-in [:buffer ware] + taken) (assoc-in [:inputs ware] storage-atom))]
      (if (every? (fn [[resource quantity]] (>= (get-in stocked [:buffer resource]) quantity)) (:bill stocked))
        (do
          (Thread/sleep (:duration stocked))
          (dispatch send (:worker (:target-storage stocked)) supply-msg (:amount stocked))
          (let [finished (update stocked :buffer #(reduce-kv (fn [buffer resource quantity] (update buffer resource - quantity)) % (:bill stocked)))]
            (when *agent*
              (doseq [[resource stock] (:inputs finished) :when (pos? @stock)]
                (dispatch send-off *agent* notify-msg resource stock @stock)))
            finished))
        stocked))))
(defn source [amount duration target-storage]
  (Thread. (fn []
             (try
               (loop []
                 (Thread/sleep duration)
                 (dispatch send (:worker target-storage) supply-msg amount)
                 (recur))
               (catch InterruptedException _
                 (.interrupt (Thread/currentThread)))))))
(defn production-line
  ([] (production-line 1))
  ([scale]
   (let [safe-storage (storage "Safe" 1)
         safe-factory (factory 1 (long (* scale 3000)) safe-storage "Metal" 3)
         clock-storage (storage "Cuckoo-clock" 1)
         clock-factory (factory 1 (long (* scale 2000)) clock-storage "Lumber" 5 "Gears" 10)
         gears-storage (storage "Gears" 20 clock-factory)
         gears-factory (factory 4 (long (* scale 1000)) gears-storage "Ore" 4)
         metal-storage (storage "Metal" 5 safe-factory)
         metal-factory (factory 1 (long (* scale 1000)) metal-storage "Ore" 10)
         lumber-storage (storage "Lumber" 20 clock-factory)
         ore-storage (storage "Ore" 10 metal-factory gears-factory)
         pending (atom 0)
         sources [(source 2 (max 1 (long (* scale 1000))) ore-storage) (source 5 (max 1 (long (* scale 4000))) lumber-storage)]
         line {:ore ore-storage :lumber lumber-storage :safes safe-storage :clocks clock-storage :sources sources :pending pending
      :agents (mapv :worker [ore-storage lumber-storage metal-storage gears-storage safe-storage clock-storage metal-factory gears-factory safe-factory clock-factory])}]
     (doseq [worker (:agents line)] (alter-meta! worker assoc :pending pending))
     line)))
(defn start [line]
  (doseq [thread (:sources line)] (.start thread))
  line)
(defn drain [line]
  (let [deadline (+ (System/nanoTime) (* 60 1000000000))]
    (loop []
      (doseq [worker (:agents line)]
        (when-let [error (agent-error worker)] (throw error))
        (when-not (await-for 10000 worker)
          (throw (ex-info "Factory drain timed out" {})))
        (when-let [error (agent-error worker)] (throw error)))
      (when (pos? @(:pending line))
        (when (> (System/nanoTime) deadline)
          (throw (ex-info "Factory drain timed out" {})))
        (recur)))))
(defn stop [line]
  (doseq [thread (:sources line)] (.interrupt thread))
  (doseq [thread (:sources line)] (.join thread 2000))
  (drain line)
  {:safes @(:storage (:safes line)) :clocks @(:storage (:clocks line))})
(defn -main [& args]
  (let [seconds (if (seq args) (Long/parseLong (first args)) 45)
        line (start (production-line))]
    (try (Thread/sleep (* seconds 1000))
         (finally (println (stop line)) (shutdown-agents)))))
