(ns yamfood.telegram.handlers.client.order
  (:require
    [yamfood.core.orders.core :as o]
    [yamfood.core.orders.core :as ord]
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
                                        :update   (:update ctx)}}}))))


(defn order-confirmation-markup
  [lang order-state]
  (let [payment (get-in order-state [:client :payload :payment])]
    {:inline_keyboard
     [[{:text e/location-emoji :callback_data "request-location"}
       (cond
         (= payment u/card-payment) {:text e/card-emoji :callback_data "switch-payment-type"}
         :else {:text e/cash-emoji :callback_data "switch-payment-type"})
       {:text e/comment-emoji :callback_data "change-comment"}]
      [{:text (translate lang :oc-basket-button) :callback_data "basket"}]
      [{:text (translate lang :oc-create-order-button) :callback_data "create-order"}]]}))


(defn pre-order-text
  [lang order-state]
  (translate lang :oc-message
             (u/fmt-values (:total_cost (:basket order-state)))
             (translate lang (keyword (or (get-in order-state [:client :payload :payment])
                                          "cash")))
             (or (:comment (:payload (:client order-state)))
                 (translate lang :oc-empty-comment-text))
             (u/text-from-address
               (get-in order-state [:client :payload :location :address]))))


(defn order-detail-handler
  [ctx order-state]
  (let [update (:update ctx)
        lang (:lang ctx)
        chat-id (u/chat-id update)]
    {:send-text {:chat-id chat-id
                 :text    (pre-order-text lang order-state)
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
         chat-id (:id (:from query))
         message-id (:message_id (:message query))]
     {:edit-message {:chat-id    chat-id
                     :message-id message-id
                     :text       (pre-order-text lang order-state)
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
                                     :update   (:update ctx)}}
     :answer-callback {:callback_query_id (:id query)
                       :text              " "}}))


(defn create-order-handler
  [ctx]
  (let [update (:update ctx)
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
        card? (= payment u/card-payment)]
    (merge
      {:run            [(merge {:function ord/create-order!
                                :args     [basket-id location
                                           (u/text-from-address address)
                                           comment payment]}
                               (if card?
                                 {:next-event :c/send-invoice}
                                 {:next-event :c/active-order}))
                        (when (not card?)
                          {:function bsk/clear-basket!
                           :args     [basket-id]})]
       :delete-message {:chat-id    chat-id
                        :message-id message-id}})))


(def write-comment-text "Напишите свой комментарий к заказу")
(defn change-comment-handler
  [ctx]
  (let [update (:update ctx)
        query (:callback_query update)
        chat-id (:id (:from query))
        message-id (:message_id (:message query))]
    {:send-text      {:chat-id chat-id
                      :text    write-comment-text
                      :options {:reply_markup {:force_reply true}}}
     :delete-message {:chat-id    chat-id
                      :message-id message-id}}))


(defn active-order-text
  [lang order]
  (translate lang :active-order-message
             (:id order)
             (apply str (u/order-products-text (:products order)))
             (u/fmt-values (:total_cost order))
             (translate lang (keyword (:payment order)))))


(defn product-price
  [product]
  {:label  (format "%d x %s" (:count product) (:name product))
   :amount (* (:price product) (:count product) 100)})


(defn order-prices
  [order]
  (map product-price (:products order)))


(defn active-order
  [ctx order]
  (if (int? order)
    {:run {:function   o/order-by-id!
           :args       [order]
           :next-event :c/active-order}}
    (let [update (:update ctx)
          lang (:lang ctx)
          chat-id (u/chat-id update)]
      {:send-text {:chat-id chat-id
                   :text    (active-order-text lang order)
                   :options {:parse_mode "markdown"}}})))


(defn invoice-description
  [order]
  (format (str (apply str (u/order-products-text (:products order))))))


(defn invoice-reply-markup
  [lang]
  {:inline_keyboard [[{:text (translate lang :pay-button) :pay true}]]})


(defn send-invoice
  [ctx order]
  (if (int? order)
    {:run {:function   o/order-by-id!
           :args       [order]
           :next-event :c/send-invoice}}
    (let [update (:update ctx)
          lang (:lang ctx)
          chat-id (u/chat-id update)
          message-id (:message_id (:message (:callback_query update)))]
      {:send-invoice   {:chat-id     chat-id
                        :title       (translate lang :invoice-title (:id order))
                        :description (invoice-description order)
                        :payload     {:order_id (:id order)}
                        :currency    "UZS"
                        :prices      (order-prices order)
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
  :c/order-confirmation-state
  order-confirmation-state)


(d/register-event-handler!
  :c/send-order-detail
  order-detail-handler)


(d/register-event-handler!
  :c/create-order
  create-order-handler)


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
