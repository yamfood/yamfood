(ns yamfood.core
  (:require
    [compojure.core :as c]
    [yamfood.api.core :as api]
    [yamfood.web.core :as web]
    [environ.core :refer [env]]
    [compojure.route :as route]
    [yamfood.core.db.init :as db]
    [yamfood.tasks.core :as tasks]
    [yamfood.telegram.core :as telegram]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.json :refer [wrap-json-body]]))


(c/defroutes
  app-routes

  (c/context "/" [] web/web-routes)
  (c/context "/api" [] api/api-routes)

  (c/context "/updates" [] telegram/telegram-routes)

  (route/not-found "Not Found"))


(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})))


(defn -main []
  (db/init)
  (tasks/run-tasks!)
  (let [port (Integer. (or (env :port) 666))]
    (run-jetty #'app {:port port :join? false})))


;(def server (-main))
;(.stop server)
