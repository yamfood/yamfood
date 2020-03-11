(ns yamfood.api.admin.handlers.kitchens
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.core.kitchens.core :as k]))


(defn kitchens-list
  [_]
  {:body (k/all-kitchens!)})


(defn kitchen-detail
  [request]
  (let [kitchen-id (u/str->int (:id (:params request)))
        kitchen (k/kitchen-by-id! kitchen-id)]
    (if kitchen
      {:body kitchen}
      {:body   {:error "Not found"}
       :status 404})))


(c/defroutes
  routes
  (c/GET "/" [] kitchens-list)
  (c/GET "/:id{[0-9]+}/" [] kitchen-detail))

