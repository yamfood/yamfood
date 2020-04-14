(ns yamfood.api.admin.handlers.data
  (:require
    [compojure.core :as c]
    [yamfood.core.orders.core :as o]))


(defn orders-heatmap-geojson
  [_]
  {:body (map :location (o/ended-orders! 0 10000))})


(c/defroutes
  routes
  (c/GET "/heatmap/" [] orders-heatmap-geojson))



