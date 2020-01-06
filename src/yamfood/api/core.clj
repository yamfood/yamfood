(ns yamfood.api.core
  (:require
    [compojure.core :as c]
    [yamfood.core.regions.core :as regions]
    [ring.middleware.json :refer [wrap-json-response]]))


(defn regions-list
  [_]
  {:body (regions/all-regions!)})


(c/defroutes
  *api-routes
  (c/GET "/regions" [] regions-list))


(def api-routes
  (wrap-json-response *api-routes))


