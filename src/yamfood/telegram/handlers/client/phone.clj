(ns yamfood.telegram.handlers.client.phone
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [yamfood.core.sms.core :as sms]
    [yamfood.core.specs.core :as cs]
    [yamfood.telegram.dispatcher :as d]
    [yamfood.core.clients.core :as clients]
    [yamfood.telegram.handlers.utils :as u]
    [yamfood.telegram.translation.core :refer [translate]]
    [yamfood.core.baskets.core :as baskets]))


(defn generate-confirmation-code
  [digits-count]
  (str/join (map (fn [_]
                   (str (+ 1 (rand-int 9))))
                 (range digits-count))))


(defn request-phone-markup
  [lang]
  {:resize_keyboard true
   :keyboard        [[{:text            (translate lang :send-contact-button)
                       :request_contact true}]]})


(defn request-phone
  [ctx]
  (let [update (:update ctx)
        chat-id (u/chat-id update)
        query (:callback_query update)
        client (:client ctx)]
    (merge
      {:run       {:function clients/update-payload!
                   :args     [(:id client)
                              (assoc
                                (:payload client)
                                :step u/phone-step)]}
       :send-text {:chat-id chat-id
                   :options {:reply_markup (request-phone-markup (:lang ctx))
                             :parse_mode   "markdown"}
                   :text    (translate (:lang ctx) :request-contact-message)}}
      (when query
        {:delete-message {:chat-id    chat-id
                          :message-id (:message_id (:message query))}}))))


(defn phone-confirmation-markup
  [lang]
  {:inline_keyboard
   [[{:text (translate lang :change-phone-button) :callback_data "request-phone"}]]})


(defn get-phone
  [update]
  (let [message (:message update)
        contact (:contact message)
        phone (u/parse-int (if contact
                             (:phone_number contact)
                             (:text message)))]
    (when (s/valid? ::cs/phone phone)
      phone)))


(defn phone-handler
  ([ctx]
   (let [update (:update ctx)
         lang (:lang ctx)
         client (:client ctx)
         bot (:bot ctx)
         chat-id (u/chat-id update)
         phone (get-phone update)
         code (generate-confirmation-code 5)
         bot-name (:name bot)]
     (cond
       (not phone)
       {:send-text {:chat-id chat-id
                    :text    (translate lang :invalid-phone-message)}}

       :else
       ;; then proceed with confirmation
       {:run       [{:function sms/create!
                     :args     [phone (translate lang :confirmation-code bot-name code)]}
                    {:function clients/update-payload!
                     :args     [(:id client) (-> (:payload client)
                                                 (assoc :unconfirmed-phone phone)
                                                 (assoc :step u/phone-confirmation-step)
                                                 (assoc :code code))]}]
        :send-text [{:chat-id chat-id
                     :options {:parse_mode   "markdown"
                               :reply_markup {:remove_keyboard true}}
                     :text    (translate lang :accepted)}
                    {:chat-id chat-id
                     :options {:parse_mode   "markdown"
                               :reply_markup (phone-confirmation-markup (:lang ctx))}
                     :text    (translate lang :request-code-message phone)}]}))))



(defn confirm-phone-handler
  ([ctx]
   {:run {:function   clients/client-with-bot-id-and-phone!
          :args       [(get-in ctx [:bot :id])
                       (get-in ctx [:client :payload :unconfirmed-phone])]
          :next-event :c/confirm-phone}})
  ([ctx conflicting-client]
   (let [update (:update ctx)
         lang (:lang ctx)
         chat-id (u/chat-id update)
         client (:client ctx)
         phone (:unconfirmed-phone (:payload client))
         code (get-in client [:payload :code])
         text (:text (:message update))
         valid? (= text code)]
     (cond
       (not valid?)
       {:send-text {:chat-id chat-id
                    :text    (translate lang :incorrect-code-message)}}

       ;; If no conflicting-client
       (nil? conflicting-client)
       ;; then confirm
       {:run       {:function clients/update-phone!
                    :args     [(:id client) phone]}
        :send-text {:chat-id chat-id
                    :text    (translate lang :phone-confirmed-message)}
        :dispatch  {:args [:c/menu]}}

       ;; if conflicting-client is external
       (nil? (:tid conflicting-client))
       ;; then swap tids, change external payload and current basket owner to conflicting-client, and confirm
       {:run       [{:function clients/update-tid!
                     :args     [(:id client) nil]}
                    {:function clients/update-tid!
                     :args     [(:id conflicting-client) (:tid client)]}
                    {:function clients/update-payload!
                     :args     [(:id conflicting-client) (:payload client)]}
                    {:function baskets/update-owner!
                     :args     [(:basket_id (:client ctx)) (:id conflicting-client)]}]
        :send-text {:chat-id chat-id
                    :text    (translate lang :phone-confirmed-message)}
        :dispatch  {:args [:c/menu]}}

       ;; if conflicting-client is same as current
       (= (:id conflicting-client) (:id client))
       ;; then confirm
       {:send-text {:chat-id chat-id
                    :text    (translate lang :phone-confirmed-message)}
        :dispatch  {:args [:c/menu]}}


       ;; If conflicting-client is not external
       (some? (:tid conflicting-client))
       ;; then unconfirm external phone and confirm current
       {:run       [{:function clients/update-phone!
                     :args     [(:id conflicting-client) nil]}
                    {:function clients/update-phone!
                     :args     [(:id client) phone]}]
        :send-text {:chat-id chat-id
                    :text    (translate lang :phone-confirmed-message)}
        :dispatch  {:args [:c/menu]}}

       :else (throw (ex-info
                      "Unexpected input, in confirm-phone-handler"
                      {:conflicting_client_id (:id conflicting-client)
                       :client_id             (:id client)}))))))


(defn contact-handler
  [ctx]
  (let [step (:step (:payload (:client ctx)))
        chat-id (u/chat-id (:update ctx))]
    (if (= step u/phone-step)
      {:dispatch {:args [:c/phone]}}
      {:send-text {:chat-id chat-id
                   :text    step}})))


(d/register-event-handler!
  :c/contact
  contact-handler)


(d/register-event-handler!
  :c/request-phone
  request-phone)


(d/register-event-handler!
  :c/phone
  phone-handler)


(d/register-event-handler!
  :c/confirm-phone
  confirm-phone-handler)
