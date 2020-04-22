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
        :hello-message                   "Готовим и бесплатно доставляем за 30 минут"
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
                                              e/money-emoji "{price} сум  " e/energy-emoji "{energy} кКал")

        ; Basket
        :basket-message                  "Ваша корзина:"
        :empty-basket-text               "К сожалению, ваша корзина пока пуста :("
        :basket-menu-button              (str e/back-emoji " В меню")
        :to-order-button                 "✅ Далее"

        ; Order
        :oc-basket-button                (str e/basket-emoji " Корзина")
        :oc-create-order-button          "✅ Подтвердить"
        :oc-empty-comment-text           "Пусто..."
        :oc-message                      (str "*Детали вашего заказа:* \n\n"
                                              e/money-emoji " {1} сум ({2})\n"
                                              e/comment-emoji " `{3}` \n\n"
                                              e/location-emoji " {4}")

        ; Payments
        :pay-button                      "Оплатить"
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
        :accepted                        "Принято"}})


(def translate
  (tongue/build-translate dictionary))
