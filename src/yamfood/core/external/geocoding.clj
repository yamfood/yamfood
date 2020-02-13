(ns yamfood.core.external.geocoding
  (:require
    [environ.core :refer [env]]
    [clj-http.client :as client]
    [clojure.data.json :as json]))


(def LIQ-token (env :liq-token))

(defn- reverse-geocoding-url
  [token lon lat]
  (format
    "https://eu1.locationiq.com/v1/reverse.php?key=%s&lat=%s&lon=%s&format=json&accept-language=native"
    token lat lon))

(defn- liq-reverse-geocoding!
  [lon lat]
  (let [url (reverse-geocoding-url LIQ-token lon lat)
        result (client/get url)]
    (println url)
    (json/read-str (:body result) :key-fn keyword)))


(defn get-address!
  [lon lat]
  (liq-reverse-geocoding! lon lat))