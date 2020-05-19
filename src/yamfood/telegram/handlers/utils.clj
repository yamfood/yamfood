(ns yamfood.telegram.handlers.utils
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.translation.core :refer [translate]]))


(def menu-step "menu")
(def phone-step "phone")
(def phone-confirmation-step "phone-confirmation")
(def browse-step "browse")
(def basket-step "basket")
(def order-confirmation-step "order-confirmation")
(def feedback-step "feedback")

(def cash-payment "cash")
(def card-payment "card")


(defn tid-from-update
  [update]
  (let [message (:message update)
        callback (:callback_query update)
        inline (:inline_query update)]
    (cond
      message (:id (:from message))
      callback (:id (:from callback))
      inline (:id (:from inline)))))


(defn fmt-values
  [amount]
  (str/replace (format "%,d" amount) "," " "))


(defn translated
  [lang map]
  (get map lang))


(defn more-button
  [lang state]
  (let [category (:category state)]
    (if category
      [{:text (str (:emoji state) " " (translated lang category)) :switch_inline_query_current_chat (:emoji state)}]
      [{:text (translate lang :more-button) :switch_inline_query_current_chat ""}])))


(defn navigation-buttons
  [lang state]
  [(more-button lang state)
   [{:text (translate lang :product-basket-button (fmt-values (:basket_cost state))) :callback_data "basket"}]
   [{:text (translate lang :product-menu-button) :callback_data "menu"}]])


(defn product-not-in-basket-markup
  [lang state]
  {:inline_keyboard
   (into
     [[{:text (translate lang :add-product-button) :callback_data (str "want/" (:id state))}]]
     (navigation-buttons lang state))})


(defn basket-product-controls
  [action-prefix product-id count]
  [{:text "-" :callback_data (str action-prefix "-/" product-id)}
   {:text (str count) :callback_data "nothing"}
   {:text "+" :callback_data (str action-prefix "+/" product-id)}])


(defn product-in-basket-markup
  [lang state]
  {:inline_keyboard
   (into
     [(basket-product-controls "detail" (:id state) (:count_in_basket state))]
     (navigation-buttons lang state))})


