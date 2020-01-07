(ns yamfood.telegram.handlers.client.inline
  (:require
    [environ.core :refer [env]]
    [yamfood.core.products.core :as p]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]))


(defn product-description
  [product]
  (format "%s сум, %s кКал"
          (u/fmt-values (:price product))
          (u/fmt-values (:energy product))))


(defn query-result-from-product
  [product]
  {:type                  "article"
   :id                    (:id product)
   :input_message_content {:message_text (:name product)}
   :title                 (:name product)
   :description           (product-description product)
   :thumb_url             (:thumbnail product)})


(defn inline-query-handler
  ([_]
   {:run {:function   p/all-products!
          :next-event :c/inline}})
  ([ctx products]
   (let [update (:update ctx)]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         (map query-result-from-product products)}})))


(d/register-event-handler!
  :c/inline
  inline-query-handler)
