(ns yamfood.api.admin.handlers.products
  (:require
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.core.products.core :as p]))


(s/def ::photo string?)
(s/def ::thumbnail string?)
(s/def ::name string?)
(s/def ::energy int?)
(s/def ::price int?)
(s/def ::category_id int?)
(s/def ::is_active boolean?)


(defn products-list
  [_]
  {:body (p/all-products!)})


(s/def ::create-product
  (s/keys :req-un [::photo ::thumbnail ::name ::price]
          :opt-un [::energy ::category_id]))


(defn validate-create-product!
  [body]
  (let [valid? (s/valid? ::create-product body)
        name (:name body)]
    (if valid?
      (nil? (p/product-by-name! name))
      false)))


(defn create-product
  [request]
  (let [body (:body request)
        valid? (validate-create-product! body)]
    (if valid?
      (let [product (p/create-product! body)]
        {:body product})
      {:body   {:error "Invalid input or duplicate name"}
       :status 400})))


(c/defroutes
  routes
  (c/GET "/" [] products-list)
  (c/POST "/" [] create-product))