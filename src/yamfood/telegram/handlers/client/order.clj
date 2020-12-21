(ns yamfood.telegram.handlers.client.order
  (:require
    [clojure.string :as str]
    [environ.core :refer [env]]
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]
    [yamfood.core.kitchens.core :as k]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.baskets.core :as bsk]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.emojies :as e]
    [yamfood.telegram.handlers.client.core :as c]
    [yamfood.telegram.translation.core :refer [translate]]))


(defn order-confirmation-state
  [ctx]
  (let [client (:client ctx)]
    {:run {:function   bsk/order-confirmation-state!
           :args       [(:basket_id client)]
           :next-event :c/send-order-detail}}))


(defn to-order-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        client (:client ctx)
        message-id (:message_id (:message query))]
    (into
      {:delete-message {:chat-id    chat-id
                        :message-id message-id}
       :run            {:function clients/update-payload!
                        :args     [(:id client)
                                   (assoc (:payload client) :step u/order-confirmation-step)]}}
      (cond
        (:location (:payload client)) {:dispatch {:args [:c/order-confirmation-state]}}
        :else {:dispatch {:args        [:c/request-location]
                          :rebuild-ctx {:function c/build-ctx!
                                        :update   (:update ctx)
                                        :token    (:token ctx)}}}))))

(defn confirm-location
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        client (:client ctx)
        lang (:lang ctx)
        address (u/text-from-address
                  (get-in client [:payload :location :address]))
        message-id (:message_id (:message query))]
    {:delete-message {:chat-id    chat-id
                      :message-id message-id}
     :send-text {:chat-id chat-id
                 :text    (translate lang :lc-message-text address)
                 :options {:parse_mode   "markdown"
                           :reply_markup {:inline_keyboard
                                          [[{:text          (translate lang :lc-yes)
                                             :callback_data "to-order"}
                                            {:text          (translate lang :lc-no)
                                             :callback_data "request-location"}]]}}}
     :run       {:function clients/update-payload!
                 :args     [(:id client)
                            (assoc (:payload client) :step u/order-confirmation-step)]}}))


(defn payment-buttons
  [lang current-payment]
  (let [emoji "☑️ "]
    [{:text          (str (when (= current-payment u/cash-payment) emoji)
                          (translate lang :cash))
      :callback_data (if (= current-payment u/cash-payment)
                       "nothing"
                       "switch-payment-type")}
     {:text          (str (when (= current-payment u/card-payment) emoji)
                          (translate lang :card))
      :callback_data (if (= current-payment u/card-payment)
                       "nothing"
                       "switch-payment-type")}]))


(defn order-confirmation-markup
  [lang order-state]
  (let [payment (get-in order-state [:client :payload :payment])]
    {:inline_keyboard
     [(payment-buttons lang payment)
      [{:text (translate lang :oc-location-button) :callback_data "request-location"}]
      [{:text (translate lang :oc-comment-button) :callback_data "send-last-comments"}]
      [{:text (translate lang :oc-basket-button) :callback_data "basket"}]
      [{:text (translate lang :oc-create-order-button) :callback_data "create-order"}]]}))


(defn pre-order-text
  [lang params order-state]
  (let [price (:total_cost (:basket order-state))
        products (:products (:basket order-state))
        delivery (if (every? false? (map :is_delivery_free products))
                   (:delivery-cost params)
                   0)]
    (translate lang :oc-message
               {:price    (u/fmt-values price)
                :comment  (or (:comment (:payload (:client order-state)))
                              (translate lang :oc-empty-comment-text))
                :delivery (u/fmt-values delivery)
                :total    (u/fmt-values (+ delivery price))
                :address  (u/text-from-address
                            (get-in order-state [:client :payload :location :address]))})))


(defn order-detail-handler
  [ctx order-state]
  (let [update (:update ctx)
        params (:params ctx)
        lang (:lang ctx)
        chat-id (u/chat-id update)]
    {:send-text {:chat-id chat-id
                 :text    (pre-order-text lang params order-state)
                 :options {:reply_markup (order-confirmation-markup lang order-state)
                           :parse_mode   "markdown"}}}))


(defn update-order-confirmation-handler
  ([ctx]
   (let [client (:client ctx)]
     {:run {:function   bsk/order-confirmation-state!
            :args       [(:basket_id client)]
            :next-event :c/update-order-confirmation}}))
  ([ctx order-state]
   (let [query (:callback_query (:update ctx))
         lang (:lang ctx)
         params (:params ctx)
         chat-id (:id (:from query))
         message-id (:message_id (:message query))]
     {:edit-message {:chat-id    chat-id
                     :message-id message-id
                     :text       (pre-order-text lang params order-state)
                     :options    {:reply_markup (order-confirmation-markup lang order-state)
                                  :parse_mode   "markdown"}}})))


