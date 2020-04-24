(ns yamfood.integrations.iiko.core
  (:require
    [environ.core :refer [env]]
    [yamfood.integrations.iiko.api :as api]
    [yamfood.integrations.iiko.utils :as utils]))


(defn iiko-context!
  [access-token organization-id]
  (let [iiko-payment-types (:paymentTypes
                             (api/iiko-payment-types! access-token
                                                      organization-id))]
    {:access-token    access-token
     :organization-id organization-id
     :city            "Ташкент"
     :home            "1"
     :street          "Wok & Street"
     :payments        {:cash (:id (first (filter #(= (:code %) "CASH") iiko-payment-types)))
                       :card (:id (first (filter #(= (:code %) "PAYME") iiko-payment-types)))}}))


(defn create-order!
  [order]
  (let [access-token (api/get-access-token! (env :iiko-user-id)
                                            (env :iiko-user-secret))
        organization-id (-> (api/organizations! access-token)
                            (first)
                            :id)
        context (iiko-context! access-token organization-id)
        order (utils/order->iiko context order)]
    (api/create-order! access-token order)))
