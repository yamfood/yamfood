(ns yamfood.api.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.core :as admin]
    [yamfood.api.middleware :refer [wrap-cors]]
    [yamfood.core.regions.core :as regions]
    [ring.middleware.json :refer [wrap-json-response]]))


(defn regions-list
  [_]
  {:body (regions/all-regions!)})


(c/defroutes
  *api-routes
  (c/context "/admin" [] admin/admin-api-routes)
  (c/GET "/regions" [] regions-list))


(def api-routes
  (-> *api-routes
      (wrap-cors)
      (wrap-json-response)))


