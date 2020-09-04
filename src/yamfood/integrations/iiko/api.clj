(ns yamfood.integrations.iiko.api
  (:require
    [clojure.string :as str]
    [clj-http.client :as http]
    [environ.core :refer [env]]
    [clojure.data.json :as json]))


(def iiko-host "https://iiko.biz:9900")
(def access-token-url (str iiko-host "/api/0/auth/access_token?user_id=%s&user_secret=%s"))
(def organizations-url (str iiko-host "/api/0/organization/list?access_token=%s"))
(def payment-types-url (str iiko-host "/api/0/rmsSettings/getPaymentTypes?access_token=%s&organization=%s"))
(def nomenclature-url (str iiko-host "/api/0/nomenclature/%s?access_token=%s"))
(def delivery-terminals-url (str iiko-host "/api/0/deliverySettings/getDeliveryTerminals?access_token=%s&organization=%s"))
(def check-order-url (str iiko-host "/api/0/orders/checkCreate?access_token=%s"))
(def order-info-url (str iiko-host "/api/0/orders/info?access_token=%s&organization=%s&order=%s"))
(def create-order-url (str iiko-host "/api/0/orders/add?access_token=%s"))
(def stop-list-url (str iiko-host "/api/0/stopLists/getDeliveryStopList?access_token=%s&organization=%s"))


(defn get-access-token!
  [user-id user-secret]
  (let [url (format access-token-url
                    user-id
                    user-secret)
        response (http/get url)]
    (if (http/success? response)
      (str/replace (:body response) #"\"" "")
      nil)))


(defn organizations!
  [access-token]
  (let [url (format organizations-url
                    access-token)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn iiko-payment-types!
  [access-token organization-id]
  (let [url (format payment-types-url
                    access-token
                    organization-id)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn nomenclature!
  [access-token organization-id]
  (let [url (format nomenclature-url
                    organization-id
                    access-token)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn delivery-terminals!
  [access-token organization-id]
  (let [url (format
              delivery-terminals-url
              access-token
              organization-id)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn check-order!
  [access-token order]
  (let [url (format check-order-url access-token)
        response (http/post url {:content-type          :json
                                 :body                  (json/write-str order)
                                 :throw-entire-message? true})]
    (http/success? response)))


(defn create-order!
  [access-token order]
  (let [url (format create-order-url access-token)
        response (http/post url {:content-type          :json
                                 :body                  (json/write-str order)
                                 :connection-timeout    5000
                                 :throw-entire-message? true})]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn order-info!
  [access-token organization-id order-id]
  (let [url (format order-info-url
                    access-token
                    organization-id
                    order-id)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))


(defn stop-list!
  [access-token organization-id]
  (let [url (format stop-list-url
                    access-token
                    organization-id)
        response (http/get url)]
    (if (http/success? response)
      (json/read-str (:body response) :key-fn keyword))))
