(ns yamfood.integrations.iiko.core
  (:require
    [environ.core :refer [env]]
    [yamfood.core.params.core :as p]
    [yamfood.integrations.iiko.api :as api]
    [yamfood.integrations.iiko.utils :as utils]))


(defn iiko-context!
  [params access-token organization-id]
  (let [iiko-payment-types (:paymentTypes
                             (api/iiko-payment-types! access-token
                                                      organization-id))]
    {:access-token    access-token
     :organization-id organization-id
     :city            "Ташкент"
     :home            (get :iiko-home params)
     :street          (get :iiko-street params)
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
