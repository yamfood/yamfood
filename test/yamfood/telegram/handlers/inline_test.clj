(ns yamfood.telegram.handlers.inline-test
  (:require [clojure.test :refer :all]
            [yamfood.telegram.handlers.inline :as inline]))


(def upd
  {:update_id    435323082,
   :inline_query {:id     "340271653120582092",
                  :from   {:id            79225668,
                           :is_bot        false,
                           :first_name    "Рустам",
                           :last_name     "Бабаджанов",
                           :username      "kensay",
                           :language_code "ru"},
                  :query  "",
                  :offset ""}})


(def ctx
  {:token          "488312680:AAGsKHKufV9TQEAB8-g6INps-W82G_noRP8",
   :payments-token "371317599:TEST:79225668",
   :user           {:id        10,
                    :phone     998909296339,
                    :tid       79225668,
                    :location  {:longitude 34.74037, :latitude 32.020955},
                    :comment   "Test",
                    :basket_id 4}})


(def products
  [{:id        1,
    :name      "Глазунья с болгарским перцем и паштетом",
    :price     15000,
    :photo     "https://i.ibb.co/T8TRNm3/image.png",
    :thumbnail "https://i.ibb.co/T8TRNm3/image.png",
    :energy    360}
   {:id        2,
    :name      "Рисовая каша с ежевикой",
    :price     13800,
    :photo     "https://i.ibb.co/cFcyvGD/13-800.png",
    :thumbnail "https://i.ibb.co/cFcyvGD/13-800.png",
    :energy    540}
   {:id        3,
    :name      "Скрембл с авокадо и помидорами",
    :price     22000,
    :photo     "https://i.ibb.co/NF7s1GV/22.png",
    :thumbnail "https://i.ibb.co/NF7s1GV/22.png",
    :energy    440}
   {:id        4,
    :name      "Сырники со сметаной и джемом",
    :price     15000,
    :photo     "https://i.ibb.co/B4dYM6c/17-000.png",
    :thumbnail "https://i.ibb.co/B4dYM6c/17-000.png",
    :energy    360}
   {:id        5,
    :name      "Глазунья с болгарским перцем и паштетом",
    :price     15000,
    :photo     "https://i.ibb.co/T8TRNm3/image.png",
    :thumbnail "https://i.ibb.co/T8TRNm3/image.png",
    :energy    540}
   {:id        6,
    :name      "Рисовая каша с ежевикой",
    :price     13800,
    :photo     "https://i.ibb.co/cFcyvGD/13-800.png",
    :thumbnail "https://i.ibb.co/cFcyvGD/13-800.png",
    :energy    440}
   {:id        7,
    :name      "Скрембл с авокадо и помидорами",
    :price     22000,
    :photo     "https://i.ibb.co/NF7s1GV/22.png",
    :thumbnail "https://i.ibb.co/NF7s1GV/22.png",
    :energy    360}
   {:id        8,
    :name      "Сырники со сметаной и джемом",
    :price     15000,
    :photo     "https://i.ibb.co/B4dYM6c/17-000.png",
    :thumbnail "https://i.ibb.co/B4dYM6c/17-000.png",
    :energy    540}
   {:id        9,
    :name      "Свежесваренный кофе",
    :price     11000,
    :photo     "https://i.ibb.co/C8nLScV/11-000.png",
    :thumbnail "https://i.ibb.co/C8nLScV/11-000.png",
    :energy    440}
   {:id        10,
    :name      "Яблочный фреш",
    :price     9900,
    :photo     "https://i.ibb.co/hXGZv1t/9-900.png",
    :thumbnail "https://i.ibb.co/hXGZv1t/9-900.png",
    :energy    360}])


(def inline-handler-results
  {:answer-inline {:inline-query-id "340271653120582092",
                   :options         {:cache_time 0},
                   :results         [{:type                  "article",
                                      :id                    1,
                                      :input_message_content {:message_text "Глазунья с болгарским перцем и паштетом"},
                                      :title                 "Глазунья с болгарским перцем и паштетом",
                                      :description           "15 000 сум, 360 кКал",
                                      :thumb_url             "https://i.ibb.co/T8TRNm3/image.png"}
                                     {:type                  "article",
                                      :id                    2,
                                      :input_message_content {:message_text "Рисовая каша с ежевикой"},
                                      :title                 "Рисовая каша с ежевикой",
                                      :description           "13 800 сум, 540 кКал",
                                      :thumb_url             "https://i.ibb.co/cFcyvGD/13-800.png"}
                                     {:type                  "article",
                                      :id                    3,
                                      :input_message_content {:message_text "Скрембл с авокадо и помидорами"},
                                      :title                 "Скрембл с авокадо и помидорами",
                                      :description           "22 000 сум, 440 кКал",
                                      :thumb_url             "https://i.ibb.co/NF7s1GV/22.png"}
                                     {:type                  "article",
                                      :id                    4,
                                      :input_message_content {:message_text "Сырники со сметаной и джемом"},
                                      :title                 "Сырники со сметаной и джемом",
                                      :description           "15 000 сум, 360 кКал",
                                      :thumb_url             "https://i.ibb.co/B4dYM6c/17-000.png"}
                                     {:type                  "article",
                                      :id                    5,
                                      :input_message_content {:message_text "Глазунья с болгарским перцем и паштетом"},
                                      :title                 "Глазунья с болгарским перцем и паштетом",
                                      :description           "15 000 сум, 540 кКал",
                                      :thumb_url             "https://i.ibb.co/T8TRNm3/image.png"}
                                     {:type                  "article",
                                      :id                    6,
                                      :input_message_content {:message_text "Рисовая каша с ежевикой"},
                                      :title                 "Рисовая каша с ежевикой",
                                      :description           "13 800 сум, 440 кКал",
                                      :thumb_url             "https://i.ibb.co/cFcyvGD/13-800.png"}
                                     {:type                  "article",
                                      :id                    7,
                                      :input_message_content {:message_text "Скрембл с авокадо и помидорами"},
                                      :title                 "Скрембл с авокадо и помидорами",
                                      :description           "22 000 сум, 360 кКал",
                                      :thumb_url             "https://i.ibb.co/NF7s1GV/22.png"}
                                     {:type                  "article",
                                      :id                    8,
                                      :input_message_content {:message_text "Сырники со сметаной и джемом"},
                                      :title                 "Сырники со сметаной и джемом",
                                      :description           "15 000 сум, 540 кКал",
                                      :thumb_url             "https://i.ibb.co/B4dYM6c/17-000.png"}
                                     {:type                  "article",
                                      :id                    9,
                                      :input_message_content {:message_text "Свежесваренный кофе"},
                                      :title                 "Свежесваренный кофе",
                                      :description           "11 000 сум, 440 кКал",
                                      :thumb_url             "https://i.ibb.co/C8nLScV/11-000.png"}
                                     {:type                  "article",
                                      :id                    10,
                                      :input_message_content {:message_text "Яблочный фреш"},
                                      :title                 "Яблочный фреш",
                                      :description           "9 900 сум, 360 кКал",
                                      :thumb_url             "https://i.ibb.co/hXGZv1t/9-900.png"}]}})



(deftest inline-handler-test
  (testing "Testing inline handler")
  (is (= (inline/inline-query-handler ctx upd products)
         inline-handler-results)))



