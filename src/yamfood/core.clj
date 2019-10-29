(ns yamfood.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as j]
            [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes
  app-routes
  (GET "/" [] "Hello World!")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))


(defn -main []
  (let [port (Integer. (or (env :port) 666))]
    (j/run-jetty #'app {:port port :join? false})))