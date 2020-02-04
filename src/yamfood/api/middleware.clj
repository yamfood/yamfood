(ns yamfood.api.middleware)


(defn wrap-cors
  "Wrap the server response in a Control-Allow-Origin Header to
  allow connections from the web app."
  [handler]
  (fn [request]
    (let [method (:request-method request)
          response (cond
                     (= (compare method :options) 0) {:body "OK!"}
                     :else (handler request))]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "x-requested-with, content-type, token")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "*")))))
