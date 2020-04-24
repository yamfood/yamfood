(ns yamfood.api.admin.handlers.products
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.products.core :as p]))


(s/def ::photo string?)
(s/def ::thumbnail string?)

(s/def ::ru string?)
(s/def ::uz string?)
(s/def ::en string?)
(s/def ::name
  (s/keys :req-un [::ru ::uz ::en]))
(s/def ::description
  (s/keys :req-un [::ru ::uz ::en]))
(s/def ::energy int?)
(s/def ::price int?)
(s/def ::position int?)
(s/def ::category_id (s/nilable int?))


(defn set-translations
  [product]
  (-> product
      (update :category :ru)
      (update :name :ru)))


(defn products-list
  [_]
  {:body
   (->> (p/all-products!)
        (map set-translations))})


(defn categories-list
  [_]
  {:body
   (->> (p/all-categories!)
        (map #(update % :name :ru)))})


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


(c/defroutes
  routes
  (c/GET "/" [] products-list)
  (c/POST "/" [] create-product)
  (c/GET "/:id{[0-9]+}/" [] product-detail)
  (c/PATCH "/:id{[0-9]+}/" [] patch-product)
  (c/DELETE "/:id{[0-9]+}/" [] delete-product)

  (c/GET "/categories/" [] categories-list))