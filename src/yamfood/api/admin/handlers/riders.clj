(ns yamfood.api.admin.handlers.riders
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.riders.core :as r]))


(defn riders-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        phone (u/str->int (get (:params request) "phone" ""))
        search (when phone [:= :riders.phone phone])
        count (r/riders-count! search)
        offset (p/calc-offset page per-page)]
    {:body (p/format-result
             count
             per-page
             page
             (r/all-riders! offset per-page search))}))


(s/def ::name string?)
(s/def ::add-rider
  (s/keys :req-un [::cs/phone ::name]))


(defn add-rider
  [request]
  (let [rider (:body request)
        valid (s/valid? ::add-rider rider)]
    (if valid
      {:body (merge rider (r/new-rider! rider))}
      {:body   {:error "Invalid rider"}
       :status 400})))


(defn rider-detail
  [request]
  (let [rider-id (u/str->int (:id (:params request)))]
    {:body (r/rider-by-id! rider-id)}))


(s/def ::amount int?)
(s/def ::make-deposit
  (s/keys :req-un [::amount]))


(defn make-deposit
  [request]
  (let [rider-id (u/str->int (:id (:params request)))
        admin-id (:id (:admin request))
        body (:body request)
        valid? (s/valid? ::make-deposit body)]
    (if valid?
      {:body (r/make-deposit! rider-id admin-id (:amount body))}
      {:body {:error "Incorrect input"}
       :code 400})))


(c/defroutes
  routes
  (c/POST "/" [] add-rider)
  (c/GET "/" [] riders-list)
  (c/GET "/:id{[0-9]+}/" [] rider-detail)
  (c/POST "/:id{[0-9]+}/deposit/" [] make-deposit))
