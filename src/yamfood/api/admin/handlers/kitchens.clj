(ns yamfood.api.admin.handlers.kitchens
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.kitchens.core :as k]))


(defn time?
  [str]
  (and
    (string? str)
    (not
      (nil?
        (re-matches
          #"^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"
          str)))))


(s/def ::name string?)
(s/def ::longitude float?)
(s/def ::latitude float?)
(s/def ::location
  (s/keys :req-un [::longitude ::latitude]))
(s/def ::start_at time?)
(s/def ::end_at time?)
(s/def ::payload map?)


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


(s/def ::create-kitchen
  (s/keys :req-un [::name ::location]
          :opt-un [::start_at ::end_at ::payload]))


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


(s/def ::patch-kitchen
  (s/keys :opt-un [::name ::location
                   ::start_at ::end_at
                   ::payload]))


(defn patch-kitchen
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)
        body (:body request)
        valid? (s/valid? ::patch-kitchen body)]
    (if (and kitchen valid?)
      (do
        (k/update! kitchen-id body)
        {:body (k/kitchen-by-id! kitchen-id)})
      {:body   {:error "Invalid input or kitchen_id"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/" [] kitchens-list)
  (c/POST "/" [] create-kitchen)
  (c/GET "/:id{[0-9]+}/" [] kitchen-detail)
  (c/PATCH "/:id{[0-9]+}/" [] patch-kitchen))

