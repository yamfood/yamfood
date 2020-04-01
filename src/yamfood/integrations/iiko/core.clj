(ns yamfood.integrations.iiko.core
  (:require
    [yamfood.utils :as u]
    [clojure.string :as str]
    [clj-http.client :as http]
    [environ.core :refer [env]]
    [clojure.data.json :as json]))


(defn access-token!
  []
  (let [user-id (env :iiko-user-id)
        user-secret (env :iiko-user-secret)
        url (format "https://iiko.biz:9900/api/0/auth/access_token?user_id=%s&user_secret=%s"
                    user-id
                    user-secret)
        response (http/get url)]
    (if (http/success? response)
      (str/replace (:body response) #"\"" "")
      nil)))


(defn organizations!
  [access-token]
  (let [url (format "https://iiko.biz:9900/api/0/organization/list?access_token=%s"
                    access-token)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn nomenclature!
  [access-token organization-id]
  (let [url (format "https://iiko.biz:9900/api/0/nomenclature/%s?access_token=%s"
                    organization-id
                    access-token)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn deliveryTerminals!
  [access-token organization-id]
  (let [url (format
              "https://iiko.biz:9900/api/0/deliverySettings/getDeliveryTerminals?access_token=%s&organization=%s"
              access-token
              organization-id)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


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


(defn check-order!
  [access-token order]
  (let [url (format "https://iiko.biz:9900/api/0/orders/checkCreate?access_token=%s" access-token)
        response (http/post url {:content-type :json
                                 :body         (json/write-str order)})]
    (http/success? response)))


(defn product->item
  [product]
  {:id     (:iiko_id (:payload product))
   :amount (:count product)})


(defn get-iiko-payment-type
  ;TODO: Remove hardcoded id's
  [order]
  (let [payment (:payment order)]
    (cond
      (= payment "card") {:sum                   (:total_sum order)
                          :paymentType           {:id "09322f46-578a-d210-add7-eec222a08871"}
                          :isProcessedExternally true}
      (= payment "cash") {:sum         (:total_sum order)
                          :paymentType {:id "8304ec80-76df-417c-8344-c5d7aa9a8f2d"}})))


(defn order->iiko
  [order]
  ;TODO: Remove hardcoded id's
  {:organization     "24c446bc-7bc3-11e9-80e8-d8d38565926f"
   :deliveryTerminal (get-in order [:kitchen_payload :deliveryTerminalId])
   :order            {:id            (u/uuid)
                      :items         (map product->item (:products order))
                      :payment_items [(get-iiko-payment-type order)]
                      :phone         (:phone order)
                      :address       {:home    "666"
                                      :comment (:address order)}
                      :comment       (:comment order)}
   :customer         {:name  (:name order)
                      :phone (:phone order)}})


(defn create-order!
  [order]
  (let [order (order->iiko order)]
    (-> (access-token!)
        (check-order! order))))

