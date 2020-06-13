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


(defn products
  [nomenclature category-id]
  (->> (filter
         #(and (= (:parentGroup %) category-id)
               (:isIncludedInMenu %))
         (:products nomenclature))
       (sort-by :order)))


(defn modifiers
  [nomenclature]
  (->> (filter
         #(= "modifier" (:type %))
         (:products nomenclature))))


(defn iiko->modifier-group
  [modifier-group]
  {:required  (:required modifier-group)
   :modifiers (map :modifierId (:childModifiers modifier-group))})



(defn iiko->product
  [iiko-product]
  {:payload     {:iiko_id        (:id iiko-product)
                 :groupModifiers (map iiko->modifier-group (:groupModifiers iiko-product))}
   :price       (:price iiko-product)
   :name        {:ru (:name iiko-product)}
   :description {:ru (:description iiko-product)}
   :energy      (:energyAmount iiko-product)
   :is_active   (not (:isDeleted iiko-product))})


(defn iiko->modifier
  [iiko-modifier]
  {:id       (u/str->uuid (:id iiko-modifier))
   :group_id (u/str->uuid (:groupId iiko-modifier))
   :price    (:price iiko-modifier)
   :name     {:ru (:name iiko-modifier)}})


(defn modifier->item-modifier
  [modifier]
  {:id      (:id modifier)
   :groupId (:group_id modifier)
   :amount  1})


(defn product->item
  [product]
  {:id        (:iiko_id (:payload product))
   :amount    (:count product)
   :comment   (:comment product)
   :modifiers (map modifier->item-modifier (:modifiers product))})


(defn get-iiko-payment-type
  [context order delivery]
  (let [payment (:payment order)]
    (cond
      (= payment "card") [{:sum                   (+ (:total_sum order) delivery)
                           :paymentType           {:id (:card (:payments context))}
                           :isProcessedExternally true}]
      (= payment "cash") nil)))


(defn order->iiko
  [context order]
  (let [deliveries-count (int (/ (:delivery_cost order)
                                 (:delivery-cost context)))]
    {:organization       (:organization-id context)
     :deliveryTerminalId (get-in order [:kitchen_payload :deliveryTerminalId])
     :order              {:id           (u/uuid)
                          :items        (into (map product->item (:products order))
                                              (when (not (= (:delivery_cost order) 0))
                                                [{:id      (:delivery-id context)
                                                  :comment ""
                                                  :amount  deliveries-count}]))
                          :paymentItems (into [] (get-iiko-payment-type context order (* deliveries-count
                                                                                         (:delivery-cost context))))
                          :phone        (:phone order)
                          :address      {:city    (:city context)
                                         :home    (:home context)
                                         :street  (:street context)
                                         :comment (:address order)}
                          :comment      (format "TGBOT %s (%s)" (:id order) (:notes order))}
     :customer           {:name  (:name order)
                          :phone (:phone order)}}))
