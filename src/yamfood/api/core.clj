(ns yamfood.api.core
  (:require
    [compojure.core :as c]
    [yamfood.api.admin.core :as admin]
    [yamfood.core.regions.core :as regions]
    [yamfood.api.middleware :refer [wrap-cors]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.json :refer [wrap-json-response]]))


(defn regions-list
  [_]
  {:body (regions/all-regions!)})


(c/defroutes
  *api-routes
  (c/context "/admin" [] admin/routes)
  (c/GET "/regions" [] regions-list)
  (c/OPTIONS "/" [] "OK"))


(def api-routes
  (-> *api-routes
      (wrap-cors)
      (wrap-params)
      (wrap-json-response)))
