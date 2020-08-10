(ns yamfood.api.admin.handlers.products
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]
    [yamfood.core.products.core :as p]
    [yamfood.integrations.iiko.core :as i]
    [yamfood.integrations.iiko.utils :as utils]))


(s/def ::photo string?)
(s/def ::thumbnail string?)

(s/def ::ru string?)
(s/def ::uz string?)
(s/def ::en string?)
(s/def ::name
  (s/keys :req-un [::ru ::uz ::en]))

(s/def ::description
  (s/keys :opt-un [::ru ::uz ::en]))

(s/def ::energy int?)
(s/def ::bot_id int?)
(s/def ::price int?)
(s/def ::position int?)
(s/def ::is_free_delivery boolean?)
(s/def ::rider_delivery_cost int?)
(s/def ::category_id (s/nilable int?))
(s/def ::group_id (s/nilable uuid?))


(defn set-translations
  [product]
  (-> product
      (update-in [:category :name] :ru)
      (update :name :ru)
      (update :groupModifiers
              (fn [groupModifiers]
                (map (fn [group]
                       (update group :modifiers
                               (partial map #(update % :name :ru)))) groupModifiers)))))


(defn products-list
  [_]
  {:body
   (->> (p/products-with-modifiers!)
        (map set-translations))})


(defn fmt-modifier
  [modifier]
  (-> modifier
      (update :name :ru)))


(defn modifier-details
  [request]
  (let [modifier-id (u/str->uuid (:id (:params request)))
        modifier (first (p/modifiers! [:= :id modifier-id]))]
    (if modifier
      {:body modifier}
      {:body   {:error "Not found"}
       :status 404})))


(defn patch-modifier
  [request]
  (let [modifier-id (u/str->uuid (:id (:params request)))
        modifier (first (p/modifiers! [:= :id modifier-id]))
        body (select-keys (:body request) [:name :group_id :price])
        valid? (and modifier (s/valid? ::patch-modifier body))]
    (if valid?
      (try
        {:body (-> (p/update-modifier! modifier-id body)
                   (fmt-modifier))}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input or category"}
       :status 400})))


(s/def ::patch-modifier
  (s/keys
    :opt-un
    [::name
     ::price
     ::group_id]))


(defn fmt-category
  [category]
  (-> category
      (update :name :ru)
      (#(assoc % :name (format "[%s] %s" (:bot %) (:name %))))))


(defn categories-list
  [_]
  {:body
   (->> (p/all-categories!)
        (map fmt-category))})


(defn category-details
  [request]
  (let [category-id (u/str->int (:id (:params request)))
        category (p/category-by-id! category-id)]
    (if category
      {:body category}
      {:body   {:error "Not found"}
       :status 404})))


(s/def ::patch-category
  (s/keys :opt-un [::name ::bot_id ::position ::is_delivery_free ::rider_delivery_cost]))


(defn patch-category
  [request]
  (let [category-id (u/str->int (:id (:params request)))
        category (p/category-by-id! category-id)
        body (select-keys (:body request) [:name
                                           :emoji
                                           :bot_id
                                           :position
                                           :rider_delivery_cost
                                           :is_delivery_free])
        valid? (and category
                    (s/valid? ::patch-category body))]
    (if valid?
      (try
        (p/update-category! category-id body)
        {:body (-> (p/category-by-id! category-id)
                   (fmt-category))}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input or category"}
       :status 400})))


(s/def ::create-category
  (s/keys :req-un [::name ::bot_id ::position ::is_delivery_free ::rider_delivery_cost]))


(defn create-category
  [request]
  (let [body (select-keys (:body request) [:name
                                           :emoji
                                           :bot_id
                                           :position
                                           :is_delivery_free])
        valid? (s/valid? ::create-category body)]
    (if valid?
      (try
        (let [category (p/create-category! body)]
          {:body (->> (p/category-by-id! (:id category))
                      (fmt-category))})
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input"}
       :status 400})))


(defn delete-category
  [request]
  (let [category-id (u/str->int (:id (:params request)))
        category (p/category-by-id! category-id)]
    (if category
      (try
        (p/delete-category! category-id)
        {:status 204}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Not found"}
       :status 404})))


(s/def ::create-product
  (s/keys :req-un [::photo ::thumbnail ::name ::price]
          :opt-un [::energy ::category_id ::position ::description]))


(defn create-product
  [request]
  (let [body (:body request)
        valid? (s/valid? ::create-product body)]
    (if valid?
      (try
        {:body (p/create-product! body)}
        (catch Exception e
          (println e)
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input or duplicate name"}
       :status 400})))


(defn product-detail
  [request]
  (let [product-id (u/str->int (:id (:params request)))
        product (p/product-by-id! product-id)]
    (if product
      {:body product}
      {:body   {:error "Product not found"}
       :status 404})))


(s/def ::patch-product
  (s/keys
    :opt-un
    [::photo
     ::thumbnail
     ::name
     ::description
     ::price
     ::energy
     ::category_id
     ::position]))


(defn patch-product
  [request]
  (let [product-id (u/str->int (:id (:params request)))
        product (p/product-by-id! product-id)
        body (:body request)
        valid? (s/valid? ::patch-product body)]
    (if product
      (if valid?
        (do
          (p/update! product-id body)
          {:body (p/product-by-id! product-id)})
        {:body   {:error "Invalid input or duplicate name"}
         :status 400})
      {:body   {:error "Product not found"}
       :status 404})))


(defn delete-product
  [request]
  (let [product-id (u/str->int (:id (:params request)))
        product (p/product-by-id! product-id)]
    (if product
      (do
        (p/delete! product-id)
        {:status 204})
      {:body   {:error "Not found"}
       :status 404})))


(defn sync-products
  [_]
  (try
    (log/info "Starting to sync iiko products")
    (let [{products "dish" modifiers "modifier"}
          (->
            (group-by :type (:products (i/nomenclature!)))
            (update "dish" (partial map utils/iiko->product))
            (update "modifier" (partial map utils/iiko->modifier)))]
      (p/upsert-products-and-modifiers products modifiers))
    (log/info "Successfully synchronized iiko products")
    {:status 200}
    (catch Exception e
      (log/warn "Error occurred during iiko sync!" (.getMessage e))
      {:status 500})))



(c/defroutes
  routes
  (c/GET "/" [] products-list)
  (c/GET "/sync" [] sync-products)
  (c/POST "/" [] create-product)

  (c/GET "/:id{[0-9]+}/" [] product-detail)
  (c/PATCH "/:id{[0-9]+}/" [] patch-product)
  (c/DELETE "/:id{[0-9]+}/" [] delete-product)

  (c/GET "/modifiers/:id/" [] modifier-details)
  (c/PATCH "/modifiers/:id/" [] patch-modifier)


  (c/GET "/categories/" [] categories-list)
  (c/POST "/categories/" [] create-category)

  (c/GET "/categories/:id{[0-9]+}/" [] category-details)
  (c/PATCH "/categories/:id{[0-9]+}/" [] patch-category)
  (c/DELETE "/categories/:id{[0-9]+}/" [] delete-category))
