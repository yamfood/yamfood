(ns yamfood.api.admin.handlers.riders
  (:require
    [compojure.core :as c]
    [yamfood.core.riders.core :as r]
    [yamfood.api.admin.specs.rider :as rs]
    [clojure.spec.alpha :as s]))


(defn riders-list
  [_]
  {:body (r/all-riders!)})


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
