(ns yamfood.telegram.inline
  (:require [morse.api :as t]
            [yamfood.core.products.core :as p]))

(defn format-money
  [money]
  (format "%,d сум." money))

(defn get-product-description
  [product]
  (str "Цена: " (format-money (:price product)) ", "
       (:energy product) "кКал"))

(defn query-result-from-product
  [product]
  {:type                  "article"
   :id                    (:id product)
   :input_message_content {:message_text (:name product)}
   :title                 (:name product)
   :description           (get-product-description product)
   :thumb_url             (:thumbnail product)})

(defn handle-inline-query
  [token query]
  (t/answer-inline
    token
    (:id query)
    {:cache_time 0}
    (map query-result-from-product (p/get-all-products!))))

