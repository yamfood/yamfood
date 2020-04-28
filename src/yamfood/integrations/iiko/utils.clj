(ns yamfood.integrations.iiko.utils
  (:require
    [yamfood.utils :as u]))


(defn restaurants
  [nomenclature]
  (->> (filter
         #(and (nil? (:parentGroup %))
               (:isIncludedInMenu %))
         (:groups nomenclature))
       (sort-by :order)
       (map #(select-keys % [:id :name]))))


(defn categories
  [nomenclature restaurant-id]
  (->> (filter
         #(and (= (:parentGroup %) restaurant-id)
               (:isIncludedInMenu %))
         (:groups nomenclature))
       (sort-by :order)
       (map #(select-keys % [:id :name]))))


(defn product->item
  [product]
  {:id      (:iiko_id (:payload product))
   :amount  (:count product)
   :comment (:comment product)})


(defn get-iiko-payment-type
  [context order]
  (let [payment (:payment order)]
    (cond
      (= payment "card") {:sum                   (:total_sum order)
                          :paymentType           {:id (:card (:payments context))}
                          :isProcessedExternally true}
      (= payment "cash") {:sum         (:total_sum order)
                          :paymentType {:id (:cash (:payments context))}})))


(defn order->iiko
  [context order]
  {:organization       (:organization-id context)
   :deliveryTerminalId (get-in order [:kitchen_payload :deliveryTerminalId])
   :order              {:id            (u/uuid)
                        :items         (map product->item (:products order))
                        :payment_items [(get-iiko-payment-type context order)]
                        :phone         (:phone order)
                        :address       {:city    (:city context)
                                        :home    (:home context)
                                        :street  (:street context)
                                        :comment (:address order)}
                        :comment       (str "TGBOT " (:id order))}
   :customer           {:name  (:name order)
                        :phone (:phone order)}})


