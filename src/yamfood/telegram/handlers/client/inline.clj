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


(defn current-location-inline-result
  [address]
  {:type                  "article"
   :id                    99999
   :input_message_content {:message_text "Обновить локацию"}
   :title                 address
   :description           "Коснитесь чтобы обновить\n"
   :thumb_url             "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/320/apple/114/round-pushpin_1f4cd.png"})

{:update_id 220544854, :inline_query {:id "340271653860938823", :from {:id 79225668, :is_bot false, :first_name "Рустам", :last_name "Бабаджанов", :username "kensay", :language_code "ru"}, :query "🍔", :offset ""}}

(defn inline-query-handler
  ([ctx]
   (let [update (:update ctx)
         query (:inline_query update)]
     (cond
       (= (:query query) "") {:run {:function   p/all-products!
                                    :next-event :c/inline}}
       :else {:run {:function   p/products-by-category-emoji!
                    :args       [(:query query)]
                    :next-event :c/inline}})))
  ([ctx products]
   (let [update (:update ctx)
         address (get-in ctx [:user :payload :location :address])]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         (into
                          [(current-location-inline-result
                             (u/text-from-address address))]
                          (map query-result-from-product products))}})))


(d/register-event-handler!
  :c/inline
  inline-query-handler)