(defn switch-payment-type-handler
  [ctx]
  (let [query (:callback_query (:update ctx))
        client (:client ctx)
        payload (:payload client)
        payment (:payment payload)
        new-payment (cond
                      (or (= payment nil)
                          (= payment u/cash-payment)) u/card-payment
                      (= payment u/card-payment) u/cash-payment)]
    {:run             {:function clients/update-payload!
                       :args     [(:id client) (assoc payload :payment new-payment)]}
     :dispatch        {:args        [:c/update-order-confirmation]
                       :rebuild-ctx {:function c/build-ctx!
                                     :update   (:update ctx)
                                     :token    (:token ctx)}}
     :answer-callback {:callback_query_id (:id query)
                       :text              " "}}))


(defn create-order-handler
  ([ctx]
   (let [payload (:payload (:client ctx))
         location (:location payload)]
     {:run {:function   (fn [bot-id lng lat]
                          (let [kitchen (k/nearest-kitchen! bot-id lng lat)]
                            [kitchen (when kitchen
                                       (bsk/disabled-basket-products!
                                         (:basket_id (:client ctx))
                                         (:id kitchen)))]))
            :args       [(:id (:bot ctx))
                         (:longitude location)
                         (:latitude location)]
            :next-event :c/create-order}}))
  ([ctx [kitchen disabled-products]]
   (let [update (:update ctx)
         lang (:lang ctx)
         query (:callback_query update)
         chat-id (:id (:from query))
         message-id (:message_id (:message query))
         basket-id (:basket_id (:client ctx))
         payload (:payload (:client ctx))
         location (:location payload)
         payment (or (:payment payload)
                     u/cash-payment)
         comment (:comment payload)
         address (:address location)
         card? (= payment u/card-payment)
         delivery-cost (get-in ctx [:params :delivery-cost])]
     (if kitchen
       (if (empty? disabled-products)
         {:run            [(merge {:function ord/create-order!
                                   :args     [basket-id (:id kitchen) location
                                              (u/text-from-address address)
                                              comment payment delivery-cost]}
                                  (if card?
                                    {:next-event :c/send-invoice}
                                    {:next-event :c/active-order}))
                           (when (not card?)
                             {:function bsk/clear-basket!
                              :args     [basket-id]})]
          :delete-message {:chat-id    chat-id
                           :message-id message-id}}
         {:run             {:function bsk/remove-basket-products!
                            :args     [(map :id disabled-products)]}
          :answer-callback {:callback_query_id (:id query)
                            :show_alert        true
                            :text              (->> disabled-products
                                                    (map #(format
                                                            (str (:emoji %) " %d x %s")
                                                            (:count %)
                                                            (u/translated lang (:name %))))
                                                    (str/join "\n")
                                                    (str (translate lang :disabled-products-removed)))}
          :dispatch        {:args [:c/basket]}})
       {:answer-callback {:callback_query_id (:id query)
                          :show_alert        true
                          :text              (translate lang :all-kitchens-closed)}}))))


(defn send-last-comments-handler
  ([ctx]
   {:run {:function   ord/last-n-order-comments-by-client-id!
          :args       [(:id (:client ctx)) (or (:number-last-comments env) 5)]
          :next-event :c/send-last-comments}})
  ([ctx last-orders]
   (let [update (:update ctx)
         query (:callback_query update)
         chat-id (:id (:from query))
         client (:client ctx)
         lang (:lang ctx)
         message-id (:message_id (:message query))]
     {:run            {:function clients/update-payload!
                       :args     [(:id client) (assoc (:payload client) :step u/comment-step)]}
      :send-text      {:chat-id    chat-id
                       :text       (translate lang :oc-write-comment-text)
                       :options    {:reply_markup
                                    {:inline_keyboard (conj
                                                        (mapv (fn [order]
                                                                [{:text          (:comment order)
                                                                  :callback_data (str "set-comment-from-order/"
                                                                                      (:id order))}])
                                                              last-orders)
                                                        [{:text          (translate lang :oc-comment-back-button)
                                                          :callback_data "to-order"}])}}
                       :next-event :c/pre-save-message-id}
      :delete-message {:chat-id    chat-id
                       :message-id message-id}})))


