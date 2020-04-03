(ns yamfood.api.admin.handlers.announcements
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [yamfood.api.pagination :as p]
    [yamfood.core.announcements.core :as a]))


(defn announcements-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        offset (p/calc-offset page per-page)
        count (a/all-announcements-count!)]
    {:body (p/format-result
             count
             per-page
             page
             (a/all-announcements!
               offset
               per-page))}))


(defn announcement-details
  [request])


(defn create-announcement
  [request])


(defn patch-announcement
  [request])


(defn delete-announcement
  [request])


(c/defroutes
  routes
  (c/GET "/" [] announcements-list)
  (c/POST "/" [] create-announcement)

  (c/GET "/:id{[0-9]+}/" [] announcement-details)
  (c/PATCH "/:id{[0-9]+}/" [] patch-announcement)
  (c/DELETE "/:id{[0-9]+}/" [] delete-announcement))

