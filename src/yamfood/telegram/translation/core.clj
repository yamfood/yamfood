(ns yamfood.telegram.translation.core
  (:require
    [tongue.core :as tongue]
    [yamfood.telegram.handlers.emojies :as e]))


(def dictionary
  {:ru {; Registration
        :send-contact-button             "Отправить контакт"
        :change-phone-button             "Изменить номер"
        :request-code-message            "Отправьте 4х значный код отправленный на номер _+{1}_"
        :invalid-phone-message           "Неверный номер телефона, попробуйте еще раз..."
        :phone-confirmed-message         "Номер успешно подтвержден!"
        :incorrect-code-message          "Неверный код, попробуйте еще раз."
        :request-contact-message         (str "Отправь свой контакт или номер телефона в формате _998901234567_\n\n"
                                              "Мы отправим смс с кодом для подтверждения")

        ; Start
        :hello-message                   "С чего начнем?"
        :menu-button                     "\uD83C\uDF7D Что поесть?"
        :regions-button                  (str e/location-emoji " Зона покрытия")
        :settings-button                 (str e/settings-emoji " Настройки")

        ; Settings
        :settings-change-phone-button    "Изменить номер телефона"
        :settings-menu-button            (str e/back-emoji " Назад")
        :settings-message                (str e/settings-emoji " *Настройки* \n\n"
                                              "*Язык*: Русский\n"
                                              "*Номер телефона*: +{1}\n\n"
                                              "_Для смены языка нажмите на соответствующую кнопку_")

        ; Update location
        :update-location-inline-button   "Коснитесь чтобы обновить\n"
        :send-current-location-button    "Отправить текущее положение"
        :new-location-message            "Новый адресс: {1}"
        :request-location-message        (str "*куда доставить?*\n\n"
                                              "нажмите отправить локацию или отправьте локацию вручную\n\n"
                                              "_не забудьте включить локацию на своем телефоне..._")

        ; Invalid-location
        :invalid-location-message        "К сожалению, мы не обслуживаем данный регион"
        :invalid-location-regions-button "Карта обслуживания"
        :invalid-location-menu-button    (str e/back-emoji " Меню")
        :invalid-location-basket-button  (str e/basket-emoji " Корзина")

        ; Product Details
        :added-to-basket-message         "Добавлено в корзину"
        :more-button                     "\uD83C\uDF7D Еще?"
        :add-product-button              "Хочу"
        :product-basket-button           (str e/basket-emoji " Корзина ({1} сум)")
        :product-menu-button             (str e/back-emoji " Назад")
        :product-caption                 (str e/food-emoji " *{name}* \n\n"
                                              e/money-emoji "{price} сум")

        ; Basket
        :basket-message                  "Ваша корзина:"
        :empty-basket-text               "К сожалению, ваша корзина пока пуста :("
        :basket-menu-button              (str e/back-emoji " В меню")
        :to-order-button                 "✅ Далее"

        ; Order Confirmation
        :oc-basket-button                (str e/back-emoji " Корзина")
        :oc-create-order-button          "✅ Подтвердить"
        :oc-empty-comment-text           "Пусто..."
        :oc-message                      (str "*Детали вашего заказа:* \n\n"
                                              e/money-emoji " {1} сум ({2})\n"
                                              e/comment-emoji " `{3}` \n\n"
                                              e/location-emoji " {4}")

        ; Active Order
        :active-order-message            (str "*Заказ №{1}:*\n\n"
                                              "{2}"
                                              "\n"
                                              e/money-emoji " {3} сум ({4})\n\n"
                                              "Ваш заказ готовится, курьер приедет через 30 минут")

        ; Payments
        :pay-button                      "Оплатить"
        :invoice-cancel-button           "Назад"
        :invoice-title                   "Оплатить заказ №{1}"

        ; Statuses
        :status-on-kitchen               "Ваш заказ уже начал готовиться!"
        :status-canceled                 "Заказ отменен ("
        :status-on-way                   "Райдер уже в пути!"

        ; Feedback
        :request-feedback-message        "Оцените пожалуйста заказ!"

        ; Other
        :unhandled-text                  "Не понял"
        :blocked-message                 "Вы заблокированы, обратитесь в поддержку"
        :accepted                        "Принято"
        :card                            "Картой"
        :cash                            "Наличными"}
   :en {; Registration
        :send-contact-button             "[en] Отправить контакт"
        :change-phone-button             "[en] Изменить номер"
        :request-code-message            "[en] Отправьте 4х значный код отправленный на номер _+{1}_"
        :invalid-phone-message           "[en] Неверный номер телефона, попробуйте еще раз..."
        :phone-confirmed-message         "[en] Номер успешно подтвержден!"
        :incorrect-code-message          "[en] Неверный код, попробуйте еще раз."
        :request-contact-message         (str "[en] Отправь свой контакт или номер телефона в формате _998901234567_\n\n"
                                              "Мы отправим смс с кодом для подтверждения")

        ; Start
        :hello-message                   "[en] С чего начнем?"
        :menu-button                     "\uD83C\uDF7D [en] Что поесть?"
        :regions-button                  (str e/location-emoji " [en] Зона покрытия")
        :settings-button                 (str e/settings-emoji " [en] Настройки")

        ; Settings
        :settings-change-phone-button    "[en] Изменить номер телефона"
        :settings-menu-button            (str e/back-emoji " [en] Назад")
        :settings-message                (str e/settings-emoji " *[en] Настройки* \n\n"
                                              "*Язык*: Русский\n"
                                              "*Номер телефона*: +{1}\n\n"
                                              "_Для смены языка нажмите на соответствующую кнопку_")

        ; Update location
        :update-location-inline-button   "[en] Коснитесь чтобы обновить\n"
        :send-current-location-button    "[en] Отправить текущее положение"
        :new-location-message            "[en] Новый адресс: {1}"
        :request-location-message        (str "*[en] куда доставить?*\n\n"
                                              "нажмите отправить локацию или отправьте локацию вручную\n\n"
                                              "_не забудьте включить локацию на своем телефоне..._")

        ; Invalid-location
        :invalid-location-message        "[en] К сожалению, мы не обслуживаем данный регион"
        :invalid-location-regions-button "[en] Карта обслуживания"
        :invalid-location-menu-button    (str e/back-emoji " Меню")
        :invalid-location-basket-button  (str e/basket-emoji " Корзина")

        ; Product Details
        :added-to-basket-message         "[en] Добавлено в корзину"
        :more-button                     "\uD83C\uDF7D [en] Еще?"
        :add-product-button              "[en] Хочу"
        :product-basket-button           (str e/basket-emoji " [en] Корзина ({1} сум)")
        :product-menu-button             (str e/back-emoji " [en] Назад")
        :product-caption                 (str e/food-emoji " [en] *{name}* \n\n"
                                              e/money-emoji "{price} сум")

        ; Basket
        :basket-message                  "[en] Ваша корзина:"
        :empty-basket-text               "[en] К сожалению, ваша корзина пока пуста :("
        :basket-menu-button              (str e/back-emoji " [en] В меню")
        :to-order-button                 "✅ [en] Далее"

        ; Order
        :oc-basket-button                (str e/back-emoji " [en] Корзина")
        :oc-create-order-button          "✅ [en] Подтвердить"
        :oc-empty-comment-text           "[en] Пусто..."
        :oc-message                      (str "*[en] Детали вашего заказа:* \n\n"
                                              e/money-emoji " {1} сум ({2})\n"
                                              e/comment-emoji " `{3}` \n\n"
                                              e/location-emoji " {4}")

        ; Active Order
        :active-order-message            (str "[en] *Заказ №{1}:*\n\n"
                                              "{2}"
                                              "\n"
                                              e/money-emoji " {3} сум ({4})\n\n"
                                              "Ваш заказ готовится, курьер приедет через 30 минут")

        ; Payments
        :pay-button                      "[en] Оплатить"
        :invoice-cancel-button           "[en] Назад"
        :invoice-title                   "[en] Оплатить заказ №{1}"

        ; Statuses
        :status-on-kitchen               "[en] Ваш заказ уже начал готовиться!"
        :status-canceled                 "[en] Заказ отменен ("
        :status-on-way                   "[en] Райдер уже в пути!"

        ; Feedback
        :request-feedback-message        "[en] Оцените пожалуйста заказ!"

        ; Other
        :unhandled-text                  "[en] Не понял"
        :blocked-message                 "[en] Вы заблокированы, обратитесь в поддержку"
        :accepted                        "[en] Принято"
        :card                            "[en] Картой"
        :cash                            "[en] Наличными"}})


(def translate
  (tongue/build-translate dictionary))
