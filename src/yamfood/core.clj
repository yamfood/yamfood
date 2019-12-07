(ns yamfood.core
  (:require
    [compojure.core :as c]
    [environ.core :refer [env]]
    [compojure.route :as route]
    [yamfood.core.db.init :as db]
    [yamfood.web.admin.core :as admin]
    [yamfood.telegram.core :as telegram]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.json :refer [wrap-json-body]]))


(c/defroutes
  app-routes
  (c/GET "/" [] "Hello World!")
  (c/context "/admin" [] admin/routes)
  (c/POST "/updates" request (telegram/telegram-handler! request))
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