(defn set-comment-from-order
  ([ctx]
   (let [update (:update ctx)
         query (:callback_query update)
         params (u/callback-params (:data query))
         order-id (u/parse-int (first params))]
     {:run {:function   ord/order-by-id!
            :args       [order-id]
            :next-event :c/set-comment-from-order}}))
  ([ctx order]
   (let [update (:update ctx)
         query (:callback_query update)
         client (:client ctx)
         chat-id (:id (:from query))
         message-id (:message_id (:message query))]
     {:run            {:function clients/update-payload!
                       :args     [(:id client) (assoc (:payload client)
                                                 :step :order-confirmation
                                                 :comment (:comment order))]}
      :dispatch       {:args [:c/order-confirmation-state]}
      :delete-message {:chat-id    chat-id
                       :message-id message-id}})))




(defn change-comment-handler
  ([ctx]
   (let [update (:update ctx)
         message (:message update)
         message-id (:message_id message)
         text (:text message)
         client (:client ctx)
         last-message-id (:last_message_id (:payload (:client ctx)))]
     {:run            {:function clients/update-payload!
                       :args     [(:id client) (assoc
                                                 (:payload client)
                                                 :comment
                                                 text)]}
      :dispatch       {:args [:c/order-confirmation-state]}
      :delete-message [{:chat-id    (:tid client)
                        :message-id last-message-id}
                       {:chat-id    (:tid client)
                        :message-id message-id}]})))


(defn active-order-text
  [lang order]
  (translate lang :active-order-message
             (:id order)
             (apply str (u/order-products-text lang (:products order)))
             (u/fmt-values (+ (:total_cost order) (:delivery_cost order)))
             (translate lang (keyword (:payment order)))))


(defn product-price
  [lang]
  (fn [product]
    {:label  (format "%d x %s" (:count product) (u/translated lang (:name product)))
     :amount (* (:price product) (:count product) 100)}))


(defn order-prices
  [lang params order]
  (let [products (:products order)
        delivery-cost (if (every? false? (map :is_delivery_free products))
                        (:delivery-cost params)
                        0)]
    (into (map (product-price lang) (:products order))
          [{:label  "Доставка"
            :amount (* delivery-cost 100)}])))


(defn active-order
  [ctx order]
  (cond
    (nil? order) {}
    (int? order) {:run {:function   o/order-by-id!
                        :args       [order]
                        :next-event :c/active-order}}
    (map? order) (let [update (:update ctx)
                       lang (:lang ctx)
                       chat-id (u/chat-id update)]
                   {:send-text {:chat-id chat-id
                                :text    (active-order-text lang order)
                                :options {:parse_mode "markdown"}}})))


(defn invoice-description
  [lang order]
  (format (str (apply str (u/order-products-text lang (:products order))))))


(defn invoice-reply-markup
  [lang]
  {:inline_keyboard [[{:text (translate lang :pay-button)
                       :pay  true}]
                     [{:text          (translate lang :invoice-cancel-button)
                       :callback_data "basket"}]]})


(defn send-invoice
  [ctx order]
  (cond
    (nil? order) {}
    (int? order) {:run {:function   o/order-by-id!
                        :args       [order]
                        :next-event :c/send-invoice}}
    (map? order) (let [update (:update ctx)
                       lang (:lang ctx)
                       chat-id (u/chat-id update)
                       message-id (:message_id (:message (:callback_query update)))]
                   {:send-invoice   {:chat-id     chat-id
                                     :title       (translate lang :invoice-title (:id order))
                                     :description (invoice-description lang order)
                                     :payload     {:order_id (:id order)}
                                     :currency    "UZS"
                                     :prices      (order-prices lang (:params ctx) order)
                                     :options     {:reply_markup (invoice-reply-markup lang)}}
                    :delete-message {:chat-id    chat-id
                                     :message-id message-id}})))


(defn cancel-invoice-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:dispatch       {:args [:c/active-order]}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(d/register-event-handler!
  :c/to-order
  to-order-handler)


(d/register-event-handler!
  :c/confirm-location
  confirm-location)


(d/register-event-handler!
  :c/order-confirmation-state
  order-confirmation-state)


(d/register-event-handler!
  :c/send-order-detail
  order-detail-handler)


(d/register-event-handler!
  :c/create-order
  create-order-handler)


(d/register-event-handler!
  :c/send-last-comments
  send-last-comments-handler)


(d/register-event-handler!
  :c/set-comment-from-order
  set-comment-from-order)


(d/register-event-handler!
  :c/change-comment
  change-comment-handler)


(d/register-event-handler!
  :c/switch-payment-type
  switch-payment-type-handler)


(d/register-event-handler!
  :c/update-order-confirmation
  update-order-confirmation-handler)


(d/register-event-handler!
  :c/active-order
  active-order)


(d/register-event-handler!
  :c/send-invoice
  send-invoice)


(d/register-event-handler!
  :c/cancel-invoice
  cancel-invoice-handler)
