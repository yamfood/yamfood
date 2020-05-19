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


(defn disabled-products-query
  [kitchen-id]
  {:select [:disabled_products.product_id]
   :from   [:disabled_products]
   :where  [:= :disabled_products.kitchen_id kitchen-id]})


(def modifier-groups
  [{:required  true
    :modifiers ["f0e5b012-8ab2-48bd-9008-b844c3184cc9"
                "fc30a0c7-1ffd-4ee1-bc78-4d0b966aac88"
                "67c97b05-d224-4a35-be62-94598ca08831"
                "c16bffd7-f0b8-46d0-b482-520d10e6112f"
                "10c1f575-d190-4e2c-b33b-ceefa6895a7f"
                "7b0eddf9-24a5-4ab9-b93f-1c56ae7342a0"
                "4193af88-7211-42e1-b645-ef28c0190a75"
                "1f0497f6-7c3f-4e93-8f89-ff75fbb1fe2b"
                "edd3da9a-beaa-467c-ad76-219998edc302"
                "78b78f89-9906-449e-93c7-73e5920d41b4"
                "4303da6b-23bc-4179-8a49-724a51e18b8e"
                "653511eb-7904-4757-b550-a684ed390f0c"
                "55e71919-7380-4605-a07d-6001b0d93adc"
                "ba321a99-86b9-4062-86ba-740387e9a24b"
                "4468e484-b96d-4885-8744-13255453cc34"
                "56f20dac-ceab-447d-8092-63ff7d8f4e9f"
                "c049238a-054c-4095-bc60-385150452f69"]}
   {:required  true
    :modifiers ["773d9e6d-8933-4df3-b5f3-2eb78f4f7dab"
                "9098aeb6-07b0-401e-b27c-e2951729f7f7"
                "6d3b9e6a-aa05-48a8-b768-f06b517954f2"
                "aa91bc5e-ddce-4a82-b7f7-5a118b0920e5"
                "ced88454-24c5-46f1-993f-a352cfaa2d28"
                "5b0ccf5b-84a8-40db-95e3-8c3879429f8d"
                "bebfff31-0fa3-4c7e-9487-79862ce35ce6"
                "d0618a1d-4087-4866-bb4f-3e5dcbea608c"]}])


(def modifiers [{:id "f0e5b012-8ab2-48bd-9008-b844c3184cc9", :price 35500, :name {:ru "Тигровые креветки"}}
                {:id "fc30a0c7-1ffd-4ee1-bc78-4d0b966aac88", :price 8500, :name {:ru "✔️ Говядина"}}
                {:id "67c97b05-d224-4a35-be62-94598ca08831", :price 8000, :name {:ru "Утка"}}
                {:id "c16bffd7-f0b8-46d0-b482-520d10e6112f", :price 7000, :name {:ru "Филе Курицы"}}
                {:id "10c1f575-d190-4e2c-b33b-ceefa6895a7f", :price 3500, :name {:ru "Тофу"}}
                {:id "7b0eddf9-24a5-4ab9-b93f-1c56ae7342a0", :price 5000, :name {:ru "Соевое мясо"}}
                {:id "4193af88-7211-42e1-b645-ef28c0190a75", :price 2000, :name {:ru "✔️ Томаты черри"}}
                {:id "1f0497f6-7c3f-4e93-8f89-ff75fbb1fe2b", :price 25500, :name {:ru "Морской коктейль"}}
                {:id "edd3da9a-beaa-467c-ad76-219998edc302", :price 25500, :name {:ru "Лосось"}}
                {:id "78b78f89-9906-449e-93c7-73e5920d41b4", :price 5000, :name {:ru "Грибы древесные"}}
                {:id "4303da6b-23bc-4179-8a49-724a51e18b8e", :price 1000, :name {:ru "✔️ Соевые ростки"}}
                {:id "653511eb-7904-4757-b550-a684ed390f0c", :price 1000, :name {:ru "Маш проросший"}}
                {:id "55e71919-7380-4605-a07d-6001b0d93adc", :price 1500, :name {:ru "Имбирь"}}
                {:id "ba321a99-86b9-4062-86ba-740387e9a24b", :price 2500, :name {:ru "Кукуруза"}}
                {:id "4468e484-b96d-4885-8744-13255453cc34", :price 1000, :name {:ru "✔️ Яйцо"}}
                {:id "56f20dac-ceab-447d-8092-63ff7d8f4e9f", :price 6000, :name {:ru "Грибы Шиитаки"}}
                {:id "c049238a-054c-4095-bc60-385150452f69", :price 0, :name {:ru "--"}}
                {:id "773d9e6d-8933-4df3-b5f3-2eb78f4f7dab", :price 5000, :name {:ru "✔️ Терияки"}}
                {:id "9098aeb6-07b0-401e-b27c-e2951729f7f7", :price 5000, :name {:ru "✔️ Якитори"}}
                {:id "6d3b9e6a-aa05-48a8-b768-f06b517954f2", :price 5000, :name {:ru "Кисло-сладкий"}}
                {:id "aa91bc5e-ddce-4a82-b7f7-5a118b0920e5", :price 5000, :name {:ru "Чили"}}
                {:id "ced88454-24c5-46f1-993f-a352cfaa2d28", :price 5000, :name {:ru "Пикантный"}}
                {:id "5b0ccf5b-84a8-40db-95e3-8c3879429f8d", :price 5000, :name {:ru "Кунг Пао"}}
                {:id "bebfff31-0fa3-4c7e-9487-79862ce35ce6", :price 5000, :name {:ru "Дасуан Чесночный "}}
                {:id "d0618a1d-4087-4866-bb4f-3e5dcbea608c", :price 0, :name {:ru "--"}}])


(defn get-modifier
  [id]
  (first (filter #(= id (:id %)) modifiers)))


(def modifiers-mock
  (map
    (fn [mg]
      (assoc
        mg
        :modifiers
        (map get-modifier (:modifiers mg))))
    modifier-groups))


(defn keywordize-json-fields
  [product]
  (-> product
      (cu/keywordize-field :category)
      (cu/keywordize-field :description)
      (cu/keywordize-field :name)))


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


(defn basket-cost-query
  [basket-id]
  {:select [[(hs/raw "coalesce(sum(products.price * basket_products.count), 0)") :cost]]
   :from   [:basket_products :products]
   :where  [:and
            [:= :basket_products.basket_id basket-id]
            [:= :products.id :basket_products.product_id]]})


(defn product-detail-state-query
  [basket-id]
  {:select    [:products.id
               :products.name
               :products.description
               :products.price
               :products.photo
               :products.thumbnail
               :products.energy
               :categories.emoji
               [:categories.name :category]
               [(basket-cost-query basket-id) :basket_cost]
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


(defn state-for-product-detail!
  [basket-id id]
  (->> (product-detail-state-by-id-query basket-id id)
       (jdbc/query db/db)
       (map keywordize-json-fields)
       (map #(assoc % :modifiers modifiers-mock))
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
  (->> (basket-cost-query basket-id)
       (hs/format)
       (jdbc/query db/db)
       (first)
       (:cost)))


(defn menu-state!
  [basket-id bot-id]
  {:categories  (categories-with-products! bot-id)
   :basket_cost (basket-cost! basket-id)})
