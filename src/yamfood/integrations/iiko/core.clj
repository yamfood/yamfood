(ns yamfood.integrations.iiko.core
  (:require
    [yamfood.utils :as u]
    [clojure.string :as str]
    [clj-http.client :as http]
    [environ.core :refer [env]]
    [clojure.data.json :as json]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


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


(def organization-id "24c446bc-7bc3-11e9-80e8-d8d38565926f")
(def n (-> (access-token!)
           (nomenclature! organization-id)))


(filter #(= (:parentGroup %) "78d36cc8-c216-492a-9770-995a29584af6") (:products n))
(filter #(= (:parentGroup %) "b18419d9-0ab2-481c-aaf9-c8a81352c587") (:groups n))


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
  [nomenclature category-ids]
  (->> (filter
         #(and (u/in? category-ids (:parentGroup %))
               (:isIncludedInMenu %))
         (:products nomenclature))
       (sort-by :order)
       (map #(select-keys % [:id :name :price]))))


(defn ->yam-products
  [p]
  (let [photo "https://5.imimg.com/data5/WN/XY/MY-40943811/750-ml-round-wok-box-500x500.jpg"]
    {:payload   {:iiko_id (:id p)}
     :name      (:name p)
     :price     (:price p)
     :energy    0
     :photo     photo
     :thumbnail photo}))


(restaurants n)
(map :id (categories n "40745ab8-634c-4891-9da7-50d980176946"))
(def prod
  (map ->yam-products (products n ["f35dc98d-5265-4aa3-bddd-80a263170363"
                                   "a58eb1a5-c38a-4064-add6-e01cacabdc4d"
                                   "818c70a8-7775-4ff3-bce9-064b02866336"
                                   "c62abbf3-5407-4c5f-885d-1bf0ef12289b"
                                   "a04bb578-3126-4359-a148-d49208d157b3"
                                   "63fdc274-7cf0-4cad-b2f7-1faaab4fabea"
                                   "6052690b-c848-4f07-a68c-38a6bdb46605"
                                   "30bb1e4f-7ce0-48f4-b324-a5ac79a4e8c4"
                                   "8a293ba6-d91c-4a98-a21b-5f6991454043"
                                   "2bb18657-99a0-4ad0-aff7-0cc5aeed01ec"
                                   "658e52a8-ea74-4679-a1c8-ffb2362d8ed5"
                                   "04fca6aa-7a17-4440-99cf-9cdb641a66f2"])))


;(jdbc/insert-multi!
;  db/db
;  "products"
;  prod)


(defn menu
  [n]
  (let [r (restaurants n)
        r (map
            (fn [restaurant]
              (assoc restaurant
                :categories
                (map
                  #(assoc % :products (products n (:id %)))
                  (categories n (:id restaurant)))))
            r)]
    r))


(menu n)



(defn product->item
  [product]
  {:id    (:id product)
   :count (:count product)})


(defn order->iiko
  [order]
  {:id      (u/uuid)
   :items   (map product->item (:products order))
   :phone   (:phone order)
   :address {:home    "666"
             :comment (:location order)}
   :comment (:comment order)})


(defn create-order!
  [access-token organization-id order]
  {:organization organization-id
   :customer     {:name  (:name order)
                  :phone (format "+%s" (:phone order))}
   :order        (order->iiko order)})