;{:count_in_basket 0,
; :description {},
; :emoji "🥞",
; :category {:ru "Блинчики"},
; :name {:ru "Блинчики с ананасом и рикоттой!"},
; :energy 393,
; :modifiers ({:required true,
;              :modifiers ({:id "f0e5b012-8ab2-48bd-9008-b844c3184cc9", :price 35500, :name {:ru "Тигровые креветки"}}
;                          {:id "fc30a0c7-1ffd-4ee1-bc78-4d0b966aac88", :price 8500, :name {:ru "Говядина"}}
;                          {:id "67c97b05-d224-4a35-be62-94598ca08831", :price 8000, :name {:ru "Утка"}}
;                          {:id "c16bffd7-f0b8-46d0-b482-520d10e6112f", :price 7000, :name {:ru "Филе Курицы"}}
;                          {:id "10c1f575-d190-4e2c-b33b-ceefa6895a7f", :price 3500, :name {:ru "Тофу"}}
;                          {:id "7b0eddf9-24a5-4ab9-b93f-1c56ae7342a0", :price 5000, :name {:ru "Соевое мясо"}}
;                          {:id "4193af88-7211-42e1-b645-ef28c0190a75", :price 2000, :name {:ru "Томаты черри"}}
;                          {:id "1f0497f6-7c3f-4e93-8f89-ff75fbb1fe2b", :price 25500, :name {:ru "Морской коктейль"}}
;                          {:id "edd3da9a-beaa-467c-ad76-219998edc302", :price 25500, :name {:ru "Лосось"}}
;                          {:id "78b78f89-9906-449e-93c7-73e5920d41b4", :price 5000, :name {:ru "Грибы древесные"}}
;                          {:id "4303da6b-23bc-4179-8a49-724a51e18b8e", :price 1000, :name {:ru "Соевые ростки"}}
;                          {:id "653511eb-7904-4757-b550-a684ed390f0c", :price 1000, :name {:ru "Маш проросший"}}
;                          {:id "55e71919-7380-4605-a07d-6001b0d93adc", :price 1500, :name {:ru "Имбирь"}}
;                          {:id "ba321a99-86b9-4062-86ba-740387e9a24b", :price 2500, :name {:ru "Кукуруза"}}
;                          {:id "4468e484-b96d-4885-8744-13255453cc34", :price 1000, :name {:ru "Яйцо"}}
;                          {:id "56f20dac-ceab-447d-8092-63ff7d8f4e9f", :price 6000, :name {:ru "Грибы Шиитаки"}}
;                          {:id "c049238a-054c-4095-bc60-385150452f69", :price 0, :name {:ru "--"}})}
;             {:required true,
;              :modifiers ({:id "773d9e6d-8933-4df3-b5f3-2eb78f4f7dab", :price 5000, :name {:ru "Терияки"}}
;                          {:id "9098aeb6-07b0-401e-b27c-e2951729f7f7", :price 5000, :name {:ru "Якитори"}}
;                          {:id "6d3b9e6a-aa05-48a8-b768-f06b517954f2", :price 5000, :name {:ru "Кисло-сладкий"}}
;                          {:id "aa91bc5e-ddce-4a82-b7f7-5a118b0920e5", :price 5000, :name {:ru "Чили"}}
;                          {:id "ced88454-24c5-46f1-993f-a352cfaa2d28", :price 5000, :name {:ru "Пикантный"}}
;                          {:id "5b0ccf5b-84a8-40db-95e3-8c3879429f8d", :price 5000, :name {:ru "Кунг Пао"}}
;                          {:id "bebfff31-0fa3-4c7e-9487-79862ce35ce6", :price 5000, :name {:ru "Дасуан Чесночный "}}
;                          {:id "d0618a1d-4087-4866-bb4f-3e5dcbea608c", :price 0, :name {:ru "--"}})}),
; :thumbnail "https://ucarecdn.com/6bf64dfa-6aca-48d1-9365-ab8f2a9fd4f0/-/resize/x256/-/format/auto/",
; :photo "https://ucarecdn.com/6bf64dfa-6aca-48d1-9365-ab8f2a9fd4f0/-/resize/x1024/-/format/auto/",
; :id 22,
; :basket_cost 50100,
; :price 21100}


(defn constructable-product-markup
  [lang state]
  {:inline_keyboard
   (into
     [[{:text (translate lang :construct-product-button) :callback_data (str "construct/" (:id state) "/1")}]]
     (navigation-buttons lang state))})


(defn product-detail-markup
  [lang state]
  (let [constructable? (some true? (map :required (:modifiers state)))
        count-in-basket (:count_in_basket state)]
    (if constructable?
      (constructable-product-markup lang state)
      (if (= count-in-basket 0)
        (product-not-in-basket-markup lang state)
        (product-in-basket-markup lang state)))))


(defn callback-action
  [callback-data]
  (first (str/split callback-data #"/")))


(defn callback-params
  [callback-data]
  (drop 1 (str/split callback-data #"/")))


(defn chat-id
  "Use when you don't know input update type"
  [update]
  (let [message (:message update)
        inline (:inline_query update)
        query (:callback_query update)]
    (cond
      message (:id (:from message))
      inline (:id (:from inline))
      query (:id (:from query)))))


(defn update-type
  [update]
  (let [message (:message update)
        callback-query (:callback_query update)
        inline-query (:inline_query update)]
    (cond
      message :message
      callback-query :callback_query
      inline-query :inline_query)))


(def map-url
  (format "https://%s.herokuapp.com/regions"
          (or (env :app-name) "test")))


(defn parse-int [s]
  (try
    (let [result (read-string s)]
      (if (number? result)
        result
        nil))
    (catch Exception e
      nil)))


(defn order-products-text
  ([lang products]
   (doall
     (map
       #(format (str e/food-emoji " %d x %s\n")
                (:count %)
                (translated lang (:name %)))
       products))))


(defn text-from-address
  [address]
  (let [display-name (:display_name address)]
    (str/join ", " (drop-last 3 (str/split display-name #", ")))))


(defn utm
  [update]
  (let [message (:message update)
        text (:text message)]
    (second (str/split text #" "))))
