(ns yamfood.core.products.core
  (:require
    [honeysql.core :as hs]
    [honeysql.helpers :as hh]
    [yamfood.core.utils :as cu]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]))


(def all-products-query
  {:select    [:products.id
               :products.name
               :products.description
               :products.price
               :products.photo
               :products.position
               :products.thumbnail
               :products.energy
               :products.category_id
               :categories.emoji
               :categories.bot_id
               [:categories.name :category]]
   :from      [:products]
   :where     [:= :products.is_active true]
   :left-join [:categories
               [:= :categories.id :products.category_id]]
   :order-by  [:categories.position :products.position]})


(defn basket-products-totals-query
  [basket-id]
  {:select    [[(hs/raw "sum(distinct products.price)") :products_cost]
               [(hs/raw "coalesce(sum(modifiers.price), 0)") :modifiers_cost]
               [:basket_products.count :count]]
   :from      [:basket_products]
   :left-join [:products [:= :basket_products.product_id :products.id]
               :modifiers [:in
                           (hs/raw "modifiers.id::text")
                           (hs/raw "(select jsonb_array_elements_text(basket_products.payload -> 'modifiers'))")]]
   :where     [:= :basket_products.basket_id basket-id]
   :group-by  [:basket_products.id]})


(defn basket-totals-query
  [basket-id]
  {:with   [[:basket_products_totals (basket-products-totals-query basket-id)]]
   :select [[(hs/raw "coalesce(sum((totals.products_cost + totals.modifiers_cost) * totals.count)::int, 0)") :total_cost]]
   :from   [[:basket_products_totals :totals]]})


(defn disabled-products-query
  [kitchen-id]
  {:select [:disabled_products.product_id]
   :from   [:disabled_products]
   :where  [:= :disabled_products.kitchen_id kitchen-id]})


(defn modifiers!
  ([]
   (modifiers! nil))
  ([f]
   (->> {:select [:modifiers.id
                  :modifiers.name
                  :modifiers.group_id
                  :modifiers.price]
         :from   [:modifiers]}
        (merge (when (seq f) {:where f}))
        (hs/format)
        (jdbc/query db/db)
        (map #(-> %
                  (cu/keywordize-field :name)
                  (update :id str)
                  (update :group_id str))))))


(defn keywordize-json-fields
  [product]
  (-> product
      (cu/keywordize-field :category)
      (cu/keywordize-field :payload)
      (cu/keywordize-field :description)
      (cu/keywordize-field :name)))


