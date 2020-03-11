(ns yamfood.api.admin.handlers.kitchens
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.kitchens.core :as k]))


(s/def ::name string?)
(s/def ::longitude float?)
(s/def ::latitude float?)
(s/def ::location
  (s/keys :req-un [::longitude ::latitude]))


(defn kitchens-list
  [_]
  {:body (k/all-kitchens!)})


(defn kitchen-detail
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)]
    (if kitchen
      {:body kitchen}
      {:body   {:error "Not found"}
       :status 404})))

(s/valid? ::location {:longitude 1
                      :latitude  1})

(s/def ::create-kitchen
  (s/keys :req-un [::name ::location]))


(defn create-kitchen
  [request]
  (let [body (:body request)
        valid? (s/valid? ::create-kitchen body)
        name (:name body)
        location (:location body)]
    (if valid?
      {:body (k/create! name
                        (:longitude location)
                        (:latitude location))}
      {:body   {:error "Incorrect input"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/" [] kitchens-list)
  (c/POST "/" [] create-kitchen)
  (c/GET "/:id{[0-9]+}/" [] kitchen-detail))

