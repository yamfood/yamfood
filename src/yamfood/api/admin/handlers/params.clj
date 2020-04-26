(ns yamfood.api.admin.handlers.params
  (:require
    [compojure.core :as c]
    [yamfood.core.params.core :as p]
    [clojure.spec.alpha :as s]
    [yamfood.utils :as u]))


(defn all-params
  [_]
  {:body (p/params-detail-list!)})


(s/def ::value string?)
(s/def ::patch-param (s/keys :req-un [::value]))


(defn patch-param
  [request]
  (let [param-id (u/str->int (:id (:params request)))
        param (select-keys (:body request) [:value])
        valid? (s/valid? ::patch-param param)]
    (if valid?
      (try
        (p/update! param-id param)
        {:body (p/params-detail-list!)}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Incorrect input"}
       :status 400})))



(c/defroutes
  routes
  (c/GET "/" [] all-params)
  (c/PATCH "/:id{[0-9]+}/" [] patch-param))




