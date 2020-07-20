(ns yamfood.integrations.2gis.core
  (:require
    [environ.core :refer [env]]
    [clj-http.client :as http]
    [clojure.string :as str]))


(defn filtered-join
  ([arr]
   (filtered-join arr nil))
  ([arr delim]
   (->> (remove nil? arr)
        (str/join (or delim ",")))))


(defn location-by-address [q]
  (let [request {:as              :json
                 :url             (str (:2gis-url env) "/3.0/items")
                 :conn-timeout    10000
                 :socket-timeout  10000
                 :method          :get
                 :query-params    {:key       (:2gis-key env)
                                   :q         q
                                   :fields    "items.point"
                                   :region_id 208}
                 :decompress-body false
                 ;; To disable warning
                 :cookie-policy   :standard
                 :headers         {"Accept-Encoding" "identity"
                                   "Content-Type"    "application/json"
                                   "Authorization"   (str "Bearer " (:stripe-key env))}}]
    (some->
      (http/request request)
      (get-in [:body :result :items 0 :point]))))


(defn address-by-location [lat lon]
  (let [request {:as              :json
                 :url             (str (:2gis-url env) "/3.0/items")
                 :conn-timeout    10000
                 :socket-timeout  10000
                 :method          :get
                 :query-params    {:key       (:2gis-key env)
                                   :fields    "items.address"
                                   :lat       lat
                                   :lon       lon
                                   :region_id 208}
                 :cookie-policy   :standard
                 :decompress-body false
                 :headers         {"Accept-Encoding" "identity"
                                   "Content-Type"    "application/json"
                                   "Authorization"   (str "Bearer " (:stripe-key env))}}
        prefix {:street "улица"}
        suffix {:district "район"}

        {:keys [region district living_area street place building]}
        (reduce (fn [acc v]
                  (let [name (or (:building_name v) (:address_name v) (:name v))
                        key (or (:subtype v) (:type v))]
                    (if (some? name)
                      (assoc acc (keyword key) (filtered-join
                                                 [(get prefix key)
                                                  name
                                                  (get suffix key)] " "))
                      acc)))
                {}
                (-> (http/request request)
                    (get-in [:body :result :items])))]
    (filtered-join [region
                    district
                    (when living_area (str/replace living_area "ж/м" "массив"))
                    (or street place)
                    building])))
