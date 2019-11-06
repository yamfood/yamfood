(ns yamfood.telegram.handlers.inline
  (:require [morse.api :as t]
            [environ.core :refer [env]]
            [yamfood.core.products.core :as p]
            [yamfood.telegram.dispatcher :as d]))

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
  [_ update]
  {:answer-inline
   {:inline-query-id (:id (:inline_query update))
    :options         {:cache_time 0}
    :results         (map query-result-from-product (p/get-all-products!))}})
