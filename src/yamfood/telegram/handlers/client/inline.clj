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


(def current-location-inline-result
  {:type                  "article"
   :id                    99999
   :input_message_content {:message_text "Обновить локацию"}
   :title                 "ул. Богишамол, 127"
   :description           "Коснитесь чтобы обновить\n"
   :thumb_url             "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/320/apple/114/round-pushpin_1f4cd.png"})


(defn inline-query-handler
  ([_]
   {:run {:function   p/all-products!
          :next-event :c/inline}})
  ([ctx products]
   (let [update (:update ctx)]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         (into
                          [current-location-inline-result]
                          (map query-result-from-product products))}})))


(d/register-event-handler!
  :c/inline
  inline-query-handler)
