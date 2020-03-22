(ns yamfood.integrations.iiko.core
  (:require
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

