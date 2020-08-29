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
        rider (r/rider-by-id! rider-id)]
    (if rider
      {:body (assoc rider :info [{:label "Баланс"
                                  :value (str " " (:balance rider) " сум")}])}
      {:body   {:error "Not found"}
       :status 404})))


(defn rider-balance-logs
  [request]
  (let [rider-id (u/str->int (:id (:params request)))
        params (->> [:offset :limit]
                    (select-keys (->> (:query-params request)
                                      (medley.core/map-keys keyword)))
                    (medley.core/map-vals u/str->int))
        offset (or (:offset params) 0)
        limit (or (:limit params) 10)
        logs (r/balance-logs! rider-id limit offset)
        total (r/count-balance-logs! rider-id)]
    {:body {:logs   logs
            :total  total}}))


(s/def ::amount int?)
(s/def ::make-deposit
  (s/keys :req-un [::amount]))


(defn withdraw-from-balance
  [request]
  (let [rider-id (u/str->int (:id (:params request)))
        admin-id (:id (:admin request))
        body (:body request)
        valid? (s/valid? ::make-deposit body)]
    (if valid?
      {:body (r/withdraw-from-balance! rider-id admin-id (:amount body) nil)}
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
  (c/GET "/:id{[0-9]+}/balance-logs" [] rider-balance-logs)
  (c/PATCH "/:id{[0-9]+}/" [] patch-rider)
  (c/POST "/:id{[0-9]+}/withdraw/" [] withdraw-from-balance))
