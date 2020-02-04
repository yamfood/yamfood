(ns yamfood.api.admin.products
  (:require
    [yamfood.core.products.core :as products]
    [compojure.core :as c]))


(defn products-list
  [request]
  {:body (products/all-products!)})


(c/defroutes
  products-routes
  (c/GET "/" [] products-list))