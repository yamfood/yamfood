(ns yamfood.telegram.translation.core
  (:require
    [tongue.core :as tongue]
    [yamfood.telegram.handlers.emojies :as e]))


(def dictionary
  {:ru {; Registration
        :send-contact-button             "Отправить контакт"
        :change-phone-button             "Изменить номер"
        :request-code-message            "Отправьте код отправленный на номер _+{1}_"
        :invalid-phone-message           "Неверный номер телефона, попробуйте еще раз"
        :phone-confirmed-message         "Номер успешно подтвержден!"
        :incorrect-code-message          "Неверный код, попробуйте еще раз"
        :request-contact-message         (str "Отправь свой контакт или номер телефона в формате _998901234567_\n\n"
                                              "Мы отправим СМС с кодом для подтверждения")

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
        :new-location-message            "Новый адрес: {1}"
        :request-location-message        (str "*куда доставить?*\n\n"
                                              "нажмите «Отправить текущее местоположение» или отправьте локацию вручную\n\n"
                                              "_не забудьте включить локацию на своем телефоне_")

        ; Invalid-location
        :invalid-location-message        "К сожалению, мы не обслуживаем данный регион"
        :invalid-location-regions-button "Карта обслуживания"
        :invalid-location-menu-button    (str e/back-emoji " Меню")
        :invalid-location-basket-button  (str e/basket-emoji " Корзина")

        ; Product Details
        :added-to-basket-message         "Добавлено в корзину"
        :more-button                     "\uD83C\uDF7D Еще?"
        :add-product-button              "Хочу"
        :construct-product-button        "Хочу"
        :construct-product-next-button   "✅ Дальше"
        :construct-product-back-button   (str e/back-emoji " Назад")
        :construct-product-success-text  "Блюдо успешно собрано и добавлено в корзину!"
        :product-basket-button           (str e/basket-emoji " Корзина ({1} сум)")
        :product-menu-button             (str e/back-emoji " Назад")
        :product-caption                 (str e/food-emoji " *{name}* \n\n"
                                              "{description}"
                                              e/money-emoji "{price} сум")

        ; Basket
        :basket-message                  (str "Ваша корзина:\n\n"
                                              "_Коснитесь блюда чтобы посмотреть детали_")
        :empty-basket-text               "К сожалению, ваша корзина пока пуста :("
        :basket-menu-button              (str e/back-emoji " В меню")
        :to-order-button                 "✅ Далее"

        ; Order Confirmation
        :oc-basket-button                (str e/back-emoji " Корзина")
        :oc-location-button              (str e/location-emoji " Изменить локацию")
        :oc-comment-button               (str e/comment-emoji " Комментарий")
        :oc-create-order-button          "✅ Подтвердить"
        :oc-empty-comment-text           "Укажите комментарий к адресу или к заказу"
        :oc-message                      (str "*Детали вашего заказа:* \n\n"
                                              e/food-emoji " {price} сум\n"
                                              e/delivery-emoji " {delivery} сум\n"
                                              e/money-emoji " {total} сум ({payment})\n\n"
                                              e/comment-emoji " `{comment}` \n\n"
                                              e/location-emoji " *{address}* \n\n"
                                              " _❗️Вы заказываете на указанный выше адрес, при необходимости измените его_")

        ; Active Order
        :active-order-message            (str "*Заказ №{1}:*\n\n"
                                              "{2}"
                                              "\n"
                                              e/money-emoji " {3} сум ({4})\n\n"
                                              "Ваш заказ принят, ожидайте подтверждения в боте\n\n"
                                              "Ждать звонка оператора больше не нужно \uD83D\uDE04")

        ; Payments
        :pay-button                      "Оплатить"
        :invoice-cancel-button           "Назад"
        :invoice-title                   "Оплатить заказ №{1}"

        ; Statuses
        :status-on-kitchen               "Ваш заказ готовится, курьер приедет через 60 минут или раньше!"
        :status-canceled                 "Заказ отменен :("
        :status-on-way                   "Курьер уже в пути!"

        ; Feedback
        :request-feedback-message        "Оцените, пожалуйста, заказ!"
        :request-text-feedback           "Напишите комментарий или выберите из предложенных"

        :feedback-ok                     "Спасибо, все хорошо"
        :feedback-long-delivery          "Долгая доставка"
        :feedback-cold-food              "Остывшая еда"
        :feedback-incomplete-order       "Неполный заказ"
        :feedback-no-cutlery             "Нет приборов"
        :feedback-bad-courier            "Жалоба на курьера"

        ; Other
        :inline-back-title               "Назад"
        :inline-back-description         "Коснитесь чтобы вернуться в меню"
        :all-kitchens-closed             "К сожалению, на данный момент все кухни закрыты :("
        :disabled-products-removed       "Указанные ниже блюда временно недоступны в ближайшем филиале, они удалены из корзины\n\n"
        :blocked-message                 "Вы заблокированы, обратитесь в службу поддержки"
        :accepted                        "Принято"
        :card                            "Картой"
        :cash                            "Наличными"
        :confirmation-code               "Ваш код подтверждения {1} {2}"}
   :en {; Registration
        :send-contact-button             "Send a contact"
        :change-phone-button             "Change phone number"
        :request-code-message            "Enter the code you've just received on your phone number _+{1}_"
        :invalid-phone-message           "Invalid phone number, please try again"
        :phone-confirmed-message         "The phone number was successfully confirmed!"
        :incorrect-code-message          "Invalid code, try again"
        :request-contact-message         (str "Send your contact or phone number in the format _998901234567_\n\n"
                                              "We will send an SMS with a confirmation code")

        ; Start
        :hello-message                   "Where do we start?"
        :menu-button                     "\uD83C\uDF7D What to eat?"
        :regions-button                  (str e/location-emoji " Delivery zone")
        :settings-button                 (str e/settings-emoji " Settings")

        ; Settings
        :settings-change-phone-button    "Change your phone number"
        :settings-menu-button            (str e/back-emoji " Back")
        :settings-message                (str e/settings-emoji " *Settings* \n\n"
                                              "*Language*: English\n"
                                              "*Phone number*: +{1}\n\n"
                                              "_To change the language, click the relevant button_")

        ; Update location
        :update-location-inline-button   "Tap to update\n"
        :send-current-location-button    "Send current location"
        :new-location-message            "New address: {1}"
        :request-location-message        (str "*Where to deliver?*\n\n"
                                              "Tap «Send current location» or send the location manually\n\n"
                                              "_don't forget to enable location on your phone_")

        ; Invalid-location
        :invalid-location-message        "Unfortunately, we do not serve this region"
        :invalid-location-regions-button "Service map"
        :invalid-location-menu-button    (str e/back-emoji " Menu")
        :invalid-location-basket-button  (str e/basket-emoji " Cart")

        ; Product Details
        :added-to-basket-message         "Added to cart"
        :more-button                     "\uD83C\uDF7D More?"
        :add-product-button              "Want"
        :construct-product-button        "Want"
        :construct-product-next-button   "✅ Next"
        :construct-product-back-button   (str e/back-emoji " Back")
        :construct-product-success-text  "The dish was successfully constructed and added to the basket!"
        :product-basket-button           (str e/basket-emoji " Cart ({1} sum)")
        :product-menu-button             (str e/back-emoji " Back")
        :product-caption                 (str e/food-emoji " *{name}* \n\n"
                                              e/money-emoji "{price} sum")

        ; Basket
        :basket-message                  (str "Your Cart:\n\n"
                                              "_Tap a dish to view details_")
        :empty-basket-text               "Unfortunately, your Cart is still empty :("
        :basket-menu-button              (str e/back-emoji " Into menu")
        :to-order-button                 "✅ Further"

        ; Order Confirmation
        :oc-basket-button                (str e/back-emoji " Cart")
        :oc-location-button              (str e/location-emoji " Change location")
        :oc-comment-button               (str e/comment-emoji " Comment")
        :oc-create-order-button          "✅ Confirm"
        :oc-empty-comment-text           "Enter a comment to the address or order"
        :oc-message                      (str "*Details of your order:* \n\n"
                                              e/food-emoji " {price} sum\n"
                                              e/delivery-emoji " {delivery} sum\n"
                                              e/money-emoji " {total} sum ({payment})\n"
                                              e/comment-emoji " `{comment}` \n\n"
                                              e/location-emoji " *{address}* \n\n"
                                              " _❗️You order to the above address, change it if necessary_")

        ; Active Order
        :active-order-message            (str "*Order №{1}:*\n\n"
                                              "{2}"
                                              "\n"
                                              e/money-emoji " {3} sum ({4})\n\n"
                                              "Your order is accepted, wait for confirmation in the bot\n\n"
                                              "You no longer need to wait for the operator to call you \uD83D\uDE04")

        ; Payments
        :pay-button                      "Pay"
        :invoice-cancel-button           "Back"
        :invoice-title                   "Pay order №{1}"

        ; Statuses
        :status-on-kitchen               "Your order is preparing, rider will arrive in 60 minutes or earlier"
        :status-canceled                 "The order has been cancelled :("
        :status-on-way                   "Rider is on its way!"

        ; Feedback
        :request-feedback-message        "Please rate the order!"
        :request-text-feedback           "Write a comment or choose from proposed\n"

        :feedback-ok                     "Thank you, everything is good"
        :feedback-long-delivery          "Late delivery"
        :feedback-cold-food              "Cooled food"
        :feedback-incomplete-order       "Incomplete order"
        :feedback-no-cutlery             "No appliances"
        :feedback-bad-courier            "Courier complaint"

        ; Other
        :inline-back-title               "Back"
        :inline-back-description         "Tap to return to the menu"
        :all-kitchens-closed             "Unfortunately, all branches are currently closed :("
        :disabled-products-removed       "The dishes listed below are temporarily unavailable at the nearest branch and have been removed from the cart\n\n"
        :blocked-message                 "You are blocked, please contact the support service"
        :accepted                        "Accepted"
        :card                            "By card"
        :cash                            "In cash"
        :confirmation-code               "Your {1} confirmation code is {2}"}
   :uz {; Registration
        :send-contact-button             "Telefon raqamini yuborish"
        :change-phone-button             "Telefon raqamini o'zgartirish"
        :request-code-message            "Telefon raqamingizga yuborilgan kodni yuboring _+{1}_"
        :invalid-phone-message           "Noto'g'ri telefon raqami, yana urinib ko'ring"
        :phone-confirmed-message         "Telefon raqami tasdiqlangan!"
        :incorrect-code-message          "Noto'g'ri kod, yana urinib ko'ring"
        :request-contact-message         (str "Kontaktingizni yoki telefon raqamingizni formatda yuboring _998901234567_\n\n"
                                              "Tasdiqlash uchun kod bilan SMS yuboramiz")

        ; Start
        :hello-message                   "Nimadan boshlaymiz?"
        :menu-button                     "\uD83C\uDF7D Nima ovqatlar bor?"
        :regions-button                  (str e/location-emoji " Qoplama maydoni")
        :settings-button                 (str e/settings-emoji " Sozlamalar")

        ; Settings
        :settings-change-phone-button    "Telefon raqamini o'zgartirish"
        :settings-menu-button            (str e/back-emoji " Orqaga")
        :settings-message                (str e/settings-emoji " *Sozlamalar* \n\n"
                                              "*Til*: O'zbekcha\n"
                                              "*Telefon raqami*: +{1}\n\n"
                                              "_Tilni o'zgartirish uchun tegishli tugmani bosing_")

        ; Update location
        :update-location-inline-button   "Yangilash uchun bosing\n"
        :send-current-location-button    "Joriy manzilni yuborish"
        :new-location-message            "Yangi manzil: {1}"
        :request-location-message        (str "*qayerga yetkazib berish kerak?*\n\n"
                                              "«Joriy manzilni yuborish» tugmasini bosing yoki joyni qo'lda yuboring\n\n"
                                              "_telefoningizdagi joyni yoqishni unutmang_")

        ; Invalid-location
        :invalid-location-message        "Afsuski, biz ushbu manzilda xizmat qilmaymiz"
        :invalid-location-regions-button "Xizmat kartasi"
        :invalid-location-menu-button    (str e/back-emoji " Menyu")
        :invalid-location-basket-button  (str e/basket-emoji " Savat")

        ; Product Details
        :added-to-basket-message         "Savatga qo'shilgan"
        :more-button                     "\uD83C\uDF7D Yana?"
        :add-product-button              "Istayman"
        :construct-product-button        "Istayman"
        :construct-product-next-button   "✅ Keyingi"
        :construct-product-back-button   (str e/back-emoji " Orqaga")
        :construct-product-success-text  "Ovqat yig'ildi va savatga qo'shildi"
        :product-basket-button           (str e/basket-emoji " Savat ({1} so'm)")
        :product-menu-button             (str e/back-emoji " Orqaga")
        :product-caption                 (str e/food-emoji " *{name}* \n\n"
                                              "{description}"
                                              e/money-emoji "{price} so'm")

        ; Basket
        :basket-message                  (str "Sizning savatingiz:\n\n"
                                              "_Tafsilotlarini ko'rish uchun taomni bosing_")
        :empty-basket-text               "Afsuski, sizning savatingiz hali bo'sh :("
        :basket-menu-button              (str e/back-emoji " Menyuga")
        :to-order-button                 "✅ Keyingi"

        ; Order Confirmation
        :oc-basket-button                (str e/back-emoji " Savat")
        :oc-location-button              (str e/location-emoji " Manzilni o'zgartirish")
        :oc-comment-button               (str e/comment-emoji " Izoh")
        :oc-create-order-button          "✅ Tasdiqlash"
        :oc-empty-comment-text           "Manzilga yoki buyurtmaga izoh kiriting"
        :oc-message                      (str "*Buyurtmaning tafsilotlari:* \n\n"
                                              e/food-emoji " {price} so'm\n"
                                              e/delivery-emoji " {delivery} so'm\n"
                                              e/money-emoji " {total} so'm ({payment})\n"
                                              e/comment-emoji " `{comment}` \n\n"
                                              e/location-emoji " *{address}* \n\n"
                                              " _❗️Yuqoridagi manzilga buyurtma beryapsiz, agar kerak bo'lsa, uni o'zgartiring_")

        ; Active Order
        :active-order-message            (str "*Buyurtma №{1}:*\n\n"
                                              "{2}"
                                              "\n"
                                              e/money-emoji " {3} so'm ({4})\n\n"
                                              "Sizning buyurtmangiz qabul qilindi, botda tasdiqlashni kuting\n\n"
                                              "Operator qo'ng'irog'ini kutish endi kerak emas \uD83D\uDE04")

        ; Payments
        :pay-button                      "To'lash"
        :invoice-cancel-button           "Orqaga"
        :invoice-title                   "Buyurtmani to'lash №{1}"

        ; Statuses
        :status-on-kitchen               "Sizning buyurtmangiz tayyorlanyapti, kuryer 60 daqiqada yoki undan oldin keladi!"
        :status-canceled                 "Buyurtma bekor qilindi :("
        :status-on-way                   "Kuryer yo'lda!"

        ; Feedback
        :request-feedback-message        "Iltimos, buyurtmani baholang!"
        :request-text-feedback           "Sharh yozing yoki taklif qilinganlardan birini tanlang"

        :feedback-ok                     "Rahmat, hammasi yaxshi"
        :feedback-long-delivery          "Kech yetkazma"
        :feedback-cold-food              "Sovib ketdgan taom"
        :feedback-incomplete-order       "Buyurtma to'liq emas"
        :feedback-no-cutlery             "Oshxona anjomlari yo'q"
        :feedback-bad-courier            "Kuryerga shikoyat"

        ; Other
        :inline-back-title               "Orqaga"
        :inline-back-description         "Menyuga qaytish uchun bosing"
        :all-kitchens-closed             "Afsuski, hozirgi vaqtda barcha filiallar yopiq :(\n"
        :disabled-products-removed       "Quyidagi taomlar yaqin atrofdagi filialda vaqtincha mavjud emas, ular savatdan olib tashlanadi\n\n"
        :blocked-message                 "Siz bloklangindiz, qo'llab-quvvatlash xizmatiga murojaat qiling"
        :accepted                        "Qabul qilindi"
        :card                            "Karta bilan"
        :cash                            "Naqd pul bilan"
        :confirmation-code               "Sizning {1} tasdiqlash kodi {2}"}})


(def translate
  (tongue/build-translate dictionary))
