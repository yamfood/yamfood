(ns yamfood.api.admin.handlers.riders
  (:require
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.riders.core :as r]))


(defn riders-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        count (r/riders-count!)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (r/all-riders! offset per-page))}))


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
