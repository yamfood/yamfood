(ns yamfood.api.admin.handlers.riders
  (:require
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.riders.core :as r]))


(defn riders-list
  [_]
  {:body (r/all-riders!)})


(s/def ::name string?)

(s/def ::rider
  (s/keys :req-un [::cs/phone ::name]))

(defn add-rider
  [request]
  (let [rider (:body request)
        valid (s/valid? ::rs/rider rider)]
    (if valid
      {:body (merge rider (r/new-rider! rider))}
      {:body   {:error "Invalid rider"}
       :status 400})))


(c/defroutes
  routes
  (c/POST "/" [] add-rider)
  (c/GET "/" [] riders-list))
