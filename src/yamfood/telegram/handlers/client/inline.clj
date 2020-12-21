(ns yamfood.telegram.handlers.client.inline
  (:require
    [environ.core :refer [env]]
    [yamfood.core.kitchens.core :as k]
    [yamfood.core.products.core :as p]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]
    [clojure.tools.logging :as log]))


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
     :title                 (if-let [title (u/translated lang (:name product))]
                              title
                              (do (log/warn (format "Product #%s has no %s translation" (:id product) lang)
                                            (:name product))
                                  (second (first (:name product)))))
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


(defn back-button-inline-result
  [lang]
  {:type                  "article"
   :id                    88888
   :input_message_content {:message_text (translate lang :inline-back-title)}
   :title                 (translate lang :inline-back-title)
   :description           (translate lang :inline-back-description)
   :thumb_url             "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/320/apple/237/leftwards-black-arrow_2b05.png"})


(defn inline-query-handler
  ([ctx]
   (let [update (:update ctx)
         query (:inline_query update)
         location (get-in ctx [:client :payload :location])
         kitchen-id (:id (k/nearest-kitchen! (:id (:bot ctx))
                                             (:longitude location)
                                             (:latitude location)))]
     (if (:phone (:client ctx))
       (when (seq (:query query))
         {:run {:function   p/products-by-category-emoji!
                :args       [(:id (:bot ctx)) kitchen-id (:query query)]
                :next-event :c/inline}})
       {:dispatch {:args [:c/start]}})))
  ([ctx products]
   (let [update (:update ctx)
         lang (:lang ctx)
         address (get-in ctx [:client :payload :location :address])]
     {:answer-inline
      {:inline-query-id (:id (:inline_query update))
       :options         {:cache_time 0}
       :results         (flatten
                          (conj
                            [(current-location-inline-result
                               (:lang ctx)
                               (u/text-from-address address))]
                            (map (query-result-from-product lang) products)
                            [(back-button-inline-result (:lang ctx))]))}})))


(d/register-event-handler!
  :c/inline
  inline-query-handler)
