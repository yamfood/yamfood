(ns yamfood.telegram.handlers.client.inline
  (:require
    [environ.core :refer [env]]
    [yamfood.core.kitchens.core :as k]
    [yamfood.core.products.core :as p]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn product-description
  [product]
  (format "%s сум"
          (u/fmt-values (:price product))))


(defn query-result-from-product
  [lang]
  (fn [product]
    {:type                  "article"
     :id                    (:id product)
     :input_message_content {:message_text (str (:id product))}
     :title                 (u/translated lang (:name product))
     :description           (product-description product)
     :thumb_url             (:thumbnail product)}))


(defn current-location-inline-result
  [lang address]
  {:type                  "article"
   :id                    99999
   :input_message_content {:message_text "Обновить локацию"}
   :title                 address
   :description           (translate lang :update-location-inline-button)
   :thumb_url             "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/320/apple/114/round-pushpin_1f4cd.png"})


(defn inline-query-handler
  ([ctx]
   (let [update (:update ctx)
         query (:inline_query update)
         location (get-in ctx [:client :payload :location])
         kitchen-id (:id (k/nearest-kitchen! (:id (:bot ctx))
                                             (:longitude location)
                                             (:latitude location)))]
     (cond
       (= (:query query) "") {:run {:function   p/all-products!
                                    :args       [kitchen-id]
                                    :next-event :c/inline}}
       :else {:run {:function   p/products-by-category-emoji!
                    :args       [(:id (:bot ctx)) (:query query)]
                    :next-event :c/inline}})))
  ([ctx products]
   (let [update (:update ctx)
         lang (:lang ctx)
         address (get-in ctx [:client :payload :location :address])]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         (into
                          [(current-location-inline-result
                             (:lang ctx)
                             (u/text-from-address address))]
                          (map (query-result-from-product lang) products))}})))


(d/register-event-handler!
  :c/inline
  inline-query-handler)
