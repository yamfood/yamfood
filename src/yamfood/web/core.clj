(ns yamfood.web.core
  (:require
    [compojure.core :as c]))


(def regions-html
  (slurp "resources/public/html/regions.html"))


(defn regions-view
  [_]
  {:body regions-html})


(c/defroutes
  web-routes
  (c/GET "/" [] "Hello WEB!")
  (c/GET "/regions" [] regions-view))