(defn product-modifiers!
  ([]
   (product-modifiers! nil))
  ([f]
   (->> (when (seq f) {:where f})
        (merge
          {:select    [[:products.id :product_id]
                       [:products.name :product_name]
                       [:products.photo :product_photo]
                       [:products.energy :product_energy]
                       [:products.price :product_price]
                       [:products.thumbnail :product_thumbnail]
                       [:products.is_active :product_is_active]
                       [:products.category_id :product_category_id]
                       [:products.payload :product_payload]
                       [:products.position :product_position]
                       [:products.description :product_description]
                       [:modifiers.id :modifier_id]
                       [:modifiers.name :modifier_name]
                       [:modifiers.price :modifier_price]
                       [:modifiers.group_id :modifier_group_id]
                       [:product_modifiers.group_id :group_id]
                       [:product_modifiers.group_required :group_required]]
           :from      [:products]
           :left-join [:product_modifiers [:= :product_modifiers.product_id :products.id]
                       :modifiers [:= :product_modifiers.modifier_id :modifiers.id]]})
        (hs/format)
        (jdbc/query db/db)
        (map (fn [row]
               (-> row
                   (cu/keywordize-field :product_name)
                   (cu/keywordize-field :modifier_name)
                   (update :product_id #(some-> % str))
                   (update :modifier_id #(some-> % str))
                   (update :modifier_group_id #(some-> % str))
                   (cu/group-by-prefix :modifier)
                   (cu/group-by-prefix :group)
                   (cu/group-by-prefix :product))))
        ;; [{:product {:id ...}, :modifier {:id ...}]
        (group-by :product)
        (map (fn [[product product_modifiers]]
               (assoc product :groupModifiers
                              (->> product_modifiers
                                   (group-by :group)
                                   (map (fn [[group product_modifiers]]
                                          (assoc group :modifiers (map :modifier product_modifiers))))
                                   (filter #(some? (:id %))))))))))




(defn get-modifier
  [all-modifiers]
  (fn [id]
    (first (filter #(= id (:id %)) all-modifiers))))


(defn update-modifier!
  [modifier-id modifier]
  (jdbc/update!
    db/db
    "modifiers"
    modifier
    ["modifiers.id = ?" modifier-id]
    {:return-keys  true}))


(defn all-products!
  ([]
   (->> all-products-query
        (hs/format)
        (jdbc/query db/db)
        (map keywordize-json-fields)))
  ([kitchen-id]
   (->> (-> all-products-query
            (hh/merge-where
              [:not [:in
                     :products.id
                     (disabled-products-query kitchen-id)]]))
        (hs/format)
        (jdbc/query db/db)
        (map keywordize-json-fields))))


(defn products-by-bot!
  ([bot-id]
   (->> (-> all-products-query
            (hh/merge-where [:= :categories.bot_id bot-id])
            (hs/format))
        (jdbc/query db/db)
        (map keywordize-json-fields)))
  ([bot-id kitchen-id]
   (->> (-> all-products-query
            (hh/merge-where [:= :categories.bot_id bot-id])
            (hh/merge-where
              [:not [:in
                     :products.id
                     (disabled-products-query kitchen-id)]])
            (hs/format))
        (jdbc/query db/db)
        (map keywordize-json-fields))))


(defn product-detail-state-query
  [basket-id]
  {:select    [:products.id
               :products.name
               :products.description
               :products.payload
               :products.price
               :products.photo
               :products.thumbnail
               :products.energy
               :categories.emoji
               [:categories.name :category]
               [(basket-totals-query basket-id) :basket_cost]
               [(hs/raw "coalesce(basket_products.count, 0)") :count_in_basket]]
   :from      [:products]
   :where     [:= :products.is_active true]
   :order-by  [:id]
   :left-join [:categories [:= :categories.id :products.category_id]
               :basket_products [:and
                                 [:= :basket_products.basket_id basket-id]
                                 [:= :products.id :basket_products.product_id]]]
   :limit     1})


(defn- product-detail-state-by-id-query
  [basket-id product-id]
  (-> (product-detail-state-query basket-id)
      (hh/merge-where [:= :products.id product-id])
      (hs/format)))


(defn attach-modifiers!
  [product]
  (let [modifier-groups (get-in product [:payload :groupModifiers])]
    (assoc product :modifiers (map (fn [mg]
                                     (assoc
                                       mg
                                       :modifiers
                                       (map (get-modifier (modifiers!)) (:modifiers mg))))
                                   modifier-groups))))


(defn state-for-product-detail!
  [basket-id id]
  (->> (product-detail-state-by-id-query basket-id id)
       (jdbc/query db/db)
       (map keywordize-json-fields)
       (map attach-modifiers!)
       (first)))


(def all-categories-query
  {:select    [:categories.id
               :categories.name
               :categories.position
               [:bots.id :bot_id]
               [:bots.name :bot]
               :categories.emoji
               :categories.is_delivery_free]
   :from      [:categories]
   :where     [:= :categories.is_active true]
   :left-join [:bots
               [:= :bots.id :categories.bot_id]]
   :order-by  [:categories.position :categories.bot_id]})


(def categories-list-query
  {:select   [:categories.id
              :categories.name
              :categories.emoji]
   :from     [:categories]
   :order-by [:categories.position :categories.bot_id]})


(defn product-by-name!
  [name]
  (->> (-> all-products-query
           (hh/merge-where [:= :products.name name]))
       (hs/format)
       (jdbc/query db/db)
       (first)
       (keywordize-json-fields)))


(defn product-by-id!
  [id]
  (->> (-> all-products-query
           (hh/merge-where [:= :products.id id]))
       (hs/format)
       (jdbc/query db/db)
       (first)
       (keywordize-json-fields)))


(defn update-or-create-iiko-product!
  ([product]
   (update-or-create-iiko-product! product db/db))
  ([product db]
   (let [product* (-> product
                      (update :name db/map->jsonb)
                      (update :payload db/map->jsonb)
                      (update :description db/map->jsonb))]
     (when (-> (jdbc/update! db "products"
                 ;; only price and payload get updated
                 (select-keys product* [:price :payload])
                 ["products.payload->>'iiko_id' = ?" (get-in product [:payload :iiko_id])])
               (first)
               (zero?))
       (first (jdbc/insert! db "products" product*))))))


(defn update-or-create-modifier!
  ([modifier]
   (update-or-create-modifier! modifier db/db))
  ([modifier db]
   (when (-> (jdbc/update! db "modifiers"
               (select-keys modifier [:price :group_id])
               ["modifiers.id = ?" (:id modifier)])
             (first)
             (zero?))
     (first (jdbc/insert! db "modifiers" modifier)))))


(defn upsert-products-and-modifiers [products modifiers]
  (jdbc/with-db-transaction
    [t-con db/db]
    (doseq [product products]
      (update-or-create-iiko-product! product t-con))
    (doseq [modifier modifiers]
      (update-or-create-modifier! modifier t-con))))


(defn all-categories!
  []
  (->> all-categories-query
       (hs/format)
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


(defn category-by-id!
  [category-id]
  (->> (-> all-categories-query
           (hh/merge-where [:= :categories.id category-id])
           (hs/format))
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))
       (first)))


(defn update-category!
  [category-id category]
  (jdbc/update!
    db/db
    "categories"
    category
    ["categories.id = ?" category-id]))


(defn create-category!
  [category]
  (first
    (jdbc/insert!
      db/db
      "categories"
      category)))


(defn delete-category!
  [category-id]
  (jdbc/update!
    db/db
    "categories"
    {:is_active false}
    ["categories.id = ?" category-id]))


(defn categories-with-products!
  [bot-id]
  (->> (-> categories-list-query
           (update :from #(into % [:products]))
           (update :select #(into % [[:%count.products.id :products_count]]))
           (assoc :group-by [:categories.id])
           (hh/merge-where [:and
                            [:= :categories.bot_id bot-id]
                            [:= :products.is_active true]
                            [:= :products.category_id :categories.id]])
           (hs/format))
       (jdbc/query db/db)
       (map #(cu/keywordize-field % :name))))


(defn products-by-category-emoji!
  [bot-id kitchen-id emoji]
  (->> (-> all-products-query
           (hh/merge-where [:and
                            [:= :categories.bot_id bot-id]
                            [:= :categories.emoji emoji]])
           (hh/merge-where
             [:not [:in
                    :products.id
                    (disabled-products-query kitchen-id)]]))
       (hs/format)
       (jdbc/query db/db)
       (map keywordize-json-fields)))


(defn create-product!
  [product]
  (first
    (jdbc/insert!
      db/db
      "products"
      product)))


(defn multiple-create-products!
  [products]
  (first
    (jdbc/insert-multi!
      db/db
      "products"
      products)))


(defn update!
  [product-id row]
  (jdbc/update!
    db/db
    "products"
    row
    ["products.id = ?" product-id]))


(defn delete!
  [product-id]
  (jdbc/update!
    db/db
    "products"
    {:is_active false}
    ["products.id = ?" product-id]))


(defn basket-cost!
  [basket-id]
  (->> (basket-totals-query basket-id)
       (hs/format)
       (jdbc/query db/db)
       (first)
       (:total_cost)))


(defn menu-state!
  [basket-id bot-id]
  {:categories  (categories-with-products! bot-id)
   :basket_cost (basket-cost! basket-id)})
