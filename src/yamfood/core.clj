(ns yamfood.core
  (:require
    [ring.adapter.jetty :as j]
    [environ.core :refer [env]]
    [environ.core :refer [env]]
    [compojure.route :as route]
    [yamfood.core.db.init :as db]
    [compojure.core :refer :all]
    [ring.middleware.defaults :refer [wrap-defaults
                                      site-defaults]]))

(defroutes
  app-routes
  (GET "/" [] "Hello World!")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

(defn -main []
  (db/init)
  (let [port (Integer. (or (env :port) 666))]
    (j/run-jetty #'app {:port port :join? false})))