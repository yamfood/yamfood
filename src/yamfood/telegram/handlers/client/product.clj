(ns yamfood.telegram.handlers.client.product
  (:require
    [yamfood.utils :as utils]
    [clojure.data.json :as json]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as baskets]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.clients.core :as clients]
    [yamfood.core.products.core :as products]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn want-handler
  [ctx]
  (let [update (:update ctx)
        lang (:lang ctx)
        query (:callback_query update)
        client (:client ctx)
        callback-data (:data query)
        callback-params (u/callback-params callback-data)
        product-id (Integer. (first callback-params))]
    {:run             {:function   baskets/add-product-to-basket!
                       :args       [(:basket_id client) product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id query)
                       :text              (translate lang :added-to-basket-message)}}))


(defn detail-inc-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:client ctx))
        product-id (Integer. (first (u/callback-params callback-data)))]
    {:run             {:function   baskets/increment-product-in-basket!
                       :args       [basket-id product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn detail-dec-handler
  [ctx]
  (let [update (:update ctx)
        callback-query (:callback_query update)
        callback-data (:data callback-query)
        basket-id (:basket_id (:client ctx))
        product-id (Integer. (first (u/callback-params callback-data)))]
    {:run             {:function   baskets/decrement-product-in-basket!
                       :args       [basket-id product-id]
                       :next-event :c/update-markup}
     :answer-callback {:callback_query_id (:id callback-query)
                       :text              " "}}))


(defn update-detail-markup
  [ctx product-state]
  (let [update (:update ctx)
        query (:callback_query update)]
    {:edit-reply-markup {:chat_id      (:id (:from query))
                         :message_id   (:message_id (:message query))
                         :reply_markup (u/product-detail-markup
                                         (:lang ctx)
                                         product-state)}}))


(defn product-caption
  [lang product]
  (let [constructable? (some true? (map :required (:modifiers product)))
        description (u/translated lang (:description product))
        price (u/fmt-values (:price product))
        price (if constructable? (str "от " price) price)]
    (translate lang
               :product-caption
               {:name        (u/translated lang (:name product))
                :description (if (empty? description)
                               nil
                               (str description "\n\n"))
                :price       price
                :energy      (u/fmt-values (:energy product))})))


(defn product-detail-options
  [lang product-state]
  {:caption      (product-caption lang product-state)
   :parse_mode   "markdown"
   :reply_markup (json/write-str (u/product-detail-markup lang product-state))})


(defn product-detail-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)]
     {:run {:function   products/state-for-product-detail!
            :args       [(:basket_id (:client ctx))
                         (u/parse-int (:text message))]
            :next-event :c/text}}))
  ([ctx product-detail-state]
   (let [update (:update ctx)
         lang (:lang ctx)
         client (:client ctx)
         message (:message update)
         chat (:chat message)
         chat-id (:id chat)]
     (if product-detail-state
       {:send-photo     {:chat-id chat-id
                         :options (product-detail-options lang product-detail-state)
                         :photo   (:photo product-detail-state)}
        :delete-message {:chat-id    chat-id
                         :message-id (:message_id message)}
        :run            {:function clients/update-payload!
                         :args     [(:id client)
                                    (assoc (:payload client) :step u/browse-step)]}}

       {:dispatch {:args [:c/no-product-text]}}))))


(defn modifiers-reducer
  [lang product-id step selected-modifiers]
  (fn [r modifier]
    (let [current (last r)
          c (count current)
          selected? (utils/in? selected-modifiers (:id modifier))
          text (u/translated lang (:name modifier))
          text (if selected? (str "✔️ " text) text)
          next {:text text :callback_data (format "construct/%s/%s/%s"
                                                  product-id
                                                  step
                                                  (:id modifier))}]
      (cond
        (= c 1) (conj (pop r) [(first current) next])
        :else (conj r [next])))))


(defn modifiers-markup
  [lang product-id state step]
  (let [groups (:modifiers state)
        current-group (nth groups (dec step))
        modifiers (:modifiers current-group)
        callback-data (if (= step (count groups))
                        (str "construct-finish/" product-id)
                        (str "construct/" product-id "/" (inc step)))]
    {:inline_keyboard
     (into (reduce
             (modifiers-reducer lang product-id step (:selected-modifiers state))
             []
             modifiers)
           [[{:text "Дальше" :callback_data callback-data}]])}))


(defn new-modifiers
  [selected-modifiers modifier]
  (if (utils/in? selected-modifiers modifier)
    (filter #(not (= % modifier)) selected-modifiers)
    (conj selected-modifiers modifier)))


(defn construct-product-handler
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         product-id (u/parse-int (first (u/callback-params (:data query))))]
     {:run {:function   products/state-for-product-detail!
            :args       [(:basket_id (:client ctx))
                         product-id]
            :next-event :c/construct}}))
  ([ctx state]
   (let [client (:client ctx)
         update (:update ctx)
         query (:callback_query update)
         product-id (u/parse-int (first (u/callback-params (:data query))))
         step (u/parse-int (second (u/callback-params (:data query))))
         modifier-id (nth (u/callback-params (:data query)) 2 nil)
         current-modifiers (get-in client [:payload :modifiers (keyword (str product-id))] [])
         new-modifiers (new-modifiers current-modifiers modifier-id)
         state (assoc state :selected-modifiers new-modifiers)]
     (merge
       {:edit-reply-markup {:chat_id      (u/chat-id update)
                            :message_id   (:message_id (:message query))
                            :reply_markup (modifiers-markup
                                            :ru
                                            product-id
                                            state
                                            step)}
        :answer-callback   {:callback_query_id (:id query)
                            :text              " "}}
       (when modifier-id
         {:run {:function clients/update-payload!
                :args     [(:id client)
                           (-> (:payload client)
                               (assoc-in [:modifiers (keyword (str product-id))]
                                         new-modifiers))]}})))))


(defn construct-finish-handler
  [ctx]
  (let [
        client (:client ctx)
        update (:update ctx)
        query (:callback_query update)
        product-id (u/parse-int (first (u/callback-params (:data query))))
        modifiers (get-in client [:payload :modifiers (keyword (str product-id))])]
    {:run             {:function baskets/add-product-to-basket!
                       :args     [(:basket_id client) product-id modifiers]}
     :answer-callback {:callback_query_id (:id query)
                       :show_alert        true
                       :text              "Блюдо успешно собрано и добавлено в корзину!"}
     :dispatch        {:args [:c/menu]}}))


(d/register-event-handler!
  :c/text
  product-detail-handler)


(d/register-event-handler!
  :c/detail-want
  want-handler)


(d/register-event-handler!
  :c/construct
  construct-product-handler)


(d/register-event-handler!
  :c/construct-finish
  construct-finish-handler)


(d/register-event-handler!
  :c/detail-inc
  detail-inc-handler)


(d/register-event-handler!
  :c/detail-dec
  detail-dec-handler)


(d/register-event-handler!
  :c/update-markup
  update-detail-markup)
