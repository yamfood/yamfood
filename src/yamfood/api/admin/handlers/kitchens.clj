(ns yamfood.api.admin.handlers.kitchens
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [clj-time.format :as timef]
    [clj-time.coerce :as timec]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.kitchens.core :as k]
    [yamfood.core.products.core :as p]))


(s/def ::name string?)
(s/def ::longitude float?)
(s/def ::latitude float?)
(s/def ::location
  (s/keys :req-un [::longitude ::latitude]))
(s/def ::start_at cs/timestamp?)
(s/def ::end_at cs/timestamp?)
(s/def ::bot_id int?)
(s/def ::payload map?)


(defn format-time
  [time]
  (->> time
       (timec/from-sql-date)
       (timef/unparse (timef/formatter "HH:mm"))))


(defn format-working-hours
  [kitchen]
  (-> kitchen
      (update :start_at format-time)
      (update :end_at format-time)))


(defn kitchens-list
  [_]
  {:body (->> (k/all-kitchens!)
              (map format-working-hours))})



(defn kitchen-details!
  [kitchen-id]
  (let [kitchen (k/kitchen-by-id! kitchen-id)]
    (-> kitchen
        (format-working-hours)
        (assoc :disabled_products (->> (k/kitchen-disabled-products! kitchen-id)
                                       (map #(update % :name :ru)))))))


(defn kitchen-detail
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)]
    (if kitchen
      {:body (kitchen-details! kitchen-id)}
      {:body   {:error "Not found"}
       :status 404})))


(s/def ::create-kitchen
  (s/keys :req-un [::name ::bot_id ::location]
          :opt-un [::start_at ::end_at ::payload]))


(defn create-kitchen
  [request]
  (let [body (:body request)
        valid? (s/valid? ::create-kitchen body)
        name (:name body)
        location (:location body)]
    (if valid?
      (try
        {:body (k/create! name
                          (:longitude location)
                          (:latitude location)
                          (:payload body)
                          (:start_at body)
                          (:end_at body)
                          (:bot_id body))}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Incorrect input"}
       :status 400})))


(s/def ::patch-kitchen
  (s/keys :opt-un [::name ::location
                   ::start_at ::end_at
                   ::payload ::bot_id]))


(defn patch-kitchen
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)
        body (:body request)
        valid? (s/valid? ::patch-kitchen body)]
    (if (and kitchen valid?)
      (try
        (k/update! kitchen-id body)
        {:body (kitchen-details! kitchen-id)}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input or kitchen_id"}
       :status 400})))


(defn add-disabled-product
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        product-id (u/str->int (:product-id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)
        product (p/product-by-id! product-id)]
    (if (and kitchen product)
      (do
        (k/add-disabled-product! kitchen-id product-id)
        {:body (kitchen-details! kitchen-id)})
      {:body   {:error "Kitchen or Product does not exist"}
       :status 400})))


(defn delete-disabled-product
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        product-id (u/str->int (:product-id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)
        product (p/product-by-id! product-id)]
    (if (and kitchen product)
      (do
        (k/delete-disabled-product! kitchen-id product-id)
        {:body (kitchen-details! kitchen-id)})
      {:body   {:error "Kitchen or Product does not exist"}
       :status 400})))


(defn kitchen-products
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)]
    {:body (->> (p/products-by-bot! (:bot_id kitchen))
                (map #(update % :name :ru)))}))


(c/defroutes
  routes
  (c/GET "/" [] kitchens-list)
  (c/POST "/" [] create-kitchen)

  (c/GET "/:id{[0-9]+}/" [] kitchen-detail)
  (c/PATCH "/:id{[0-9]+}/" [] patch-kitchen)

  (c/GET "/:id{[0-9]+}/products/" [] kitchen-products)

  (c/POST "/:id{[0-9]+}/disabled/:product-id{[0-9]+}/" [] add-disabled-product)
  (c/DELETE "/:id{[0-9]+}/disabled/:product-id{[0-9]+}/" [] delete-disabled-product))