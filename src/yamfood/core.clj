(ns yamfood.core
  (:require
    [environ.core :refer [env]]
    [compojure.route :as route]
    [compojure.core :refer :all]
    [yamfood.core.db.init :as db]
    [yamfood.telegram.core :as telegram]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.json :refer [wrap-json-body]]))


(defroutes
  app-routes
  (GET "/" [] "Hello World!")
  (POST "/updates" request (telegram/telegram-handler! request))
  (route/not-found "Not Found"))


(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})))


(defn -main []
  (db/init)
  (let [port (Integer. (or (env :port) 666))]
    (run-jetty #'app {:port port :join? false})))


;(def server (-main))
;(.stop server)
