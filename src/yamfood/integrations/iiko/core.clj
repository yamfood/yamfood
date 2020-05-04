(ns yamfood.integrations.iiko.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.params.core :as p]
    [yamfood.integrations.iiko.api :as api]
    [yamfood.core.products.core :as products]
    [yamfood.integrations.iiko.utils :as utils]))


(defn iiko-context!
  [params access-token organization-id]
  (let [iiko-payment-types (:paymentTypes
                             (api/iiko-payment-types! access-token
                                                      organization-id))]
    {:access-token    access-token
     :organization-id organization-id
     :city            "Ташкент"
     :delivery-id     (:iiko-delivery-id params)
     :delivery-cost   (:delivery-cost params)
     :home            (:iiko-home params)
     :street          (:iiko-street params)
     :payments        {:cash (:id (first (filter #(= (:code %) "CASH") iiko-payment-types)))
                       :card (:id (first (filter #(= (:code %) "PAYME") iiko-payment-types)))}}))


(defn create-order!
  [order]
  (let [params (p/params!)
        access-token (api/get-access-token! (:iiko-user-id params)
                                            (:iiko-user-secret params))
        organization-id (-> (api/organizations! access-token)
                            (first)
                            :id)
        context (iiko-context! params access-token organization-id)
        order (utils/order->iiko context order)]
    (api/create-order! access-token order)))


(comment
  (def nomenclature
    (let [params (p/params!)
          access-token (api/get-access-token! (:iiko-user-id params)
                                              (:iiko-user-secret params))
          organization-id (-> (api/organizations! access-token)
                              (first)
                              :id)]
      (api/nomenclature! access-token organization-id)))


  (utils/categories nomenclature "40745ab8-634c-4891-9da7-50d980176946")

  (products/multiple-create-products!
    (map
      utils/iiko->product
      (utils/products nomenclature "04fca6aa-7a17-4440-99cf-9cdb641a66f2"))))
