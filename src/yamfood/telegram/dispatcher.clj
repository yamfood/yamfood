(ns yamfood.telegram.dispatcher)


(defonce handlers (atom {}))


(defn dispatch
  [[event & args]]
  (apply (event @handlers) args))


(defn register-event-handler!
  [event handler]
  (swap! handlers assoc event handler))
