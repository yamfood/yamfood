(ns yamfood.api.admin.handlers.riders
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.riders.core :as r]))


(s/def ::tid number?)
(s/def ::name string?)
(s/def ::notes string?)
(s/def ::is_blocked boolean?)


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
  (let [rider-id (u/str->int (:id (:params request)))
        rider (r/rider-by-id! 2)]
    (if rider
      {:body rider}
      {:body   {:error "Not found"}
       :status 404})))


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


(s/def ::patch-rider
  (s/keys :otp-un [::tid ::cs/phone ::name ::notes ::is_blocked]))


(defn validate-patch-rider!
  [rider-id body]
  (let [rider (r/rider-by-id! rider-id)
        valid? (s/valid? ::patch-rider body)]
    (cond
      (not rider) {:body   {:error "Not found"}
                   :status 404}
      (not valid?) {:body {:error "Incorrect input"}
                    :code 400})))


(defn patch-rider
  [request]
  (let [rider-id (u/str->int (:id (:params request)))
        body (:body request)
        error (validate-patch-rider! rider-id body)]
    (if (not error)
      (do
        (r/update! rider-id body)
        {:body (r/rider-by-id! rider-id)})
      error)))


(c/defroutes
  routes
  (c/POST "/" [] add-rider)
  (c/GET "/" [] riders-list)
  (c/GET "/:id{[0-9]+}/" [] rider-detail)
  (c/PATCH "/:id{[0-9]+}/" [] patch-rider)
  (c/POST "/:id{[0-9]+}/deposit/" [] make-deposit))
