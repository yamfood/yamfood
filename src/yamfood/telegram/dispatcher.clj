(ns yamfood.telegram.dispatcher)


(defonce event-handlers (atom {}))
(defonce effect-handlers (atom {}))


(defn apply-effect
  [ctx name effect]
  (let [handler (name @effect-handlers)]
    (cond
      (sequential? effect) (doall (map #(handler ctx %) effect))
      :else (handler ctx effect))))


(defn apply-effects-map!
  [ctx effects-map]
  (doall
    (map
      #(apply-effect ctx (first %) (second %))
      (seq effects-map))))


(defn dispatch!
  [ctx [event & args]]
  (let [effects (apply (event @event-handlers) ctx args)]
    (apply-effects-map! ctx effects)))


(defn register-event-handler!
  [event handler]
  (swap! event-handlers assoc event handler))


(defn register-effect-handler!
  [effect handler]
  (swap! effect-handlers assoc effect handler))
