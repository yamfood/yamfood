(ns yamfood.web.core
  (:require [compojure.core :as c]
            [hiccup.core :as h]))


(def regions-html
  (slurp "resources/public/html/regions.html"))


(defn regions-view
  [request]
  {:body regions-html})


(c/defroutes
  web-routes
  (c/GET "/" [] "Hello WEB!")
  (c/GET "/regions" [] regions-view))
